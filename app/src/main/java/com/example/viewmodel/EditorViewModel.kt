package com.example.viewmodel

import android.app.Application
import android.net.Uri
import androidx.annotation.OptIn
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.Player
import androidx.media3.common.util.ExperimentalApi
import androidx.media3.common.util.UnstableApi
import androidx.media3.transformer.CompositionPlayer
import com.example.data.AppDatabase
import com.example.data.ProjectRepository
import com.example.media.BuiltInAudioGenerator
import com.example.media.ExportState
import com.example.media.Media3EffectFactory
import com.example.media.ThumbnailExtractor
import com.example.media.VideoExportEngine
import com.example.model.AspectRatioPreset
import com.example.model.AudioTrackItem
import com.example.model.ExportResolution
import com.example.model.FilterPreset
import com.example.model.TextOverlayItem
import com.example.model.TextStylePreset
import com.example.model.VideoClip
import com.example.model.VideoProject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID

enum class EditorToolTab {
    EDIT,
    AUDIO,
    TEXT,
    FILTERS,
    RATIO,
    STICKERS
}

@OptIn(UnstableApi::class, ExperimentalApi::class)
class EditorViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val projectRepository = ProjectRepository(application, db.projectDao())
    private val exportEngine = VideoExportEngine(application)

    private val _project = MutableStateFlow<VideoProject?>(null)
    val project: StateFlow<VideoProject?> = _project.asStateFlow()

    private val _selectedClipId = MutableStateFlow<String?>(null)
    val selectedClipId: StateFlow<String?> = _selectedClipId.asStateFlow()

    private val _selectedOverlayId = MutableStateFlow<String?>(null)
    val selectedOverlayId: StateFlow<String?> = _selectedOverlayId.asStateFlow()

    private val _activeToolTab = MutableStateFlow(EditorToolTab.EDIT)
    val activeToolTab: StateFlow<EditorToolTab> = _activeToolTab.asStateFlow()

    private val _playheadMs = MutableStateFlow(0L)
    val playheadMs: StateFlow<Long> = _playheadMs.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _timelineZoom = MutableStateFlow(1.0f)
    val timelineZoom: StateFlow<Float> = _timelineZoom.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _canUndo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()

    private val _canRedo = MutableStateFlow(false)
    val canRedo: StateFlow<Boolean> = _canRedo.asStateFlow()

    val exportState: StateFlow<ExportState> = exportEngine.exportState

    private val undoStack = ArrayDeque<VideoProject>()
    private val redoStack = ArrayDeque<VideoProject>()

    private var _compositionPlayer: CompositionPlayer? = null
    private val _playerState = MutableStateFlow<Player?>(null)
    val playerState: StateFlow<Player?> = _playerState.asStateFlow()

    private var tickerJob: Job? = null
    private var loadedProjectId: String? = null

    fun loadProject(projectId: String, initialTool: EditorToolTab? = null) {
        if (loadedProjectId == projectId && _project.value != null) {
            if (initialTool != null) _activeToolTab.value = initialTool
            return
        }
        loadedProjectId = projectId
        viewModelScope.launch {
            val loaded = projectRepository.getProject(projectId)
            if (loaded != null) {
                _project.value = loaded
                _selectedClipId.value = loaded.clips.firstOrNull()?.id
                if (initialTool != null) _activeToolTab.value = initialTool
                ensurePlayerAndRebuild(seekToMs = 0L)
            }
        }
    }

    fun selectToolTab(tab: EditorToolTab) {
        _activeToolTab.value = tab
    }

    fun selectClip(clipId: String?) {
        _selectedClipId.value = clipId
        if (clipId != null) {
            _selectedOverlayId.value = null
        }
    }

    fun selectOverlay(overlayId: String?) {
        _selectedOverlayId.value = overlayId
    }

    fun setTimelineZoom(zoom: Float) {
        _timelineZoom.value = zoom.coerceIn(0.6f, 2.5f)
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    fun togglePlayPause() {
        val player = _compositionPlayer ?: return
        if (player.isPlaying) {
            player.pause()
        } else {
            val total = _project.value?.totalDurationMs ?: 0L
            if (total > 0 && player.currentPosition >= total - 80L) {
                player.seekTo(0L)
                _playheadMs.value = 0L
            }
            player.play()
        }
    }

    fun pausePlayback() {
        _compositionPlayer?.pause()
    }

    fun seekTo(positionMs: Long) {
        val currentProject = _project.value ?: return
        val clamped = positionMs.coerceIn(0L, currentProject.totalDurationMs.coerceAtLeast(0L))
        _playheadMs.value = clamped
        _compositionPlayer?.seekTo(clamped)
    }

    fun stepBy(deltaMs: Long) {
        seekTo(_playheadMs.value + deltaMs)
    }

    fun undo() {
        val current = _project.value ?: return
        if (undoStack.isEmpty()) return
        redoStack.addLast(current)
        val previous = undoStack.removeLast()
        applyProjectState(previous, rebuildPlayer = true, pushUndo = false)
    }

    fun redo() {
        val current = _project.value ?: return
        if (redoStack.isEmpty()) return
        undoStack.addLast(current)
        val next = redoStack.removeLast()
        applyProjectState(next, rebuildPlayer = true, pushUndo = false)
    }

    fun addClips(uris: List<Uri>) {
        val current = _project.value ?: return
        if (uris.isEmpty()) return
        viewModelScope.launch {
            val context = getApplication<Application>()
            val newClips = uris.mapIndexed { idx, uri ->
                val persisted = projectRepository.persistMediaUri(uri, prefix = "clip")
                val probe = ThumbnailExtractor.probeMedia(context, persisted)
                val name = projectRepository.resolveDisplayName(uri)
                VideoClip(
                    uriString = persisted,
                    isImage = probe.isImage,
                    originalDurationMs = probe.durationMs,
                    trimStartMs = 0L,
                    trimEndMs = probe.durationMs,
                    label = name.substringBeforeLast('.').take(16).ifBlank { "Clip ${current.clips.size + idx + 1}" }
                )
            }
            val updated = current.copy(clips = current.clips + newClips)
            _selectedClipId.value = newClips.firstOrNull()?.id
            applyProjectState(updated, rebuildPlayer = true)
        }
    }

    fun splitSelectedClipAtPlayhead(errorMessage: String) {
        val current = _project.value ?: return
        val hit = current.clipAtTimelinePosition(_playheadMs.value) ?: return
        val (clipIndex, offsetInClipMs) = hit
        val clip = current.clips[clipIndex]
        _selectedClipId.value = clip.id

        if (clip.isImage) {
            val leftDur = offsetInClipMs.coerceAtLeast(0L)
            val rightDur = clip.effectiveDurationMs - leftDur
            if (leftDur < 350L || rightDur < 350L) {
                _statusMessage.value = errorMessage
                return
            }
            val first = clip.copy(
                id = UUID.randomUUID().toString(),
                originalDurationMs = leftDur,
                trimStartMs = 0L,
                trimEndMs = leftDur
            )
            val second = clip.copy(
                id = UUID.randomUUID().toString(),
                originalDurationMs = rightDur,
                trimStartMs = 0L,
                trimEndMs = rightDur
            )
            val updatedClips = current.clips.toMutableList().apply {
                removeAt(clipIndex)
                add(clipIndex, second)
                add(clipIndex, first)
            }
            _selectedClipId.value = second.id
            applyProjectState(current.copy(clips = updatedClips), rebuildPlayer = true)
            return
        }

        val sourceOffsetMs = (offsetInClipMs * clip.speed).toLong()
        val splitPointSourceMs = clip.trimStartMs + sourceOffsetMs
        if (splitPointSourceMs - clip.trimStartMs < 350L || clip.trimEndMs - splitPointSourceMs < 350L) {
            _statusMessage.value = errorMessage
            return
        }

        val first = clip.copy(
            id = UUID.randomUUID().toString(),
            trimEndMs = splitPointSourceMs
        )
        val second = clip.copy(
            id = UUID.randomUUID().toString(),
            trimStartMs = splitPointSourceMs
        )
        val updatedClips = current.clips.toMutableList().apply {
            removeAt(clipIndex)
            add(clipIndex, second)
            add(clipIndex, first)
        }
        _selectedClipId.value = second.id
        applyProjectState(current.copy(clips = updatedClips), rebuildPlayer = true)
    }

    fun updateSelectedClipTrim(newTrimStartMs: Long, newTrimEndMs: Long) {
        val current = _project.value ?: return
        val clipId = _selectedClipId.value ?: return
        val updatedClips = current.clips.map { clip ->
            if (clip.id != clipId) clip
            else {
                val maxDur = if (clip.isImage) 15_000L else clip.originalDurationMs
                val start = newTrimStartMs.coerceIn(0L, (maxDur - 350L).coerceAtLeast(0L))
                val end = newTrimEndMs.coerceIn((start + 350L).coerceAtMost(maxDur), maxDur)
                if (clip.isImage) {
                    clip.copy(originalDurationMs = end - start, trimStartMs = 0L, trimEndMs = end - start)
                } else {
                    clip.copy(trimStartMs = start, trimEndMs = end)
                }
            }
        }
        applyProjectState(current.copy(clips = updatedClips), rebuildPlayer = true)
    }

    fun trimSelectedClipToPlayhead(trimStart: Boolean, errorMessage: String) {
        val current = _project.value ?: return
        val hit = current.clipAtTimelinePosition(_playheadMs.value) ?: return
        val (clipIndex, offsetInClipMs) = hit
        val clip = current.clips[clipIndex]
        _selectedClipId.value = clip.id
        val sourceOffset = (offsetInClipMs * clip.speed).toLong()
        val point = clip.trimStartMs + sourceOffset
        if (trimStart) {
            if (clip.trimEndMs - point < 350L) {
                _statusMessage.value = errorMessage
                return
            }
            updateSelectedClipTrim(point, clip.trimEndMs)
        } else {
            if (point - clip.trimStartMs < 350L) {
                _statusMessage.value = errorMessage
                return
            }
            updateSelectedClipTrim(clip.trimStartMs, point)
        }
    }

    fun setSelectedClipSpeed(speed: Float) {
        updateSelectedClip { it.copy(speed = speed.coerceIn(0.25f, 4.0f)) }
    }

    fun setSelectedClipVolume(volume: Float) {
        updateSelectedClip { it.copy(volume = volume.coerceIn(0f, 2.0f), isMuted = volume <= 0.01f) }
    }

    fun toggleMuteSelectedClip() {
        updateSelectedClip { it.copy(isMuted = !it.isMuted) }
    }

    fun rotateSelectedClip() {
        updateSelectedClip { it.copy(rotationDegrees = (it.rotationDegrees + 90) % 360) }
    }

    fun duplicateSelectedClip() {
        val current = _project.value ?: return
        val clipId = _selectedClipId.value ?: return
        val idx = current.clips.indexOfFirst { it.id == clipId }
        if (idx < 0) return
        val copy = current.clips[idx].copy(id = UUID.randomUUID().toString())
        val list = current.clips.toMutableList().apply { add(idx + 1, copy) }
        _selectedClipId.value = copy.id
        applyProjectState(current.copy(clips = list), rebuildPlayer = true)
    }

    fun moveSelectedClip(delta: Int) {
        val current = _project.value ?: return
        val clipId = _selectedClipId.value ?: return
        val idx = current.clips.indexOfFirst { it.id == clipId }
        val target = idx + delta
        if (idx < 0 || target !in current.clips.indices) return
        val list = current.clips.toMutableList()
        val item = list.removeAt(idx)
        list.add(target, item)
        applyProjectState(current.copy(clips = list), rebuildPlayer = true)
    }

    fun deleteSelectedClip(cannotDeleteMsg: String) {
        val current = _project.value ?: return
        if (current.clips.size <= 1) {
            _statusMessage.value = cannotDeleteMsg
            return
        }
        val clipId = _selectedClipId.value ?: return
        val idx = current.clips.indexOfFirst { it.id == clipId }
        if (idx < 0) return
        val list = current.clips.toMutableList().apply { removeAt(idx) }
        _selectedClipId.value = list.getOrNull(idx.coerceAtMost(list.lastIndex))?.id
        applyProjectState(current.copy(clips = list), rebuildPlayer = true)
    }

    fun applyFilter(preset: FilterPreset, applyToAll: Boolean) {
        val current = _project.value ?: return
        val targetId = _selectedClipId.value ?: current.clips.firstOrNull()?.id
        val updatedClips = current.clips.map { clip ->
            if (applyToAll || clip.id == targetId) clip.copy(filterPreset = preset) else clip
        }
        applyProjectState(current.copy(clips = updatedClips), rebuildPlayer = true)
    }

    fun applyAdjustments(
        brightness: Float,
        contrast: Float,
        saturation: Float,
        applyToAll: Boolean
    ) {
        val current = _project.value ?: return
        val targetId = _selectedClipId.value ?: current.clips.firstOrNull()?.id
        val updatedClips = current.clips.map { clip ->
            if (applyToAll || clip.id == targetId) {
                clip.copy(
                    brightness = brightness.coerceIn(-0.5f, 0.5f),
                    contrast = contrast.coerceIn(-0.5f, 0.5f),
                    saturation = saturation.coerceIn(-1.0f, 1.0f)
                )
            } else clip
        }
        applyProjectState(current.copy(clips = updatedClips), rebuildPlayer = true)
    }

    fun setAspectRatio(ratio: AspectRatioPreset) {
        val current = _project.value ?: return
        applyProjectState(current.copy(aspectRatio = ratio), rebuildPlayer = true)
    }

    fun setExportResolution(resolution: ExportResolution) {
        val current = _project.value ?: return
        applyProjectState(current.copy(exportResolution = resolution), rebuildPlayer = false)
    }

    fun setExportFps(fps: Int) {
        val current = _project.value ?: return
        applyProjectState(current.copy(exportFps = fps), rebuildPlayer = false)
    }

    fun addTextOverlay(
        text: String,
        stylePreset: TextStylePreset,
        colorHex: Long,
        bgColorHex: Long,
        isStickerBadge: Boolean = false
    ) {
        val current = _project.value ?: return
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        val start = _playheadMs.value.coerceIn(0L, (current.totalDurationMs - 500L).coerceAtLeast(0L))
        val end = (start + 3500L).coerceAtMost(current.totalDurationMs.coerceAtLeast(3500L))
        val item = TextOverlayItem(
            text = trimmed,
            startMs = start,
            endMs = end,
            xFraction = 0.5f,
            yFraction = if (isStickerBadge) 0.22f else 0.78f,
            colorHex = colorHex,
            bgColorHex = bgColorHex,
            fontSizeSp = if (isStickerBadge) 18f else 22f,
            stylePreset = stylePreset,
            isStickerBadge = isStickerBadge
        )
        _selectedOverlayId.value = item.id
        applyProjectState(
            current.copy(textOverlays = current.textOverlays + item),
            rebuildPlayer = false
        )
    }

    fun updateTextOverlay(updatedItem: TextOverlayItem) {
        val current = _project.value ?: return
        val list = current.textOverlays.map { if (it.id == updatedItem.id) updatedItem else it }
        applyProjectState(current.copy(textOverlays = list), rebuildPlayer = false)
    }

    fun moveTextOverlayLive(id: String, deltaXFrac: Float, deltaYFrac: Float) {
        val current = _project.value ?: return
        val list = current.textOverlays.map { item ->
            if (item.id != id) item
            else item.copy(
                xFraction = (item.xFraction + deltaXFrac).coerceIn(0.12f, 0.88f),
                yFraction = (item.yFraction + deltaYFrac).coerceIn(0.10f, 0.90f)
            )
        }
        _project.value = current.copy(textOverlays = list)
    }

    fun commitLiveOverlaysToDb() {
        val current = _project.value ?: return
        viewModelScope.launch { projectRepository.saveProject(current) }
    }

    fun deleteTextOverlay(id: String) {
        val current = _project.value ?: return
        if (_selectedOverlayId.value == id) _selectedOverlayId.value = null
        applyProjectState(
            current.copy(textOverlays = current.textOverlays.filterNot { it.id == id }),
            rebuildPlayer = false
        )
    }

    fun attachDeviceAudio(uri: Uri) {
        val current = _project.value ?: return
        viewModelScope.launch {
            val context = getApplication<Application>()
            val persisted = projectRepository.persistMediaUri(uri, prefix = "audio")
            val durationMs = ThumbnailExtractor.probeAudioDuration(context, persisted)
            val title = projectRepository.resolveDisplayName(uri).substringBeforeLast('.')
            val track = AudioTrackItem(
                uriString = persisted,
                title = title.ifBlank { "Custom Audio" },
                durationMs = durationMs,
                volume = 0.7f,
                loop = true,
                isBuiltInBeat = false
            )
            applyProjectState(current.copy(audioTrack = track), rebuildPlayer = true)
        }
    }

    fun attachBuiltInBeat(beatId: String, title: String) {
        val current = _project.value ?: return
        viewModelScope.launch {
            val context = getApplication<Application>()
            val (uriStr, durationMs) = BuiltInAudioGenerator.ensureBeatFile(context, beatId)
            val track = AudioTrackItem(
                uriString = uriStr,
                title = title,
                durationMs = durationMs,
                volume = 0.7f,
                loop = true,
                isBuiltInBeat = true
            )
            applyProjectState(current.copy(audioTrack = track), rebuildPlayer = true)
        }
    }

    fun setAudioVolume(volume: Float) {
        val current = _project.value ?: return
        val audio = current.audioTrack ?: return
        applyProjectState(
            current.copy(audioTrack = audio.copy(volume = volume.coerceIn(0f, 1.5f))),
            rebuildPlayer = true
        )
    }

    fun toggleAudioLoop() {
        val current = _project.value ?: return
        val audio = current.audioTrack ?: return
        applyProjectState(
            current.copy(audioTrack = audio.copy(loop = !audio.loop)),
            rebuildPlayer = true
        )
    }

    fun removeAudioTrack() {
        val current = _project.value ?: return
        if (current.audioTrack == null) return
        applyProjectState(current.copy(audioTrack = null), rebuildPlayer = true)
    }

    fun startExport() {
        val current = _project.value ?: return
        pausePlayback()
        exportEngine.startExport(current) { record ->
            viewModelScope.launch {
                projectRepository.addExportRecord(record)
            }
        }
    }

    fun cancelExport() {
        exportEngine.cancelExport()
    }

    fun resetExportState() {
        exportEngine.resetState()
    }

    private fun updateSelectedClip(transform: (VideoClip) -> VideoClip) {
        val current = _project.value ?: return
        val targetId = _selectedClipId.value ?: current.clips.firstOrNull()?.id ?: return
        val updatedClips = current.clips.map { if (it.id == targetId) transform(it) else it }
        applyProjectState(current.copy(clips = updatedClips), rebuildPlayer = true)
    }

    private fun applyProjectState(
        updated: VideoProject,
        rebuildPlayer: Boolean,
        pushUndo: Boolean = true
    ) {
        val previous = _project.value
        if (pushUndo && previous != null && previous != updated) {
            undoStack.addLast(previous)
            if (undoStack.size > 25) undoStack.removeFirst()
            redoStack.clear()
        }
        _canUndo.value = undoStack.isNotEmpty()
        _canRedo.value = redoStack.isNotEmpty()

        _project.value = updated
        val clampedPlayhead = _playheadMs.value.coerceIn(0L, updated.totalDurationMs.coerceAtLeast(0L))
        _playheadMs.value = clampedPlayhead

        viewModelScope.launch {
            projectRepository.saveProject(updated)
        }
        if (rebuildPlayer) {
            ensurePlayerAndRebuild(seekToMs = clampedPlayhead)
        }
    }

    private fun ensurePlayerAndRebuild(seekToMs: Long) {
        val current = _project.value ?: return
        if (current.clips.isEmpty()) return

        val context = getApplication<Application>()
        val player = _compositionPlayer ?: CompositionPlayer.Builder(context).build().also { created ->
            created.addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    _isPlaying.value = isPlaying
                    if (isPlaying) startPlayheadTicker() else stopPlayheadTicker()
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_ENDED) {
                        _isPlaying.value = false
                        stopPlayheadTicker()
                    }
                }
            })
            _compositionPlayer = created
            _playerState.value = created
        }

        val composition = Media3EffectFactory.buildComposition(
            context = context,
            project = current,
            includeExportOverlays = false
        )
        player.setComposition(composition)
        player.prepare()
        player.seekTo(seekToMs.coerceIn(0L, current.totalDurationMs.coerceAtLeast(0L)))
    }

    private fun startPlayheadTicker() {
        tickerJob?.cancel()
        tickerJob = viewModelScope.launch {
            while (isActive) {
                val player = _compositionPlayer
                if (player != null && player.isPlaying) {
                    _playheadMs.value = player.currentPosition.coerceAtLeast(0L)
                }
                delay(40L)
            }
        }
    }

    private fun stopPlayheadTicker() {
        tickerJob?.cancel()
        tickerJob = null
    }

    override fun onCleared() {
        super.onCleared()
        stopPlayheadTicker()
        exportEngine.cancelExport()
        _compositionPlayer?.release()
        _compositionPlayer = null
        _playerState.value = null
    }
}
