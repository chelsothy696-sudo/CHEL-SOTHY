package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.model.ExportRecord
import java.io.File
import java.util.Locale

fun formatTimecode(ms: Long): String {
    val safeMs = ms.coerceAtLeast(0L)
    val totalSeconds = safeMs / 1000L
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    val centis = (safeMs % 1000L) / 10L
    return String.format(Locale.US, "%02d:%02d.%02d", minutes, seconds, centis)
}

fun formatShortDuration(ms: Long): String {
    val safeMs = ms.coerceAtLeast(0L)
    val totalSeconds = (safeMs + 500L) / 1000L
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return String.format(Locale.US, "%02d:%02d", minutes, seconds)
}

fun formatFileSize(bytes: Long): String {
    if (bytes <= 0L) return "0 MB"
    val mb = bytes / (1024.0 * 1024.0)
    return if (mb >= 1.0) {
        String.format(Locale.US, "%.1f MB", mb)
    } else {
        val kb = bytes / 1024.0
        String.format(Locale.US, "%.0f KB", kb)
    }
}

fun shareExportedVideo(context: Context, record: ExportRecord) {
    val file = File(record.filePath)
    val shareUri: Uri? = when {
        file.exists() -> runCatching {
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        }.getOrNull()
        record.mediaStoreUri != null -> Uri.parse(record.mediaStoreUri)
        else -> null
    }
    if (shareUri == null) return

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "video/mp4"
        putExtra(Intent.EXTRA_STREAM, shareUri)
        putExtra(Intent.EXTRA_SUBJECT, record.projectTitle)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, record.projectTitle))
}
