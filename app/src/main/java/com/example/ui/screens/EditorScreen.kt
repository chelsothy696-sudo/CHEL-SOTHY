package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.compose.ContentFrame
import androidx.media3.ui.compose.SURFACE_TYPE_TEXTURE_VIEW
import com.example.localization.AppLanguage
import com.example.localization.stringsFor
import com.example.model.ExportRecord
import com.example.model.TextOverlayItem
import com.example.model.TextStylePreset
import com.example.ui.components.EditorBottomDock
import com.example.ui.components.ExportProgressModal
import com.example.ui.components.ExportSettingsDialog
import com.example.ui.components.ExportedVideoPlayerDialog
import com.example.ui.components.TimelineSection
import com.example.ui.components.formatTimecode
import com.example.ui.components.shareExportedVideo
import com.example.ui.theme.MonoTimecodeStyle
import com.example.ui.theme.StudioAmber
import com.example.ui.theme.StudioBg
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioCyan
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.StudioTextMuted
import com.example.ui.theme.StudioTextPrimary
import com.example.viewmodel.EditorToolTab
import com.example.viewmodel.EditorViewModel
import kotlin.math.roundToInt

@OptIn(UnstableApi::class)
@Composable
fun EditorScreen(
    projectId: String,
    initialTool: EditorToolTab?,
    language: AppLanguage,
    onNavigateBack: () -> Unit,
    editorViewModel: EditorViewModel = viewModel()
) {
    val context = LocalContext.current
    val strings = remember(language) { stringsFor(language) }

    val project by editorViewModel.project.collectAsStateWithLifecycle()
    val player by editorViewModel.playerState.collectAsStateWithLifecycle()
    val playheadMs by editorViewModel.playheadMs.collectAsStateWithLifecycle()
    val isPlaying by editorViewModel.isPlaying.collectAsStateWithLifecycle()
    val selectedClipId by editorViewModel.selectedClipId.collectAsStateWithLifecycle()
    val selectedOverlayId by editorViewModel.selectedOverlayId.collectAsStateWithLifecycle()
    val activeTab by editorViewModel.activeToolTab.collectAsStateWithLifecycle()
    val timelineZoom by editorViewModel.timelineZoom.collectAsStateWithLifecycle()
    val canUndo by editorViewModel.canUndo.collectAsStateWithLifecycle()
    val canRedo by editorViewModel.canRedo.collectAsStateWithLifecycle()
    val exportState by editorViewModel.exportState.collectAsStateWithLifecycle()
    val statusMessage by editorViewModel.statusMessage.collectAsStateWithLifecycle()

    var showExportSettings by remember { mutableStateOf(false) }
    var playingExportRecord by remember { mutableStateOf<ExportRecord?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(projectId, initialTool) {
        editorViewModel.loadProject(projectId, initialTool)
    }

    LaunchedEffect(statusMessage) {
        statusMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            editorViewModel.clearStatusMessage()
        }
    }

    BackHandler {
        editorViewModel.pausePlayback()
        onNavigateBack()
    }

    val addMediaLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 10),
        onResult = { uris ->
            if (uris.isNotEmpty()) {
                editorViewModel.addClips(uris)
            }
        }
    )

    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            uri?.let { editorViewModel.attachDeviceAudio(it) }
        }
    )

    val currentProject = project
    if (currentProject == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(StudioBg),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = StudioCyan)
        }
        return
    }

    val selectedClip = currentProject.clips.find { it.id == selectedClipId }
        ?: currentProject.clips.firstOrNull()
    val selectedOverlay = currentProject.textOverlays.find { it.id == selectedOverlayId }

    Scaffold(
        containerColor = StudioBg,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(StudioSurface)
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            editorViewModel.pausePlayback()
                            onNavigateBack()
                        },
                        modifier = Modifier.testTag("editor_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = StudioTextPrimary
                        )
                    }
                    IconButton(
                        onClick = { editorViewModel.undo() },
                        enabled = canUndo,
                        modifier = Modifier.testTag("editor_undo_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Undo,
                            contentDescription = strings.undo,
                            tint = if (canUndo) StudioTextPrimary else StudioTextMuted.copy(alpha = 0.4f)
                        )
                    }
                    IconButton(
                        onClick = { editorViewModel.redo() },
                        enabled = canRedo,
                        modifier = Modifier.testTag("editor_redo_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Redo,
                            contentDescription = strings.redo,
                            tint = if (canRedo) StudioTextPrimary else StudioTextMuted.copy(alpha = 0.4f)
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        onClick = { showExportSettings = true },
                        shape = RoundedCornerShape(18.dp),
                        color = StudioSurfaceElevated,
                        border = BorderStroke(1.dp, StudioBorder),
                        modifier = Modifier.testTag("editor_resolution_pill")
                    ) {
                        Text(
                            text = if (currentProject.exportResolution == com.example.model.ExportResolution.RES_1080P) {
                                "1080p"
                            } else {
                                "720p"
                            },
                            style = MonoTimecodeStyle,
                            color = StudioTextPrimary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }

                    Button(
                        onClick = { showExportSettings = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = StudioCyan,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .height(38.dp)
                            .testTag("editor_export_button")
                    ) {
                        Text(
                            text = strings.exportBtn,
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(StudioBg)
        ) {
            // Video Preview Stage
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                BoxWithConstraints(
                    modifier = Modifier
                        .aspectRatio(currentProject.aspectRatio.ratioFloat)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.Black)
                        .border(1.dp, StudioBorder, RoundedCornerShape(14.dp))
                ) {
                    val canvasW = maxWidth
                    val canvasH = maxHeight
                    val density = LocalDensity.current
                    val canvasWPx = with(density) { canvasW.toPx() }
                    val canvasHPx = with(density) { canvasH.toPx() }

                    ContentFrame(
                        player = player,
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectTapGestures(onTap = {
                                    editorViewModel.selectOverlay(null)
                                    editorViewModel.togglePlayPause()
                                })
                            },
                        surfaceType = SURFACE_TYPE_TEXTURE_VIEW,
                        contentScale = ContentScale.Fit
                    )

                    // Interactive draggable text & sticker overlays visible at current playhead
                    currentProject.textOverlays
                        .filter { playheadMs in it.startMs..it.endMs }
                        .forEach { overlay ->
                            DraggableOverlayNode(
                                overlay = overlay,
                                isSelected = overlay.id == selectedOverlayId,
                                canvasWPx = canvasWPx,
                                canvasHPx = canvasHPx,
                                onTap = {
                                    editorViewModel.selectOverlay(overlay.id)
                                    editorViewModel.selectToolTab(EditorToolTab.TEXT)
                                },
                                onDragDelta = { dx, dy ->
                                    editorViewModel.moveTextOverlayLive(
                                        overlay.id,
                                        dx / canvasWPx,
                                        dy / canvasHPx
                                    )
                                },
                                onDragEnd = {
                                    editorViewModel.commitLiveOverlaysToDb()
                                }
                            )
                        }
                }
            }

            // Transport Controls Strip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(StudioSurface)
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${formatTimecode(playheadMs)} / ${formatTimecode(currentProject.totalDurationMs)}",
                    style = MonoTimecodeStyle,
                    color = StudioTextPrimary,
                    modifier = Modifier.testTag("editor_timecode")
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    IconButton(
                        onClick = { editorViewModel.stepBy(-1000L) },
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("transport_rewind")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FastRewind,
                            contentDescription = "Back 1s",
                            tint = StudioTextPrimary
                        )
                    }

                    Surface(
                        onClick = { editorViewModel.togglePlayPause() },
                        shape = CircleShape,
                        color = StudioCyan,
                        modifier = Modifier
                            .size(44.dp)
                            .testTag("transport_play_pause")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = Color.Black,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = { editorViewModel.stepBy(1000L) },
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("transport_forward")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FastForward,
                            contentDescription = "Forward 1s",
                            tint = StudioTextPrimary
                        )
                    }
                }

                Text(
                    text = currentProject.aspectRatio.label,
                    style = MonoTimecodeStyle,
                    color = StudioCyan
                )
            }

            // Multi-Track Interactive Timeline
            TimelineSection(
                project = currentProject,
                strings = strings,
                playheadMs = playheadMs,
                isPlaying = isPlaying,
                selectedClipId = selectedClip?.id,
                selectedOverlayId = selectedOverlayId,
                timelineZoom = timelineZoom,
                onSeek = { editorViewModel.seekTo(it) },
                onSelectClip = { editorViewModel.selectClip(it) },
                onSelectOverlay = {
                    editorViewModel.selectOverlay(it)
                    editorViewModel.selectToolTab(EditorToolTab.TEXT)
                },
                onTrimClip = { clip, newStart, newEnd ->
                    editorViewModel.selectClip(clip.id)
                    editorViewModel.updateSelectedClipTrim(newStart, newEnd)
                },
                onAddClipClick = {
                    addMediaLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                    )
                },
                onZoomChange = { editorViewModel.setTimelineZoom(it) }
            )

            // Contextual Bottom Tool Dock
            EditorBottomDock(
                project = currentProject,
                selectedClip = selectedClip,
                selectedOverlay = selectedOverlay,
                activeTab = activeTab,
                language = language,
                strings = strings,
                onSelectTab = { editorViewModel.selectToolTab(it) },
                onSplit = { editorViewModel.splitSelectedClipAtPlayhead(strings.splitTooShort) },
                onTrimStart = { editorViewModel.trimSelectedClipToPlayhead(true, strings.splitTooShort) },
                onTrimEnd = { editorViewModel.trimSelectedClipToPlayhead(false, strings.splitTooShort) },
                onSpeedChange = { editorViewModel.setSelectedClipSpeed(it) },
                onVolumeChange = { editorViewModel.setSelectedClipVolume(it) },
                onToggleMute = { editorViewModel.toggleMuteSelectedClip() },
                onRotate = { editorViewModel.rotateSelectedClip() },
                onMoveLeft = { editorViewModel.moveSelectedClip(-1) },
                onMoveRight = { editorViewModel.moveSelectedClip(1) },
                onDuplicate = { editorViewModel.duplicateSelectedClip() },
                onDeleteClip = { editorViewModel.deleteSelectedClip(strings.cannotDeleteLastClip) },
                onPickDeviceAudio = { audioPickerLauncher.launch(arrayOf("audio/*")) },
                onAttachBuiltInBeat = { beatId, title -> editorViewModel.attachBuiltInBeat(beatId, title) },
                onAudioVolumeChange = { editorViewModel.setAudioVolume(it) },
                onToggleAudioLoop = { editorViewModel.toggleAudioLoop() },
                onRemoveAudio = { editorViewModel.removeAudioTrack() },
                onAddText = { txt, style, fg, bg, sticker ->
                    editorViewModel.addTextOverlay(txt, style, fg, bg, sticker)
                },
                onUpdateOverlay = { editorViewModel.updateTextOverlay(it) },
                onDeleteOverlay = { editorViewModel.deleteTextOverlay(it) },
                onApplyFilter = { preset, applyAll -> editorViewModel.applyFilter(preset, applyAll) },
                onApplyAdjustments = { b, c, s, applyAll ->
                    editorViewModel.applyAdjustments(b, c, s, applyAll)
                },
                onSelectRatio = { editorViewModel.setAspectRatio(it) }
            )
        }
    }

    if (showExportSettings) {
        ExportSettingsDialog(
            project = currentProject,
            strings = strings,
            onSelectResolution = { editorViewModel.setExportResolution(it) },
            onSelectFps = { editorViewModel.setExportFps(it) },
            onStartExport = {
                showExportSettings = false
                editorViewModel.startExport()
            },
            onDismiss = { showExportSettings = false }
        )
    }

    ExportProgressModal(
        exportState = exportState,
        strings = strings,
        onCancel = { editorViewModel.cancelExport() },
        onDismiss = { editorViewModel.resetExportState() },
        onPlayExport = { record ->
            editorViewModel.resetExportState()
            playingExportRecord = record
        },
        onShareExport = { record -> shareExportedVideo(context, record) }
    )

    playingExportRecord?.let { record ->
        ExportedVideoPlayerDialog(
            record = record,
            strings = strings,
            onShare = { shareExportedVideo(context, it) },
            onDismiss = { playingExportRecord = null }
        )
    }
}

@Composable
private fun DraggableOverlayNode(
    overlay: TextOverlayItem,
    isSelected: Boolean,
    canvasWPx: Float,
    canvasHPx: Float,
    onTap: () -> Unit,
    onDragDelta: (Float, Float) -> Unit,
    onDragEnd: () -> Unit
) {
    val bgAlpha = (overlay.bgColorHex ushr 24) and 0xFF
    val showBg = bgAlpha > 0 && overlay.stylePreset != TextStylePreset.CLEAN

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Surface(
            shape = RoundedCornerShape(if (overlay.isStickerBadge) 20.dp else 10.dp),
            color = if (showBg) Color(overlay.bgColorHex) else Color.Transparent,
            border = when {
                isSelected -> BorderStroke(1.5.dp, StudioAmber)
                overlay.stylePreset == TextStylePreset.NEON_CYAN -> BorderStroke(1.5.dp, StudioCyan)
                else -> null
            },
            modifier = Modifier
                .offset {
                    IntOffset(
                        x = ((overlay.xFraction * canvasWPx) - 80f).roundToInt(),
                        y = ((overlay.yFraction * canvasHPx) - 28f).roundToInt()
                    )
                }
                .pointerInput(overlay.id) {
                    detectTapGestures(onTap = { onTap() })
                }
                .pointerInput(overlay.id) {
                    detectDragGestures(
                        onDrag = { change, dragAmount ->
                            change.consume()
                            onDragDelta(dragAmount.x, dragAmount.y)
                        },
                        onDragEnd = onDragEnd
                    )
                }
        ) {
            Text(
                text = overlay.text,
                color = Color(overlay.colorHex),
                fontSize = (overlay.fontSizeSp * 0.72f).sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
            )
        }
    }
}
