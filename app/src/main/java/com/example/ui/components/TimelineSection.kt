package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Title
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.localization.ThyCutStrings
import com.example.media.ThumbnailExtractor
import com.example.model.AudioTrackItem
import com.example.model.FilterPreset
import com.example.model.TextOverlayItem
import com.example.model.VideoClip
import com.example.model.VideoProject
import com.example.ui.theme.MonoTimecodeStyle
import com.example.ui.theme.StudioAmber
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioCoral
import com.example.ui.theme.StudioCyan
import com.example.ui.theme.StudioEmerald
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.StudioSurfaceVariant
import com.example.ui.theme.StudioTextMuted
import com.example.ui.theme.StudioTextPrimary
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToLong
import kotlin.math.sin

@Composable
fun TimelineSection(
    project: VideoProject,
    strings: ThyCutStrings,
    playheadMs: Long,
    isPlaying: Boolean,
    selectedClipId: String?,
    selectedOverlayId: String?,
    timelineZoom: Float,
    onSeek: (Long) -> Unit,
    onSelectClip: (String) -> Unit,
    onSelectOverlay: (String) -> Unit,
    onTrimClip: (VideoClip, Long, Long) -> Unit,
    onAddClipClick: () -> Unit,
    onZoomChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val density = LocalDensity.current
    val pxPerSecond = 56f * timelineZoom
    val dpPerSecond = with(density) { pxPerSecond.toDp() }
    val pxPerMs = pxPerSecond / 1000f

    var isUserScrolling by remember { mutableStateOf(false) }

    LaunchedEffect(playheadMs, isPlaying, timelineZoom) {
        if (!isUserScrolling && (isPlaying || !scrollState.isScrollInProgress)) {
            val targetPx = (playheadMs * pxPerMs).toInt()
            if (abs(scrollState.value - targetPx) > 4) {
                scrollState.scrollTo(targetPx)
            }
        }
    }

    LaunchedEffect(scrollState.value, scrollState.isScrollInProgress) {
        if (scrollState.isScrollInProgress && !isPlaying) {
            isUserScrolling = true
            val newMs = (scrollState.value / pxPerMs).roundToLong()
                .coerceIn(0L, project.totalDurationMs.coerceAtLeast(0L))
            onSeek(newMs)
        } else if (!scrollState.isScrollInProgress && isUserScrolling) {
            isUserScrolling = false
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(StudioSurface)
    ) {
        // Track header & zoom bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TrackBadge(label = strings.videoTrackLabel, color = StudioCyan)
                if (project.textOverlays.isNotEmpty()) {
                    TrackBadge(label = strings.textTrackLabel, color = StudioAmber)
                }
                if (project.audioTrack != null) {
                    TrackBadge(label = strings.audioTrackLabel, color = StudioEmerald)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { onZoomChange(timelineZoom - 0.25f) },
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("timeline_zoom_out")
                ) {
                    Icon(
                        imageVector = Icons.Default.ZoomOut,
                        contentDescription = "Zoom out",
                        tint = StudioTextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Text(
                    text = "${String.format(Locale.US, "%.1f", timelineZoom)}x",
                    style = MonoTimecodeStyle,
                    color = StudioTextMuted
                )
                IconButton(
                    onClick = { onZoomChange(timelineZoom + 0.25f) },
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("timeline_zoom_in")
                ) {
                    Icon(
                        imageVector = Icons.Default.ZoomIn,
                        contentDescription = "Zoom in",
                        tint = StudioTextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(164.dp)
                .background(Color(0xFF0E1018))
        ) {
            val halfWidth = maxWidth / 2
            val totalSeconds = ((project.totalDurationMs + 1500L) / 1000L).coerceAtLeast(4L)
            val totalTrackWidth: Dp = with(density) { (project.totalDurationMs * pxPerMs).toDp() }

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .horizontalScroll(scrollState)
            ) {
                // Leading spacer so t=0 aligns with center playhead
                Spacer(modifier = Modifier.width(halfWidth))

                Column(
                    modifier = Modifier
                        .width(maxOf(totalTrackWidth + 88.dp, 220.dp))
                        .fillMaxHeight()
                ) {
                    // Time Ruler
                    TimeRulerRow(
                        totalSeconds = totalSeconds.toInt(),
                        dpPerSecond = dpPerSecond
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // V1 Video Filmstrip Track
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        project.clips.forEach { clip ->
                            val clipWidthDp = with(density) {
                                (clip.effectiveDurationMs * pxPerMs).coerceAtLeast(44f).toDp()
                            }
                            ClipFilmstripBlock(
                                clip = clip,
                                width = clipWidthDp,
                                isSelected = clip.id == selectedClipId,
                                pxPerMs = pxPerMs,
                                onClick = { onSelectClip(clip.id) },
                                onTrimChange = { newStart, newEnd ->
                                    onTrimClip(clip, newStart, newEnd)
                                }
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                        }

                        // Append clip (+) button
                        Surface(
                            onClick = onAddClipClick,
                            shape = RoundedCornerShape(10.dp),
                            color = StudioSurfaceElevated,
                            border = androidx.compose.foundation.BorderStroke(1.dp, StudioBorder),
                            modifier = Modifier
                                .padding(start = 6.dp)
                                .size(width = 48.dp, height = 56.dp)
                                .testTag("timeline_add_clip_button")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = strings.addClip,
                                    tint = StudioCyan
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // T1 Text Overlay Track
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(26.dp)
                    ) {
                        project.textOverlays.forEach { overlay ->
                            val startOffsetDp = with(density) { (overlay.startMs * pxPerMs).toDp() }
                            val widthDp = with(density) {
                                ((overlay.endMs - overlay.startMs) * pxPerMs).coerceAtLeast(44f).toDp()
                            }
                            val isSelected = overlay.id == selectedOverlayId
                            Row(
                                modifier = Modifier
                                    .offset(x = startOffsetDp)
                                    .width(widthDp)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        if (isSelected) StudioAmber.copy(alpha = 0.35f)
                                        else StudioAmber.copy(alpha = 0.18f)
                                    )
                                    .border(
                                        width = if (isSelected) 1.5.dp else 1.dp,
                                        color = StudioAmber,
                                        shape = RoundedCornerShape(6.dp)
                                    )
                                    .clickable { onSelectOverlay(overlay.id) }
                                    .padding(horizontal = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Title,
                                    contentDescription = null,
                                    tint = StudioAmber,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = overlay.text,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = StudioTextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // A1 Audio Track
                    project.audioTrack?.let { audio ->
                        val audioWidthDp = with(density) {
                            (project.totalDurationMs * pxPerMs).coerceAtLeast(64f).toDp()
                        }
                        AudioWaveformBar(
                            audio = audio,
                            width = audioWidthDp
                        )
                    }
                }

                // Trailing spacer so end of project can scroll to center playhead
                Spacer(modifier = Modifier.width(halfWidth))
            }

            // Fixed Center Playhead Line
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxHeight()
                    .width(16.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 10.dp, height = 10.dp)
                        .clip(CircleShape)
                        .background(StudioCyan)
                )
                Box(
                    modifier = Modifier
                        .padding(top = 6.dp)
                        .width(2.dp)
                        .fillMaxHeight()
                        .background(StudioCyan)
                )
            }
        }
    }
}

@Composable
private fun TrackBadge(label: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = color.copy(alpha = 0.16f)
    ) {
        Text(
            text = label,
            style = MonoTimecodeStyle.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
            color = color,
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
        )
    }
}

@Composable
private fun TimeRulerRow(
    totalSeconds: Int,
    dpPerSecond: Dp
) {
    Row(
        modifier = Modifier.height(20.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        for (sec in 0..totalSeconds) {
            Box(
                modifier = Modifier
                    .width(dpPerSecond)
                    .fillMaxHeight()
            ) {
                if (sec % 2 == 0) {
                    Text(
                        text = formatShortDuration(sec * 1000L),
                        style = MonoTimecodeStyle.copy(fontSize = 9.sp),
                        color = StudioTextMuted,
                        modifier = Modifier.align(Alignment.TopStart)
                    )
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .width(1.dp)
                        .height(if (sec % 2 == 0) 7.dp else 4.dp)
                        .background(StudioBorder)
                )
            }
        }
    }
}

@Composable
private fun ClipFilmstripBlock(
    clip: VideoClip,
    width: Dp,
    isSelected: Boolean,
    pxPerMs: Float,
    onClick: () -> Unit,
    onTrimChange: (Long, Long) -> Unit
) {
    val context = LocalContext.current
    val thumbCount = (width / 46.dp).toInt().coerceIn(1, 6)
    var frames by remember(clip.id, clip.uriString, clip.trimStartMs, clip.trimEndMs, thumbCount) {
        mutableStateOf<List<Bitmap?>>(emptyList())
    }

    LaunchedEffect(clip.id, clip.uriString, clip.trimStartMs, clip.trimEndMs, thumbCount) {
        val loaded = (0 until thumbCount).map { idx ->
            val frac = if (thumbCount == 1) 0.5f else idx.toFloat() / (thumbCount - 1)
            val tMs = clip.trimStartMs + (clip.trimmedSourceDurationMs * frac).toLong()
            ThumbnailExtractor.extractThumbnail(
                context = context,
                uriString = clip.uriString,
                isImage = clip.isImage,
                timeMs = tMs
            )
        }
        frames = loaded
    }

    var startDragAccum by remember(clip.id) { mutableFloatStateOf(0f) }
    var endDragAccum by remember(clip.id) { mutableFloatStateOf(0f) }

    Box(
        modifier = Modifier
            .width(width)
            .height(60.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(StudioSurfaceVariant)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) StudioCyan else StudioBorder,
                shape = RoundedCornerShape(8.dp)
            )
            .pointerInput(clip.id) {
                detectTapGestures(onTap = { onClick() })
            }
            .testTag("timeline_clip_${clip.id}")
    ) {
        // Filmstrip thumbnails
        Row(modifier = Modifier.fillMaxSize()) {
            val itemsToRender = if (frames.isEmpty()) List(thumbCount) { null } else frames
            itemsToRender.forEach { bmp ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    if (bmp != null) {
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = clip.label,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(StudioSurfaceElevated)
                        )
                    }
                }
            }
        }

        // Subtle dark gradient scrim for badges
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.22f))
        )

        // Duration & speed badges
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(horizontal = 6.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = Color.Black.copy(alpha = 0.65f)
            ) {
                Text(
                    text = formatShortDuration(clip.effectiveDurationMs),
                    style = MonoTimecodeStyle.copy(fontSize = 9.sp),
                    color = StudioTextPrimary,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                )
            }
            if (abs(clip.speed - 1.0f) > 0.02f) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = StudioCyan.copy(alpha = 0.85f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(9.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = "${clip.speed}x",
                            style = MonoTimecodeStyle.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                            color = Color.Black
                        )
                    }
                }
            }
            if (clip.isMuted) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.VolumeOff,
                    contentDescription = "Muted",
                    tint = StudioCoral,
                    modifier = Modifier.size(12.dp)
                )
            }
            if (clip.filterPreset != FilterPreset.ORIGINAL) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color(clip.filterPreset.previewColorHex))
                )
            }
        }

        // Left & Right Trim Handles when selected
        if (isSelected) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .width(14.dp)
                    .fillMaxHeight()
                    .background(StudioCyan)
                    .pointerInput(clip.id, clip.trimStartMs, clip.trimEndMs) {
                        detectHorizontalDragGestures(
                            onDragStart = { startDragAccum = 0f },
                            onHorizontalDrag = { _, dragAmount ->
                                startDragAccum += dragAmount
                                val deltaMs = (startDragAccum / pxPerMs).toLong()
                                if (abs(deltaMs) >= 100L) {
                                    startDragAccum = 0f
                                    onTrimChange(clip.trimStartMs + deltaMs, clip.trimEndMs)
                                }
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(22.dp)
                        .background(Color.Black, RoundedCornerShape(1.dp))
                )
            }

            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .width(14.dp)
                    .fillMaxHeight()
                    .background(StudioCyan)
                    .pointerInput(clip.id, clip.trimStartMs, clip.trimEndMs) {
                        detectHorizontalDragGestures(
                            onDragStart = { endDragAccum = 0f },
                            onHorizontalDrag = { _, dragAmount ->
                                endDragAccum += dragAmount
                                val deltaMs = (endDragAccum / pxPerMs).toLong()
                                if (abs(deltaMs) >= 100L) {
                                    endDragAccum = 0f
                                    onTrimChange(clip.trimStartMs, clip.trimEndMs + deltaMs)
                                }
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(22.dp)
                        .background(Color.Black, RoundedCornerShape(1.dp))
                )
            }
        }
    }
}

@Composable
private fun AudioWaveformBar(
    audio: AudioTrackItem,
    width: Dp
) {
    Box(
        modifier = Modifier
            .width(width)
            .height(28.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(StudioEmerald.copy(alpha = 0.16f))
            .border(1.dp, StudioEmerald.copy(alpha = 0.7f), RoundedCornerShape(6.dp))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val barSpacing = 6f
            val count = (size.width / barSpacing).toInt().coerceAtLeast(4)
            for (i in 0 until count) {
                val amp = 0.25f + 0.65f * abs(sin(i * 0.48f))
                val barH = size.height * amp
                val x = i * barSpacing + 2f
                val top = (size.height - barH) / 2f
                drawLine(
                    color = StudioEmerald.copy(alpha = 0.45f),
                    start = Offset(x, top),
                    end = Offset(x, top + barH),
                    strokeWidth = 3f
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = null,
                tint = StudioEmerald,
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = audio.title,
                style = MaterialTheme.typography.labelSmall,
                color = StudioTextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
