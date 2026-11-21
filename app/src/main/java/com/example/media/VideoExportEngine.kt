package com.example.media

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import androidx.annotation.OptIn
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.transformer.Composition
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.Transformer
import com.example.model.ExportRecord
import com.example.model.VideoProject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.util.UUID

sealed interface ExportState {
    data object Idle : ExportState
    data class Running(val progressPercent: Int) : ExportState
    data class Success(val record: ExportRecord) : ExportState
    data class Error(val message: String) : ExportState
}

@OptIn(UnstableApi::class)
class VideoExportEngine(private val context: Context) {

    private val mainHandler = Handler(Looper.getMainLooper())
    private var activeTransformer: Transformer? = null
    private var progressRunnable: Runnable? = null

    private val _exportState = MutableStateFlow<ExportState>(ExportState.Idle)
    val exportState: StateFlow<ExportState> = _exportState.asStateFlow()

    fun startExport(
        project: VideoProject,
        onRecordReady: (ExportRecord) -> Unit
    ) {
        if (project.clips.isEmpty()) {
            _exportState.value = ExportState.Error("No clips in project")
            return
        }

        cancelExport()
        _exportState.value = ExportState.Running(0)

        val exportsDir = File(context.filesDir, "exports").apply { mkdirs() }
        val safeTitle = project.title.replace(Regex("[^A-Za-z0-9_-]"), "_").take(24)
        val outFile = File(exportsDir, "ThyCut_${safeTitle}_${System.currentTimeMillis()}.mp4")

        val composition = Media3EffectFactory.buildComposition(
            context = context,
            project = project,
            includeExportOverlays = true
        )

        val listener = object : Transformer.Listener {
            override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                stopProgressPolling()
                val mediaStoreUri = saveToMediaStore(outFile, outFile.name)
                val record = ExportRecord(
                    id = UUID.randomUUID().toString(),
                    projectTitle = project.title,
                    filePath = outFile.absolutePath,
                    mediaStoreUri = mediaStoreUri,
                    durationMs = project.totalDurationMs,
                    resolutionLabel = "${project.exportResolution.label} • ${project.exportFps}fps",
                    fileSizeBytes = outFile.length(),
                    createdAt = System.currentTimeMillis()
                )
                onRecordReady(record)
                _exportState.value = ExportState.Success(record)
            }

            override fun onError(
                composition: Composition,
                exportResult: ExportResult,
                exportException: ExportException
            ) {
                stopProgressPolling()
                _exportState.value = ExportState.Error(
                    exportException.localizedMessage ?: "Export failed (${exportException.errorCodeName})"
                )
            }
        }

        val transformer = Transformer.Builder(context)
            .setVideoMimeType(MimeTypes.VIDEO_H264)
            .setAudioMimeType(MimeTypes.AUDIO_AAC)
            .addListener(listener)
            .build()

        activeTransformer = transformer
        transformer.start(composition, outFile.absolutePath)

        val holder = ProgressHolder()
        val runnable = object : Runnable {
            override fun run() {
                val t = activeTransformer ?: return
                val state = t.getProgress(holder)
                if (state == Transformer.PROGRESS_STATE_AVAILABLE) {
                    _exportState.value = ExportState.Running(holder.progress.coerceIn(0, 99))
                }
                if (state != Transformer.PROGRESS_STATE_NOT_STARTED) {
                    mainHandler.postDelayed(this, 160L)
                }
            }
        }
        progressRunnable = runnable
        mainHandler.postDelayed(runnable, 160L)
    }

    fun cancelExport() {
        stopProgressPolling()
        activeTransformer?.cancel()
        activeTransformer = null
        if (_exportState.value is ExportState.Running) {
            _exportState.value = ExportState.Idle
        }
    }

    fun resetState() {
        _exportState.value = ExportState.Idle
    }

    private fun stopProgressPolling() {
        progressRunnable?.let { mainHandler.removeCallbacks(it) }
        progressRunnable = null
    }

    private fun saveToMediaStore(sourceFile: File, displayName: String): String? {
        if (!sourceFile.exists() || Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return null
        }
        return runCatching {
            val values = ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, displayName)
                put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                put(MediaStore.Video.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MOVIES}/ThyCut")
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
                ?: return@runCatching null
            resolver.openOutputStream(uri)?.use { out ->
                sourceFile.inputStream().use { input ->
                    input.copyTo(out)
                }
            }
            values.clear()
            values.put(MediaStore.Video.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            uri.toString()
        }.getOrNull()
    }
}
