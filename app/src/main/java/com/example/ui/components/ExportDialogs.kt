package com.example.ui.components

import android.net.Uri
import androidx.annotation.OptIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.compose.ContentFrame
import androidx.media3.ui.compose.SURFACE_TYPE_TEXTURE_VIEW
import com.example.localization.ThyCutStrings
import com.example.media.ExportState
import com.example.model.ExportRecord
import com.example.model.ExportResolution
import com.example.model.VideoProject
import com.example.ui.theme.MonoTimecodeStyle
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioCoral
import com.example.ui.theme.StudioCyan
import com.example.ui.theme.StudioEmerald
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.StudioTextMuted
import com.example.ui.theme.StudioTextPrimary
import java.io.File

@Composable
fun ExportSettingsDialog(
    project: VideoProject,
    strings: ThyCutStrings,
    onSelectResolution: (ExportResolution) -> Unit,
    onSelectFps: (Int) -> Unit,
    onStartExport: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = StudioSurfaceElevated,
            border = BorderStroke(1.dp, StudioBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = strings.exportBtn,
                        style = MaterialTheme.typography.titleLarge,
                        color = StudioTextPrimary
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = strings.cancelLabel, tint = StudioTextMuted)
                    }
                }

                Text(
                    text = strings.resolutionLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = StudioTextMuted
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ExportResolution.entries.forEach { res ->
                        FilterChip(
                            selected = project.exportResolution == res,
                            onClick = { onSelectResolution(res) },
                            label = { Text(res.label, style = MonoTimecodeStyle) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = StudioCyan,
                                selectedLabelColor = Color.Black
                            ),
                            modifier = Modifier.testTag("export_res_${res.name}")
                        )
                    }
                }

                Text(
                    text = strings.frameRateLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = StudioTextMuted
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf(24, 30, 60).forEach { fps ->
                        FilterChip(
                            selected = project.exportFps == fps,
                            onClick = { onSelectFps(fps) },
                            label = { Text("${fps}fps", style = MonoTimecodeStyle) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = StudioCyan,
                                selectedLabelColor = Color.Black
                            ),
                            modifier = Modifier.testTag("export_fps_$fps")
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.Black.copy(alpha = 0.25f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${project.aspectRatio.label} • ${project.clips.size} clips",
                            style = MonoTimecodeStyle,
                            color = StudioTextMuted
                        )
                        Text(
                            text = formatShortDuration(project.totalDurationMs),
                            style = MonoTimecodeStyle,
                            color = StudioCyan
                        )
                    }
                }

                Button(
                    onClick = onStartExport,
                    colors = ButtonDefaults.buttonColors(containerColor = StudioCyan, contentColor = Color.Black),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("confirm_start_export_button")
                ) {
                    Text(strings.startExport, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun ExportProgressModal(
    exportState: ExportState,
    strings: ThyCutStrings,
    onCancel: () -> Unit,
    onDismiss: () -> Unit,
    onPlayExport: (ExportRecord) -> Unit,
    onShareExport: (ExportRecord) -> Unit
) {
    if (exportState is ExportState.Idle) return

    Dialog(onDismissRequest = { if (exportState !is ExportState.Running) onDismiss() }) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = StudioSurfaceElevated,
            border = BorderStroke(1.dp, StudioBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when (exportState) {
                    is ExportState.Running -> {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(110.dp)) {
                            CircularProgressIndicator(
                                progress = { (exportState.progressPercent / 100f).coerceIn(0f, 1f) },
                                modifier = Modifier.fillMaxSize(),
                                color = StudioCyan,
                                trackColor = StudioBorder,
                                strokeWidth = 8.dp
                            )
                            Text(
                                text = "${exportState.progressPercent}%",
                                style = MonoTimecodeStyle.copy(fontSize = 20.sp, fontWeight = FontWeight.Bold),
                                color = StudioTextPrimary
                            )
                        }
                        Text(
                            text = strings.exportingTitle,
                            style = MaterialTheme.typography.titleMedium,
                            color = StudioTextPrimary,
                            textAlign = TextAlign.Center
                        )
                        OutlinedButton(
                            onClick = onCancel,
                            border = BorderStroke(1.dp, StudioCoral),
                            modifier = Modifier.testTag("cancel_export_button")
                        ) {
                            Text(strings.cancelLabel, color = StudioCoral)
                        }
                    }

                    is ExportState.Success -> {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = StudioEmerald,
                            modifier = Modifier.size(56.dp)
                        )
                        Text(
                            text = strings.exportCompleteTitle,
                            style = MaterialTheme.typography.titleLarge,
                            color = StudioTextPrimary
                        )
                        Text(
                            text = "${strings.savedToGallery}\n${exportState.record.resolutionLabel} • ${formatFileSize(exportState.record.fileSizeBytes)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = StudioTextMuted,
                            textAlign = TextAlign.Center
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = { onPlayExport(exportState.record) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("play_exported_button")
                            ) {
                                Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(strings.playExported)
                            }
                            Button(
                                onClick = { onShareExport(exportState.record) },
                                colors = ButtonDefaults.buttonColors(containerColor = StudioCyan, contentColor = Color.Black),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("share_exported_button")
                            ) {
                                Icon(Icons.Default.Share, null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(strings.shareVideo)
                            }
                        }
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(strings.saveLabel)
                        }
                    }

                    is ExportState.Error -> {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = StudioCoral,
                            modifier = Modifier.size(52.dp)
                        )
                        Text(
                            text = strings.exportFailedTitle,
                            style = MaterialTheme.typography.titleMedium,
                            color = StudioCoral
                        )
                        Text(
                            text = exportState.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = StudioTextMuted,
                            textAlign = TextAlign.Center
                        )
                        Button(onClick = onDismiss) {
                            Text(strings.cancelLabel)
                        }
                    }

                    ExportState.Idle -> Unit
                }
            }
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
fun ExportedVideoPlayerDialog(
    record: ExportRecord,
    strings: ThyCutStrings,
    onShare: (ExportRecord) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val player = remember(record.id) {
        ExoPlayer.Builder(context).build().apply {
            val file = File(record.filePath)
            val uri = if (file.exists()) Uri.fromFile(file) else Uri.parse(record.mediaStoreUri ?: "")
            setMediaItem(MediaItem.fromUri(uri))
            repeatMode = Player.REPEAT_MODE_ONE
            prepare()
            playWhenReady = true
        }
    }
    var isPlaying by remember { mutableStateOf(true) }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = StudioSurfaceElevated,
            border = BorderStroke(1.dp, StudioBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = record.projectTitle,
                        style = MaterialTheme.typography.titleMedium,
                        color = StudioTextPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = strings.cancelLabel, tint = StudioTextMuted)
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(9f / 14f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    ContentFrame(
                        player = player,
                        modifier = Modifier.fillMaxSize(),
                        surfaceType = SURFACE_TYPE_TEXTURE_VIEW,
                        contentScale = ContentScale.Fit
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { if (player.isPlaying) player.pause() else player.play() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isPlaying) "Pause" else "Play")
                    }
                    Button(
                        onClick = { onShare(record) },
                        colors = ButtonDefaults.buttonColors(containerColor = StudioCyan, contentColor = Color.Black),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Share, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(strings.shareVideo)
                    }
                }
            }
        }
    }
}
