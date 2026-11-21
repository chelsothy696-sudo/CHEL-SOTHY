package com.example.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.annotation.OptIn
import androidx.core.content.res.ResourcesCompat
import androidx.media3.common.Effect
import androidx.media3.common.MediaItem
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.BitmapOverlay
import androidx.media3.effect.Brightness
import androidx.media3.effect.Contrast
import androidx.media3.effect.HslAdjustment
import androidx.media3.effect.OverlayEffect
import androidx.media3.effect.Presentation
import androidx.media3.effect.RgbFilter
import androidx.media3.effect.RgbMatrix
import androidx.media3.effect.ScaleAndRotateTransformation
import androidx.media3.effect.SpeedChangeEffect
import androidx.media3.effect.StaticOverlaySettings
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.Effects
import com.example.R
import com.example.model.ExportResolution
import com.example.model.FilterPreset
import com.example.model.TextOverlayItem
import com.example.model.TextStylePreset
import com.example.model.VideoClip
import com.example.model.VideoProject
import java.nio.ByteBuffer
import kotlin.math.abs

@OptIn(UnstableApi::class)
class VolumeAudioProcessor(private val gain: Float) : BaseAudioProcessor() {

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        if (inputAudioFormat.encoding != androidx.media3.common.C.ENCODING_PCM_16BIT) {
            throw AudioProcessor.UnhandledAudioFormatException(inputAudioFormat)
        }
        return inputAudioFormat
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val remaining = inputBuffer.remaining()
        if (remaining == 0) return

        val outputBuffer = replaceOutputBuffer(remaining)
        if (abs(gain - 1.0f) < 0.01f) {
            outputBuffer.put(inputBuffer)
        } else {
            while (inputBuffer.hasRemaining()) {
                val sample = inputBuffer.short
                val scaled = (sample * gain).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                outputBuffer.putShort(scaled)
            }
        }
        outputBuffer.flip()
    }
}

@OptIn(UnstableApi::class)
object Media3EffectFactory {

    fun buildComposition(
        context: Context,
        project: VideoProject,
        includeExportOverlays: Boolean
    ): Composition {
        val resolution = project.exportResolution
        val targetW = when (resolution) {
            ExportResolution.RES_720P -> project.aspectRatio.exportWidth720
            ExportResolution.RES_1080P -> project.aspectRatio.exportWidth1080
        }
        val targetH = when (resolution) {
            ExportResolution.RES_720P -> project.aspectRatio.exportHeight720
            ExportResolution.RES_1080P -> project.aspectRatio.exportHeight1080
        }

        val editedClips = project.clips.map { clip ->
            buildEditedVideoClip(clip, project.exportFps)
        }

        val videoSequence = EditedMediaItemSequence.withAudioAndVideoFrom(editedClips)
        val sequences = mutableListOf(videoSequence)

        project.audioTrack?.let { audio ->
            val audioMediaItem = MediaItem.fromUri(Uri.parse(audio.uriString))
            val audioProcessor = VolumeAudioProcessor(audio.volume.coerceIn(0f, 1.5f))
            val editedAudio = EditedMediaItem.Builder(audioMediaItem)
                .setDurationUs(audio.durationMs * 1000L)
                .setEffects(Effects(listOf(audioProcessor), emptyList()))
                .build()
            val audioSequence = EditedMediaItemSequence.withAudioFrom(listOf(editedAudio))
                .buildUpon()
                .setIsLooping(audio.loop)
                .build()
            sequences.add(audioSequence)
        }

        val compositionVideoEffects = mutableListOf<Effect>(
            Presentation.createForWidthAndHeight(
                targetW,
                targetH,
                Presentation.LAYOUT_SCALE_TO_FIT
            )
        )

        if (includeExportOverlays && project.textOverlays.isNotEmpty()) {
            val overlays = project.textOverlays.map { item ->
                createBitmapOverlay(context, item, targetW, targetH)
            }
            if (overlays.isNotEmpty()) {
                compositionVideoEffects.add(OverlayEffect(overlays))
            }
        }

        return Composition.Builder(sequences)
            .setEffects(Effects(emptyList(), compositionVideoEffects))
            .build()
    }

    private fun buildEditedVideoClip(clip: VideoClip, fps: Int): EditedMediaItem {
        val uri = Uri.parse(clip.uriString)
        val mediaItemBuilder = MediaItem.Builder().setUri(uri)

        if (clip.isImage) {
            mediaItemBuilder.setImageDurationMs(clip.trimmedSourceDurationMs)
        } else {
            mediaItemBuilder.setClippingConfiguration(
                MediaItem.ClippingConfiguration.Builder()
                    .setStartPositionMs(clip.trimStartMs)
                    .setEndPositionMs(clip.trimEndMs)
                    .build()
            )
        }

        val videoEffects = mutableListOf<Effect>()

        if (clip.rotationDegrees % 360 != 0) {
            videoEffects.add(
                ScaleAndRotateTransformation.Builder()
                    .setRotationDegrees((360 - (clip.rotationDegrees % 360)).toFloat())
                    .build()
            )
        }

        if (abs(clip.speed - 1.0f) > 0.02f && !clip.isImage) {
            videoEffects.add(SpeedChangeEffect(clip.speed))
        }

        videoEffects.addAll(buildFilterEffects(clip))

        val audioProcessors = mutableListOf<AudioProcessor>()
        val effectiveVol = if (clip.isMuted) 0f else clip.volume
        if (!clip.isImage && abs(effectiveVol - 1.0f) > 0.02f) {
            audioProcessors.add(VolumeAudioProcessor(effectiveVol))
        }

        val builder = EditedMediaItem.Builder(mediaItemBuilder.build())
            .setEffects(Effects(audioProcessors, videoEffects))
            .setRemoveAudio(clip.isImage || clip.isMuted)

        if (clip.isImage) {
            builder.setFrameRate(fps)
        } else {
            builder.setDurationUs(clip.originalDurationMs * 1000L)
        }

        return builder.build()
    }

    private fun buildFilterEffects(clip: VideoClip): List<Effect> {
        val effects = mutableListOf<Effect>()

        when (clip.filterPreset) {
            FilterPreset.ORIGINAL -> Unit
            FilterPreset.TEAL_ORANGE -> {
                effects.add(Contrast(0.16f))
                effects.add(HslAdjustment.Builder().adjustSaturation(18f).build())
                effects.add(createTintMatrix(r = 1.06f, g = 1.0f, b = 1.08f))
            }
            FilterPreset.GOLDEN_HOUR -> {
                effects.add(Brightness(0.06f))
                effects.add(HslAdjustment.Builder().adjustSaturation(22f).adjustHue(-6f).build())
                effects.add(createTintMatrix(r = 1.12f, g = 1.03f, b = 0.88f))
            }
            FilterPreset.CYBER_NEON -> {
                effects.add(Contrast(0.22f))
                effects.add(HslAdjustment.Builder().adjustSaturation(38f).adjustHue(12f).build())
                effects.add(createTintMatrix(r = 1.04f, g = 0.94f, b = 1.16f))
            }
            FilterPreset.MONO_NOIR -> {
                effects.add(RgbFilter.createGrayscaleFilter())
                effects.add(Contrast(0.24f))
            }
            FilterPreset.VIVID_POP -> {
                effects.add(Contrast(0.14f))
                effects.add(HslAdjustment.Builder().adjustSaturation(45f).build())
            }
            FilterPreset.WARM_FILM -> {
                effects.add(Brightness(0.04f))
                effects.add(Contrast(-0.06f))
                effects.add(createTintMatrix(r = 1.10f, g = 1.02f, b = 0.86f))
            }
        }

        if (abs(clip.brightness) > 0.01f) {
            effects.add(Brightness(clip.brightness.coerceIn(-0.8f, 0.8f)))
        }
        if (abs(clip.contrast) > 0.01f) {
            effects.add(Contrast(clip.contrast.coerceIn(-0.8f, 0.8f)))
        }
        if (abs(clip.saturation) > 0.01f) {
            effects.add(
                HslAdjustment.Builder()
                    .adjustSaturation((clip.saturation * 100f).coerceIn(-100f, 100f))
                    .build()
            )
        }

        return effects
    }

    private fun createTintMatrix(r: Float, g: Float, b: Float): RgbMatrix {
        val matrix = floatArrayOf(
            r, 0f, 0f, 0f,
            0f, g, 0f, 0f,
            0f, 0f, b, 0f,
            0f, 0f, 0f, 1f
        )
        return RgbMatrix { _, _ -> matrix }
    }

    private fun createBitmapOverlay(
        context: Context,
        item: TextOverlayItem,
        canvasW: Int,
        canvasH: Int
    ): BitmapOverlay {
        val bitmap = Bitmap.createBitmap(canvasW, canvasH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val typeface = runCatching {
            ResourcesCompat.getFont(context, R.font.noto_sans_khmer)
        }.getOrNull() ?: Typeface.DEFAULT_BOLD

        val scaleFactor = canvasW / 360f
        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = item.colorHex.toInt()
            textSize = item.fontSizeSp * scaleFactor
            this.typeface = Typeface.create(typeface, Typeface.BOLD)
            if (item.stylePreset == TextStylePreset.NEON_CYAN) {
                setShadowLayer(12f * scaleFactor, 0f, 0f, Color.parseColor("#00E5FF"))
            } else if (item.stylePreset == TextStylePreset.CLEAN || item.stylePreset == TextStylePreset.CINEMA_BOLD) {
                setShadowLayer(6f * scaleFactor, 0f, 3f * scaleFactor, Color.BLACK)
            }
        }

        val maxTextW = (canvasW * 0.84f).toInt().coerceAtLeast(100)
        val staticLayout = StaticLayout.Builder
            .obtain(item.text, 0, item.text.length, textPaint, maxTextW)
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .build()

        var maxLineW = 0f
        for (line in 0 until staticLayout.lineCount) {
            maxLineW = maxOf(maxLineW, staticLayout.getLineWidth(line))
        }
        val textH = staticLayout.height.toFloat()

        val centerX = item.xFraction.coerceIn(0.1f, 0.9f) * canvasW
        val centerY = item.yFraction.coerceIn(0.1f, 0.9f) * canvasH

        val padH = 14f * scaleFactor
        val padV = 8f * scaleFactor
        val boxRect = RectF(
            centerX - maxLineW / 2f - padH,
            centerY - textH / 2f - padV,
            centerX + maxLineW / 2f + padH,
            centerY + textH / 2f + padV
        )

        val bgAlpha = (item.bgColorHex ushr 24) and 0xFF
        if (bgAlpha > 0 && item.stylePreset != TextStylePreset.CLEAN) {
            val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = item.bgColorHex.toInt()
                style = Paint.Style.FILL
            }
            val radius = if (item.isStickerBadge) 24f * scaleFactor else 14f * scaleFactor
            canvas.drawRoundRect(boxRect, radius, radius, bgPaint)

            if (item.stylePreset == TextStylePreset.NEON_CYAN) {
                val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#00E5FF")
                    style = Paint.Style.STROKE
                    strokeWidth = 2.5f * scaleFactor
                }
                canvas.drawRoundRect(boxRect, radius, radius, strokePaint)
            }
        }

        canvas.save()
        canvas.translate(centerX - maxTextW / 2f, centerY - textH / 2f)
        staticLayout.draw(canvas)
        canvas.restore()

        val settings = StaticOverlaySettings.Builder().build()
        return BitmapOverlay.createStaticBitmapOverlay(bitmap, settings)
    }
}
