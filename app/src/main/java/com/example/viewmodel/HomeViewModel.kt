package com.example.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.PreferencesRepository
import com.example.data.ProjectRepository
import com.example.localization.AppLanguage
import com.example.media.BuiltInAudioGenerator
import com.example.media.ThumbnailExtractor
import com.example.model.ExportRecord
import com.example.model.StudioTemplate
import com.example.model.TextOverlayItem
import com.example.model.VideoClip
import com.example.model.VideoProject
import com.example.model.AudioTrackItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    val projectRepository = ProjectRepository(application, db.projectDao())
    private val prefsRepository = PreferencesRepository(application)

    val language: StateFlow<AppLanguage> = prefsRepository.languageFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppLanguage.KHMER)

    val projects: StateFlow<List<VideoProject>> = projectRepository.projectsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val exports: StateFlow<List<ExportRecord>> = projectRepository.exportsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isCreatingProject = MutableStateFlow(false)
    val isCreatingProject: StateFlow<Boolean> = _isCreatingProject.asStateFlow()

    fun toggleLanguage() {
        viewModelScope.launch {
            val next = if (language.value == AppLanguage.KHMER) AppLanguage.ENGLISH else AppLanguage.KHMER
            prefsRepository.setLanguage(next)
        }
    }

    fun createProjectFromUris(
        uris: List<Uri>,
        template: StudioTemplate? = null,
        onCreated: (String) -> Unit
    ) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            _isCreatingProject.value = true
            try {
                val context = getApplication<Application>()
                val clips = uris.mapIndexed { idx, uri ->
                    val persistedUri = projectRepository.persistMediaUri(uri, prefix = "clip")
                    val probe = ThumbnailExtractor.probeMedia(context, persistedUri)
                    val displayName = projectRepository.resolveDisplayName(uri)
                    VideoClip(
                        uriString = persistedUri,
                        isImage = probe.isImage,
                        originalDurationMs = probe.durationMs,
                        trimStartMs = 0L,
                        trimEndMs = probe.durationMs,
                        speed = if (probe.isImage) 1.0f else (template?.defaultSpeed ?: 1.0f),
                        filterPreset = template?.filterPreset ?: com.example.model.FilterPreset.ORIGINAL,
                        label = displayName.substringBeforeLast('.').take(16).ifBlank { "Clip ${idx + 1}" }
                    )
                }

                val audioTrack = if (template != null) {
                    val (beatUri, durMs) = BuiltInAudioGenerator.ensureBeatFile(context, template.builtInBeatId)
                    val beatInfo = BuiltInAudioGenerator.catalog.find { it.id == template.builtInBeatId }
                    AudioTrackItem(
                        uriString = beatUri,
                        title = beatInfo?.titleEn ?: "Studio Beat",
                        durationMs = durMs,
                        volume = 0.7f,
                        loop = true,
                        isBuiltInBeat = true
                    )
                } else null

                val overlays = if (template != null) {
                    val totalDur = clips.sumOf { it.effectiveDurationMs }.coerceAtLeast(3000L)
                    listOf(
                        TextOverlayItem(
                            text = template.starterOverlayText,
                            startMs = 0L,
                            endMs = totalDur.coerceAtMost(4500L),
                            xFraction = 0.5f,
                            yFraction = 0.78f,
                            stylePreset = template.starterOverlayStyle,
                            colorHex = 0xFFFFFFFF,
                            bgColorHex = if (template.starterOverlayStyle == com.example.model.TextStylePreset.CORAL_BADGE) {
                                0xFFFF3366
                            } else {
                                0xB30A0B10
                            }
                        )
                    )
                } else emptyList()

                val title = when {
                    template != null -> if (language.value == AppLanguage.KHMER) template.titleKm else template.titleEn
                    clips.firstOrNull()?.label?.isNotBlank() == true -> clips.first().label
                    else -> "ThyCut ${projects.value.size + 1}"
                }

                val project = VideoProject(
                    id = UUID.randomUUID().toString(),
                    title = title,
                    aspectRatio = template?.aspectRatio ?: com.example.model.AspectRatioPreset.RATIO_9_16,
                    clips = clips,
                    textOverlays = overlays,
                    audioTrack = audioTrack
                )
                projectRepository.saveProject(project)
                onCreated(project.id)
            } finally {
                _isCreatingProject.value = false
            }
        }
    }

    fun renameProject(project: VideoProject, newTitle: String) {
        val trimmed = newTitle.trim()
        if (trimmed.isBlank()) return
        viewModelScope.launch {
            projectRepository.saveProject(project.copy(title = trimmed))
        }
    }

    fun duplicateProject(project: VideoProject) {
        viewModelScope.launch {
            projectRepository.duplicateProject(project)
        }
    }

    fun deleteProject(projectId: String) {
        viewModelScope.launch {
            projectRepository.deleteProject(projectId)
        }
    }

    fun deleteExport(record: ExportRecord) {
        viewModelScope.launch {
            projectRepository.deleteExportRecord(record)
        }
    }
}
