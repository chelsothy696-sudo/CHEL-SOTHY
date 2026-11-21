package com.example.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import com.example.model.ExportRecord
import com.example.model.VideoProject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.security.MessageDigest

class ProjectRepository(
    private val context: Context,
    private val dao: ProjectDao
) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    val projectsFlow: Flow<List<VideoProject>> = dao.observeProjects().map { entities ->
        entities.mapNotNull { entity ->
            runCatching { json.decodeFromString<VideoProject>(entity.payloadJson) }.getOrNull()
        }
    }

    val exportsFlow: Flow<List<ExportRecord>> = dao.observeExports().map { entities ->
        entities.map { entity ->
            ExportRecord(
                id = entity.id,
                projectTitle = entity.projectTitle,
                filePath = entity.filePath,
                mediaStoreUri = entity.mediaStoreUri,
                durationMs = entity.durationMs,
                resolutionLabel = entity.resolutionLabel,
                fileSizeBytes = entity.fileSizeBytes,
                createdAt = entity.createdAt
            )
        }
    }

    suspend fun getProject(id: String): VideoProject? = withContext(Dispatchers.IO) {
        dao.getProjectById(id)?.let { entity ->
            runCatching { json.decodeFromString<VideoProject>(entity.payloadJson) }.getOrNull()
        }
    }

    suspend fun saveProject(project: VideoProject) = withContext(Dispatchers.IO) {
        val updated = project.copy(updatedAt = System.currentTimeMillis())
        val payload = json.encodeToString(updated)
        dao.upsertProject(
            ProjectEntity(
                id = updated.id,
                title = updated.title,
                updatedAt = updated.updatedAt,
                durationMs = updated.totalDurationMs,
                clipCount = updated.clips.size,
                thumbnailUri = updated.clips.firstOrNull()?.uriString,
                payloadJson = payload
            )
        )
    }

    suspend fun deleteProject(id: String) = withContext(Dispatchers.IO) {
        dao.deleteProjectById(id)
    }

    suspend fun duplicateProject(project: VideoProject): VideoProject = withContext(Dispatchers.IO) {
        val copy = project.copy(
            id = java.util.UUID.randomUUID().toString(),
            title = "${project.title} (Copy)",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        saveProject(copy)
        copy
    }

    suspend fun addExportRecord(record: ExportRecord) = withContext(Dispatchers.IO) {
        dao.insertExport(
            ExportEntity(
                id = record.id,
                projectTitle = record.projectTitle,
                filePath = record.filePath,
                mediaStoreUri = record.mediaStoreUri,
                durationMs = record.durationMs,
                resolutionLabel = record.resolutionLabel,
                fileSizeBytes = record.fileSizeBytes,
                createdAt = record.createdAt
            )
        )
    }

    suspend fun deleteExportRecord(record: ExportRecord) = withContext(Dispatchers.IO) {
        runCatching {
            val f = File(record.filePath)
            if (f.exists()) f.delete()
        }
        dao.deleteExportById(record.id)
    }

    /**
     * Ensures picked URIs remain readable across app restarts.
     * Attempts persistable permission first; if unsupported by provider (common on Photo Picker),
     * copies the asset into app-private project_media cache.
     */
    suspend fun persistMediaUri(uri: Uri, prefix: String = "media"): String = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val persisted = try {
            resolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            true
        } catch (_: Exception) {
            false
        }
        if (persisted) return@withContext uri.toString()

        val dir = File(context.filesDir, "project_media").apply { mkdirs() }
        val mime = resolver.getType(uri).orEmpty()
        val ext = when {
            mime.contains("mp4") -> "mp4"
            mime.contains("webm") -> "webm"
            mime.contains("png") -> "png"
            mime.contains("webp") -> "webp"
            mime.contains("jpeg") || mime.contains("jpg") -> "jpg"
            mime.contains("mpeg") || mime.contains("mp3") -> "mp3"
            mime.contains("wav") -> "wav"
            mime.contains("m4a") || mime.contains("mp4a") -> "m4a"
            mime.startsWith("video/") -> "mp4"
            mime.startsWith("image/") -> "jpg"
            mime.startsWith("audio/") -> "m4a"
            else -> "bin"
        }
        val hash = sha1Short(uri.toString())
        val destFile = File(dir, "${prefix}_${hash}.$ext")
        if (!destFile.exists() || destFile.length() == 0L) {
            resolver.openInputStream(uri)?.use { input ->
                destFile.outputStream().use { output ->
                    input.copyTo(output, bufferSize = 65_536)
                }
            }
        }
        if (destFile.exists() && destFile.length() > 0L) {
            Uri.fromFile(destFile).toString()
        } else {
            uri.toString()
        }
    }

    fun resolveDisplayName(uri: Uri): String {
        return runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (idx >= 0) return@runCatching cursor.getString(idx)
                    }
                    null
                }
        }.getOrNull() ?: uri.lastPathSegment ?: "Media"
    }

    private fun sha1Short(value: String): String {
        val digest = MessageDigest.getInstance("SHA-1").digest(value.toByteArray())
        return digest.take(8).joinToString("") { "%02x".format(it) }
    }
}
