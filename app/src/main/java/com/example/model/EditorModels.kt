package com.example.model

import androidx.annotation.DrawableRes
import com.example.R
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
enum class AspectRatioPreset(
    val label: String,
    val widthRatio: Float,
    val heightRatio: Float,
    val exportWidth720: Int,
    val exportHeight720: Int,
    val exportWidth1080: Int,
    val exportHeight1080: Int
) {
    RATIO_9_16("9:16", 9f, 16f, 720, 1280, 1080, 1920),
    RATIO_16_9("16:9", 16f, 9f, 1280, 720, 1920, 1080),
    RATIO_1_1("1:1", 1f, 1f, 720, 720, 1080, 1080),
    RATIO_4_5("4:5", 4f, 5f, 720, 900, 1080, 1350),
    RATIO_4_3("4:3", 4f, 3f, 960, 720, 1440, 1080);

    val ratioFloat: Float get() = widthRatio / heightRatio
}

@Serializable
enum class FilterPreset(
    val titleEn: String,
    val titleKm: String,
    val previewColorHex: Long
) {
    ORIGINAL("Original", "ដើម", 0xFF64748B),
    TEAL_ORANGE("Cinema", "កុន", 0xFF06B6D4),
    GOLDEN_HOUR("Golden", "ពន្លឺថ្ងៃ", 0xFFF59E0B),
    CYBER_NEON("Cyber", "ណេអុង", 0xFFA855F7),
    MONO_NOIR("Noir", "ស-ខ្មៅ", 0xFF94A3B8),
    VIVID_POP("Vivid", "ពណ៌ដិត", 0xFFFF3366),
    WARM_FILM("Vintage", "ហ្វីលចាស់", 0xFFEA580C)
}

@Serializable
enum class TextStylePreset(
    val label: String
) {
    CLEAN("Clean"),
    NEON_CYAN("Neon"),
    CORAL_BADGE("Coral"),
    SUBTITLE_PILL("Subtitle"),
    CINEMA_BOLD("Cinema")
}

@Serializable
enum class ExportResolution(val label: String, val heightPx: Int) {
    RES_720P("720p HD", 720),
    RES_1080P("1080p FHD", 1080)
}

@Serializable
data class VideoClip(
    val id: String = UUID.randomUUID().toString(),
    val uriString: String,
    val isImage: Boolean = false,
    val originalDurationMs: Long = 5000L,
    val trimStartMs: Long = 0L,
    val trimEndMs: Long = 5000L,
    val speed: Float = 1.0f,
    val volume: Float = 1.0f,
    val rotationDegrees: Int = 0,
    val filterPreset: FilterPreset = FilterPreset.ORIGINAL,
    val brightness: Float = 0f,     // -0.5f .. 0.5f
    val contrast: Float = 0f,       // -0.5f .. 0.5f
    val saturation: Float = 0f,     // -1.0f .. 1.0f
    val isMuted: Boolean = false,
    val label: String = "Clip"
) {
    val trimmedSourceDurationMs: Long
        get() = (trimEndMs - trimStartMs).coerceAtLeast(250L)

    val effectiveDurationMs: Long
        get() = (trimmedSourceDurationMs / speed.coerceIn(0.25f, 4.0f)).toLong().coerceAtLeast(250L)
}

@Serializable
data class TextOverlayItem(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val startMs: Long = 0L,
    val endMs: Long = 3500L,
    val xFraction: Float = 0.5f,
    val yFraction: Float = 0.78f,
    val colorHex: Long = 0xFFFFFFFF,
    val bgColorHex: Long = 0x990A0B10,
    val fontSizeSp: Float = 22f,
    val stylePreset: TextStylePreset = TextStylePreset.SUBTITLE_PILL,
    val isStickerBadge: Boolean = false
)

@Serializable
data class AudioTrackItem(
    val id: String = UUID.randomUUID().toString(),
    val uriString: String,
    val title: String,
    val durationMs: Long = 15000L,
    val volume: Float = 0.75f,
    val loop: Boolean = true,
    val isBuiltInBeat: Boolean = false
)

@Serializable
data class VideoProject(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "ThyCut Project",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val aspectRatio: AspectRatioPreset = AspectRatioPreset.RATIO_9_16,
    val clips: List<VideoClip> = emptyList(),
    val textOverlays: List<TextOverlayItem> = emptyList(),
    val audioTrack: AudioTrackItem? = null,
    val exportResolution: ExportResolution = ExportResolution.RES_1080P,
    val exportFps: Int = 30
) {
    val totalDurationMs: Long
        get() = clips.sumOf { it.effectiveDurationMs }.coerceAtLeast(0L)

    fun clipTimeRange(clipIndex: Int): LongRange {
        var cursor = 0L
        for (i in clips.indices) {
            val dur = clips[i].effectiveDurationMs
            if (i == clipIndex) {
                return cursor until (cursor + dur)
            }
            cursor += dur
        }
        return 0L until 0L
    }

    fun clipAtTimelinePosition(positionMs: Long): Pair<Int, Long>? {
        if (clips.isEmpty()) return null
        var cursor = 0L
        for (i in clips.indices) {
            val dur = clips[i].effectiveDurationMs
            if (positionMs < cursor + dur || i == clips.lastIndex) {
                return i to (positionMs - cursor).coerceIn(0L, dur)
            }
            cursor += dur
        }
        return null
    }
}

data class ExportRecord(
    val id: String,
    val projectTitle: String,
    val filePath: String,
    val mediaStoreUri: String?,
    val durationMs: Long,
    val resolutionLabel: String,
    val fileSizeBytes: Long,
    val createdAt: Long
)

data class StudioTemplate(
    val id: String,
    val titleKm: String,
    val titleEn: String,
    val tagKm: String,
    val tagEn: String,
    @DrawableRes val coverResId: Int,
    val aspectRatio: AspectRatioPreset,
    val filterPreset: FilterPreset,
    val defaultSpeed: Float,
    val starterOverlayText: String,
    val starterOverlayStyle: TextStylePreset,
    val builtInBeatId: String
)

object StudioTemplates {
    val all = listOf(
        StudioTemplate(
            id = "tpl_neon_night",
            titleKm = "ភ្នំពេញរាត្រីណេអុង",
            titleEn = "Neon Night Reel",
            tagKm = "9:16 • ពណ៌កុន",
            tagEn = "9:16 • Cinema LUT",
            coverResId = R.drawable.img_template_cinematic,
            aspectRatio = AspectRatioPreset.RATIO_9_16,
            filterPreset = FilterPreset.TEAL_ORANGE,
            defaultSpeed = 1.0f,
            starterOverlayText = "NIGHT VIBES",
            starterOverlayStyle = TextStylePreset.NEON_CYAN,
            builtInBeatId = "beat_lofi"
        ),
        StudioTemplate(
            id = "tpl_golden_vlog",
            titleKm = "ដំណើរកម្សាន្តឆ្នេរសមុទ្រ",
            titleEn = "Golden Coast Vlog",
            tagKm = "16:9 • ពន្លឺថ្ងៃ",
            tagEn = "16:9 • Golden Hour",
            coverResId = R.drawable.img_template_vlog,
            aspectRatio = AspectRatioPreset.RATIO_16_9,
            filterPreset = FilterPreset.GOLDEN_HOUR,
            defaultSpeed = 1.0f,
            starterOverlayText = "SUMMER DIARY",
            starterOverlayStyle = TextStylePreset.CLEAN,
            builtInBeatId = "beat_breeze"
        ),
        StudioTemplate(
            id = "tpl_beat_sync",
            titleKm = "ចង្វាក់ភ្លេង 1.25x",
            titleEn = "Bass Beat Sync",
            tagKm = "9:16 • ល្បឿនលឿន",
            tagEn = "9:16 • Beat Pulse",
            coverResId = R.drawable.img_template_beats,
            aspectRatio = AspectRatioPreset.RATIO_9_16,
            filterPreset = FilterPreset.CYBER_NEON,
            defaultSpeed = 1.25f,
            starterOverlayText = "DROP THE BEAT",
            starterOverlayStyle = TextStylePreset.CORAL_BADGE,
            builtInBeatId = "beat_cyber"
        )
    )
}
