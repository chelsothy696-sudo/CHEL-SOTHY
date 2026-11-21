package com.example.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class MediaProbeResult(
    val durationMs: Long,
    val isImage: Boolean,
    val width: Int,
    val height: Int
)

object ThumbnailExtractor {

    private val bitmapCache = object : LruCache<String, Bitmap>(64) {}

    fun getCachedFrame(key: String): Bitmap? = bitmapCache.get(key)

    suspend fun probeMedia(context: Context, uriString: String): MediaProbeResult =
        withContext(Dispatchers.IO) {
            val uri = Uri.parse(uriString)
            val mime = context.contentResolver.getType(uri).orEmpty()
            val lower = uriString.lowercase()
            val looksLikeImage = mime.startsWith("image/") ||
                lower.endsWith(".jpg") || lower.endsWith(".jpeg") ||
                lower.endsWith(".png") || lower.endsWith(".webp")

            if (looksLikeImage) {
                val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                runCatching {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        BitmapFactory.decodeStream(stream, null, opts)
                    }
                }
                return@withContext MediaProbeResult(
                    durationMs = 4000L,
                    isImage = true,
                    width = opts.outWidth.coerceAtLeast(1080),
                    height = opts.outHeight.coerceAtLeast(1080)
                )
            }

            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, uri)
                val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                val widthStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
                val heightStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
                val durationMs = durationStr?.toLongOrNull()?.coerceAtLeast(1000L) ?: 5000L
                val w = widthStr?.toIntOrNull() ?: 1080
                val h = heightStr?.toIntOrNull() ?: 1920
                MediaProbeResult(
                    durationMs = durationMs,
                    isImage = false,
                    width = w,
                    height = h
                )
            } catch (_: Exception) {
                MediaProbeResult(durationMs = 5000L, isImage = false, width = 1080, height = 1920)
            } finally {
                runCatching { retriever.release() }
            }
        }

    suspend fun probeAudioDuration(context: Context, uriString: String): Long =
        withContext(Dispatchers.IO) {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, Uri.parse(uriString))
                val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                durationStr?.toLongOrNull()?.coerceAtLeast(1000L) ?: 15000L
            } catch (_: Exception) {
                15000L
            } finally {
                runCatching { retriever.release() }
            }
        }

    suspend fun extractThumbnail(
        context: Context,
        uriString: String,
        isImage: Boolean,
        timeMs: Long,
        targetWidth: Int = 160
    ): Bitmap? = withContext(Dispatchers.IO) {
        val cacheKey = "${uriString}_${timeMs / 250}"
        bitmapCache.get(cacheKey)?.let { return@withContext it }

        val uri = Uri.parse(uriString)
        val bitmap: Bitmap? = if (isImage) {
            runCatching {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val opts = BitmapFactory.Options().apply { inSampleSize = 4 }
                    val decoded = BitmapFactory.decodeStream(stream, null, opts)
                    decoded?.let { scaleDown(it, targetWidth) }
                }
            }.getOrNull()
        } else {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, uri)
                val frame = retriever.getFrameAtTime(
                    timeMs * 1000L,
                    MediaMetadataRetriever.OPTION_CLOSEST_SYNC
                )
                frame?.let { scaleDown(it, targetWidth) }
            } catch (_: Exception) {
                null
            } finally {
                runCatching { retriever.release() }
            }
        }

        if (bitmap != null) {
            bitmapCache.put(cacheKey, bitmap)
        }
        bitmap
    }

    private fun scaleDown(src: Bitmap, targetWidth: Int): Bitmap {
        if (src.width <= targetWidth) return src
        val ratio = targetWidth.toFloat() / src.width.toFloat()
        val targetHeight = (src.height * ratio).toInt().coerceAtLeast(1)
        val scaled = Bitmap.createScaledBitmap(src, targetWidth, targetHeight, true)
        if (scaled != src) src.recycle()
        return scaled
    }
}
