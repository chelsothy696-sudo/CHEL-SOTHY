package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.FilterVintage
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Start
import androidx.compose.material.icons.filled.Title
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.localization.AppLanguage
import com.example.localization.ThyCutStrings
import com.example.media.BuiltInAudioGenerator
import com.example.model.AspectRatioPreset
import com.example.model.FilterPreset
import com.example.model.TextOverlayItem
import com.example.model.TextStylePreset
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
import com.example.ui.theme.StudioViolet
import com.example.viewmodel.EditorToolTab
import kotlin.math.roundToInt

@Composable
fun EditorBottomDock(
    project: VideoProject,
    selectedClip: VideoClip?,
    selectedOverlay: TextOverlayItem?,
    activeTab: EditorToolTab,
    language: AppLanguage,
    strings: ThyCutStrings,
    onSelectTab: (EditorToolTab) -> Unit,
    onSplit: () -> Unit,
    onTrimStart: () -> Unit,
    onTrimEnd: () -> Unit,
    onSpeedChange: (Float) -> Unit,
    onVolumeChange: (Float) -> Unit,
    onToggleMute: () -> Unit,
    onRotate: () -> Unit,
    onMoveLeft: () -> Unit,
    onMoveRight: () -> Unit,
    onDuplicate: () -> Unit,
    onDeleteClip: () -> Unit,
    onPickDeviceAudio: () -> Unit,
    onAttachBuiltInBeat: (String, String) -> Unit,
    onAudioVolumeChange: (Float) -> Unit,
    onToggleAudioLoop: () -> Unit,
    onRemoveAudio: () -> Unit,
    onAddText: (String, TextStylePreset, Long, Long, Boolean) -> Unit,
    onUpdateOverlay: (TextOverlayItem) -> Unit,
    onDeleteOverlay: (String) -> Unit,
    onApplyFilter: (FilterPreset, Boolean) -> Unit,
    onApplyAdjustments: (Float, Float, Float, Boolean) -> Unit,
    onSelectRatio: (AspectRatioPreset) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(StudioSurface)
            .navigationBarsPadding()
    ) {
        // Active Tool Contextual Panel
        Surface(
            color = StudioSurfaceVariant,
            shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp),
            border = BorderStroke(1.dp, StudioBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            when (activeTab) {
                EditorToolTab.EDIT -> EditToolPanel(
                    clip = selectedClip,
                    strings = strings,
                    onSplit = onSplit,
                    onTrimStart = onTrimStart,
                    onTrimEnd = onTrimEnd,
                    onSpeedChange = onSpeedChange,
                    onVolumeChange = onVolumeChange,
                    onToggleMute = onToggleMute,
                    onRotate = onRotate,
                    onMoveLeft = onMoveLeft,
                    onMoveRight = onMoveRight,
                    onDuplicate = onDuplicate,
                    onDelete = onDeleteClip
                )
                EditorToolTab.AUDIO -> AudioToolPanel(
                    project = project,
                    language = language,
                    strings = strings,
                    onPickDeviceAudio = onPickDeviceAudio,
                    onAttachBuiltInBeat = onAttachBuiltInBeat,
                    onVolumeChange = onAudioVolumeChange,
                    onToggleLoop = onToggleAudioLoop,
                    onRemoveAudio = onRemoveAudio
                )
                EditorToolTab.TEXT -> TextToolPanel(
                    project = project,
                    selectedOverlay = selectedOverlay,
                    strings = strings,
                    onAddText = onAddText,
                    onUpdateOverlay = onUpdateOverlay,
                    onDeleteOverlay = onDeleteOverlay
                )
                EditorToolTab.FILTERS -> FilterToolPanel(
                    clip = selectedClip,
                    language = language,
                    strings = strings,
                    onApplyFilter = onApplyFilter,
                    onApplyAdjustments = onApplyAdjustments
                )
                EditorToolTab.RATIO -> RatioToolPanel(
                    selectedRatio = project.aspectRatio,
                    onSelectRatio = onSelectRatio
                )
                EditorToolTab.STICKERS -> StickerToolPanel(
                    onAddSticker = { label, colorHex, bgHex ->
                        onAddText(label, TextStylePreset.CORAL_BADGE, colorHex, bgHex, true)
                    }
                )
            }
        }

        // Primary CapCut-style Tool Strip
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            ToolTabItem(
                icon = Icons.Default.ContentCut,
                label = strings.toolEdit,
                selected = activeTab == EditorToolTab.EDIT,
                accent = StudioCyan,
                tag = "tab_edit",
                onClick = { onSelectTab(EditorToolTab.EDIT) }
            )
            ToolTabItem(
                icon = Icons.Default.MusicNote,
                label = strings.toolAudio,
                selected = activeTab == EditorToolTab.AUDIO,
                accent = StudioEmerald,
                tag = "tab_audio",
                onClick = { onSelectTab(EditorToolTab.AUDIO) }
            )
            ToolTabItem(
                icon = Icons.Default.Title,
                label = strings.toolText,
                selected = activeTab == EditorToolTab.TEXT,
                accent = StudioAmber,
                tag = "tab_text",
                onClick = { onSelectTab(EditorToolTab.TEXT) }
            )
            ToolTabItem(
                icon = Icons.Default.FilterVintage,
                label = strings.toolFilters,
                selected = activeTab == EditorToolTab.FILTERS,
                accent = StudioViolet,
                tag = "tab_filters",
                onClick = { onSelectTab(EditorToolTab.FILTERS) }
            )
            ToolTabItem(
                icon = Icons.Default.AspectRatio,
                label = strings.toolRatio,
                selected = activeTab == EditorToolTab.RATIO,
                accent = StudioCyan,
                tag = "tab_ratio",
                onClick = { onSelectTab(EditorToolTab.RATIO) }
            )
            ToolTabItem(
                icon = Icons.Default.EmojiEmotions,
                label = strings.toolStickers,
                selected = activeTab == EditorToolTab.STICKERS,
                accent = StudioCoral,
                tag = "tab_stickers",
                onClick = { onSelectTab(EditorToolTab.STICKERS) }
            )
        }
    }
}

@Composable
private fun ToolTabItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    accent: Color,
    tag: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (selected) accent.copy(alpha = 0.16f) else Color.Transparent,
        modifier = Modifier
            .size(width = 70.dp, height = 54.dp)
            .testTag(tag)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (selected) accent else StudioTextMuted,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = if (selected) StudioTextPrimary else StudioTextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun EditToolPanel(
    clip: VideoClip?,
    strings: ThyCutStrings,
    onSplit: () -> Unit,
    onTrimStart: () -> Unit,
    onTrimEnd: () -> Unit,
    onSpeedChange: (Float) -> Unit,
    onVolumeChange: (Float) -> Unit,
    onToggleMute: () -> Unit,
    onRotate: () -> Unit,
    onMoveLeft: () -> Unit,
    onMoveRight: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit
) {
    var subMode by remember { mutableStateOf("actions") } // actions | speed | volume

    Column(modifier = Modifier.padding(vertical = 10.dp)) {
        if (clip == null) {
            Text(
                text = strings.selectClipPrompt,
                style = MaterialTheme.typography.bodyMedium,
                color = StudioTextMuted,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
            )
            return@Column
        }

        when (subMode) {
            "speed" -> {
                val speeds = listOf(0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f, 3.0f)
                Column(modifier = Modifier.padding(horizontal = 14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${strings.actionSpeed}: ${clip.speed}x",
                            style = MaterialTheme.typography.titleSmall,
                            color = StudioCyan
                        )
                        TextButton(onClick = { subMode = "actions" }) {
                            Text(strings.saveLabel)
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        speeds.forEach { spd ->
                            FilterChip(
                                selected = kotlin.math.abs(clip.speed - spd) < 0.05f,
                                onClick = { onSpeedChange(spd) },
                                label = {
                                    Text(
                                        text = "${spd}x",
                                        style = MonoTimecodeStyle
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = StudioCyan,
                                    selectedLabelColor = Color.Black
                                )
                            )
                        }
                    }
                }
            }
            "volume" -> {
                var vol by remember(clip.id, clip.volume) { mutableFloatStateOf(clip.volume) }
                Column(modifier = Modifier.padding(horizontal = 14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${strings.actionVolume}: ${(vol * 100).roundToInt()}%",
                            style = MaterialTheme.typography.titleSmall,
                            color = StudioCyan
                        )
                        TextButton(onClick = { subMode = "actions" }) {
                            Text(strings.saveLabel)
                        }
                    }
                    Slider(
                        value = vol,
                        onValueChange = { vol = it },
                        onValueChangeFinished = { onVolumeChange(vol) },
                        valueRange = 0f..2f,
                        colors = SliderDefaults.colors(
                            thumbColor = StudioCyan,
                            activeTrackColor = StudioCyan
                        )
                    )
                }
            }
            else -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ActionPill(
                        icon = Icons.Default.ContentCut,
                        label = strings.actionSplit,
                        tint = StudioCyan,
                        tag = "action_split",
                        onClick = onSplit
                    )
                    ActionPill(
                        icon = Icons.Default.Speed,
                        label = "${strings.actionSpeed} ${clip.speed}x",
                        tint = StudioCyan,
                        tag = "action_speed",
                        onClick = { subMode = "speed" }
                    )
                    ActionPill(
                        icon = if (clip.isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                        label = if (clip.isMuted) strings.actionUnmute else strings.actionMute,
                        tint = if (clip.isMuted) StudioCoral else StudioTextPrimary,
                        tag = "action_mute",
                        onClick = onToggleMute
                    )
                    ActionPill(
                        icon = Icons.Default.GraphicEq,
                        label = strings.actionVolume,
                        tint = StudioTextPrimary,
                        tag = "action_volume",
                        onClick = { subMode = "volume" }
                    )
                    ActionPill(
                        icon = Icons.Default.Start,
                        label = strings.actionTrimIn,
                        tint = StudioTextPrimary,
                        tag = "action_trim_in",
                        onClick = onTrimStart
                    )
                    ActionPill(
                        icon = Icons.Default.Start,
                        label = strings.actionTrimOut,
                        tint = StudioTextPrimary,
                        tag = "action_trim_out",
                        onClick = onTrimEnd
                    )
                    ActionPill(
                        icon = Icons.Default.RotateRight,
                        label = strings.actionRotate,
                        tint = StudioTextPrimary,
                        tag = "action_rotate",
                        onClick = onRotate
                    )
                    ActionPill(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        label = strings.actionMoveLeft,
                        tint = StudioTextPrimary,
                        tag = "action_move_left",
                        onClick = onMoveLeft
                    )
                    ActionPill(
                        icon = Icons.AutoMirrored.Filled.ArrowForward,
                        label = strings.actionMoveRight,
                        tint = StudioTextPrimary,
                        tag = "action_move_right",
                        onClick = onMoveRight
                    )
                    ActionPill(
                        icon = Icons.Default.ContentCopy,
                        label = strings.actionDuplicate,
                        tint = StudioTextPrimary,
                        tag = "action_duplicate",
                        onClick = onDuplicate
                    )
                    ActionPill(
                        icon = Icons.Default.Delete,
                        label = strings.actionDelete,
                        tint = StudioCoral,
                        tag = "action_delete_clip",
                        onClick = onDelete
                    )
                }
            }
        }
    }
}

@Composable
private fun ActionPill(
    icon: ImageVector,
    label: String,
    tint: Color,
    tag: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = StudioSurfaceElevated,
        border = BorderStroke(1.dp, StudioBorder),
        modifier = Modifier
            .height(54.dp)
            .testTag(tag)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = StudioTextPrimary,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun AudioToolPanel(
    project: VideoProject,
    language: AppLanguage,
    strings: ThyCutStrings,
    onPickDeviceAudio: () -> Unit,
    onAttachBuiltInBeat: (String, String) -> Unit,
    onVolumeChange: (Float) -> Unit,
    onToggleLoop: () -> Unit,
    onRemoveAudio: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onPickDeviceAudio,
                colors = ButtonDefaults.buttonColors(containerColor = StudioEmerald, contentColor = Color.Black),
                modifier = Modifier
                    .weight(1f)
                    .testTag("pick_device_audio_button")
            ) {
                Icon(Icons.Default.LibraryMusic, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(strings.addMusicDevice, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (project.audioTrack != null) {
                OutlinedButton(
                    onClick = onRemoveAudio,
                    border = BorderStroke(1.dp, StudioCoral),
                    modifier = Modifier.testTag("remove_audio_button")
                ) {
                    Icon(Icons.Default.Delete, contentDescription = strings.removeAudio, tint = StudioCoral, modifier = Modifier.size(18.dp))
                }
            }
        }

        // Built-in studio beats
        Text(
            text = strings.builtInBeats,
            style = MaterialTheme.typography.labelMedium,
            color = StudioTextMuted
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            BuiltInAudioGenerator.catalog.forEach { beat ->
                val title = if (language == AppLanguage.KHMER) beat.titleKm else beat.titleEn
                val isActive = project.audioTrack?.title == title
                FilterChip(
                    selected = isActive,
                    onClick = { onAttachBuiltInBeat(beat.id, title) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    label = { Text("$title • ${beat.bpm} BPM") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = StudioEmerald.copy(alpha = 0.25f),
                        selectedLabelColor = StudioEmerald,
                        selectedLeadingIconColor = StudioEmerald
                    )
                )
            }
        }

        project.audioTrack?.let { audio ->
            var musicVol by remember(audio.id, audio.volume) { mutableFloatStateOf(audio.volume) }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${strings.musicVolumeLabel}: ${(musicVol * 100).roundToInt()}%",
                    style = MaterialTheme.typography.labelMedium,
                    color = StudioEmerald
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(strings.loopMusic, style = MaterialTheme.typography.labelSmall, color = StudioTextMuted)
                    Spacer(modifier = Modifier.width(6.dp))
                    Switch(checked = audio.loop, onCheckedChange = { onToggleLoop() })
                }
            }
            Slider(
                value = musicVol,
                onValueChange = { musicVol = it },
                onValueChangeFinished = { onVolumeChange(musicVol) },
                valueRange = 0f..1.5f,
                colors = SliderDefaults.colors(thumbColor = StudioEmerald, activeTrackColor = StudioEmerald)
            )
        }
    }
}

@Composable
private fun TextToolPanel(
    project: VideoProject,
    selectedOverlay: TextOverlayItem?,
    strings: ThyCutStrings,
    onAddText: (String, TextStylePreset, Long, Long, Boolean) -> Unit,
    onUpdateOverlay: (TextOverlayItem) -> Unit,
    onDeleteOverlay: (String) -> Unit
) {
    var draftText by remember(selectedOverlay?.id) {
        mutableStateOf(selectedOverlay?.text ?: "")
    }
    var chosenStyle by remember(selectedOverlay?.id) {
        mutableStateOf(selectedOverlay?.stylePreset ?: TextStylePreset.SUBTITLE_PILL)
    }
    var chosenColor by remember(selectedOverlay?.id) {
        mutableStateOf(selectedOverlay?.colorHex ?: 0xFFFFFFFFL)
    }

    val colorSwatches = listOf(0xFFFFFFFFL, 0xFF00E5FFL, 0xFFFF3366L, 0xFFF59E0BL, 0xFF10B981L)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = draftText,
                onValueChange = { draftText = it },
                placeholder = { Text(strings.textPlaceholder) },
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .testTag("text_overlay_input")
            )
            Button(
                onClick = {
                    val bgHex = when (chosenStyle) {
                        TextStylePreset.CLEAN -> 0x00000000L
                        TextStylePreset.CORAL_BADGE -> 0xFFFF3366L
                        TextStylePreset.NEON_CYAN -> 0xCC0A0B10L
                        TextStylePreset.SUBTITLE_PILL -> 0xB30A0B10L
                        TextStylePreset.CINEMA_BOLD -> 0x66000000L
                    }
                    if (selectedOverlay != null) {
                        onUpdateOverlay(
                            selectedOverlay.copy(
                                text = draftText.ifBlank { selectedOverlay.text },
                                stylePreset = chosenStyle,
                                colorHex = chosenColor,
                                bgColorHex = bgHex
                            )
                        )
                    } else if (draftText.isNotBlank()) {
                        onAddText(draftText, chosenStyle, chosenColor, bgHex, false)
                        draftText = ""
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = StudioAmber, contentColor = Color.Black),
                modifier = Modifier.testTag("save_text_overlay_button")
            ) {
                Text(if (selectedOverlay != null) strings.saveLabel else strings.addTextLabel)
            }
            if (selectedOverlay != null) {
                IconButton(
                    onClick = { onDeleteOverlay(selectedOverlay.id) },
                    modifier = Modifier.testTag("delete_text_overlay_button")
                ) {
                    Icon(Icons.Default.Delete, contentDescription = strings.actionDelete, tint = StudioCoral)
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            colorSwatches.forEach { hex ->
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color(hex))
                        .border(
                            width = if (chosenColor == hex) 2.5.dp else 1.dp,
                            color = if (chosenColor == hex) StudioCyan else StudioBorder,
                            shape = CircleShape
                        )
                        .clickable {
                            chosenColor = hex
                            selectedOverlay?.let { onUpdateOverlay(it.copy(colorHex = hex)) }
                        }
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            TextStylePreset.entries.forEach { style ->
                FilterChip(
                    selected = chosenStyle == style,
                    onClick = {
                        chosenStyle = style
                        selectedOverlay?.let {
                            val bgHex = when (style) {
                                TextStylePreset.CLEAN -> 0x00000000L
                                TextStylePreset.CORAL_BADGE -> 0xFFFF3366L
                                else -> 0xB30A0B10L
                            }
                            onUpdateOverlay(it.copy(stylePreset = style, bgColorHex = bgHex))
                        }
                    },
                    label = { Text(style.label) }
                )
            }
        }

        if (selectedOverlay != null) {
            var sizeVal by remember(selectedOverlay.id) { mutableFloatStateOf(selectedOverlay.fontSizeSp) }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Size ${sizeVal.roundToInt()}sp", style = MonoTimecodeStyle, color = StudioAmber)
                Slider(
                    value = sizeVal,
                    onValueChange = { sizeVal = it },
                    onValueChangeFinished = {
                        onUpdateOverlay(selectedOverlay.copy(fontSizeSp = sizeVal))
                    },
                    valueRange = 14f..42f,
                    modifier = Modifier.weight(1f),
                    colors = SliderDefaults.colors(thumbColor = StudioAmber, activeTrackColor = StudioAmber)
                )
            }
        } else if (project.textOverlays.isNotEmpty()) {
            Text(
                text = strings.tapToEditText,
                style = MaterialTheme.typography.labelSmall,
                color = StudioTextMuted
            )
        }
    }
}

@Composable
private fun FilterToolPanel(
    clip: VideoClip?,
    language: AppLanguage,
    strings: ThyCutStrings,
    onApplyFilter: (FilterPreset, Boolean) -> Unit,
    onApplyAdjustments: (Float, Float, Float, Boolean) -> Unit
) {
    var applyAll by remember { mutableStateOf(false) }
    var showAdjust by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = !showAdjust,
                    onClick = { showAdjust = false },
                    leadingIcon = { Icon(Icons.Default.FilterVintage, null, modifier = Modifier.size(16.dp)) },
                    label = { Text(strings.toolFilters) }
                )
                FilterChip(
                    selected = showAdjust,
                    onClick = { showAdjust = true },
                    leadingIcon = { Icon(Icons.Default.Tune, null, modifier = Modifier.size(16.dp)) },
                    label = { Text(strings.contrastLabel) }
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(strings.applyAllLabel, style = MaterialTheme.typography.labelSmall, color = StudioTextMuted)
                Spacer(modifier = Modifier.width(6.dp))
                Switch(checked = applyAll, onCheckedChange = { applyAll = it })
            }
        }

        if (!showAdjust) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FilterPreset.entries.forEach { preset ->
                    val selected = clip?.filterPreset == preset
                    val name = if (language == AppLanguage.KHMER) preset.titleKm else preset.titleEn
                    Surface(
                        onClick = { onApplyFilter(preset, applyAll) },
                        shape = RoundedCornerShape(12.dp),
                        color = if (selected) StudioViolet.copy(alpha = 0.25f) else StudioSurfaceElevated,
                        border = BorderStroke(
                            width = if (selected) 2.dp else 1.dp,
                            color = if (selected) StudioViolet else StudioBorder
                        ),
                        modifier = Modifier
                            .size(width = 78.dp, height = 68.dp)
                            .testTag("filter_${preset.name}")
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(CircleShape)
                                    .background(Color(preset.previewColorHex))
                            )
                            Spacer(modifier = Modifier.height(5.dp))
                            Text(
                                text = name,
                                style = MaterialTheme.typography.labelSmall,
                                color = StudioTextPrimary,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        } else if (clip != null) {
            var bright by remember(clip.id, clip.brightness) { mutableFloatStateOf(clip.brightness) }
            var cont by remember(clip.id, clip.contrast) { mutableFloatStateOf(clip.contrast) }
            var sat by remember(clip.id, clip.saturation) { mutableFloatStateOf(clip.saturation) }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                AdjustmentSliderRow(
                    label = strings.brightnessLabel,
                    value = bright,
                    range = -0.4f..0.4f,
                    onValueChange = { bright = it },
                    onFinished = { onApplyAdjustments(bright, cont, sat, applyAll) }
                )
                AdjustmentSliderRow(
                    label = strings.contrastLabel,
                    value = cont,
                    range = -0.4f..0.4f,
                    onValueChange = { cont = it },
                    onFinished = { onApplyAdjustments(bright, cont, sat, applyAll) }
                )
                AdjustmentSliderRow(
                    label = strings.saturationLabel,
                    value = sat,
                    range = -0.8f..0.8f,
                    onValueChange = { sat = it },
                    onFinished = { onApplyAdjustments(bright, cont, sat, applyAll) }
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = {
                        bright = 0f; cont = 0f; sat = 0f
                        onApplyAdjustments(0f, 0f, 0f, applyAll)
                    }) {
                        Icon(Icons.Default.Refresh, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(strings.resetAdjustments)
                    }
                }
            }
        }
    }
}

@Composable
private fun AdjustmentSliderRow(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    onFinished: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = StudioTextMuted,
            modifier = Modifier.width(90.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Slider(
            value = value,
            onValueChange = onValueChange,
            onValueChangeFinished = onFinished,
            valueRange = range,
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(thumbColor = StudioViolet, activeTrackColor = StudioViolet)
        )
    }
}

@Composable
private fun RatioToolPanel(
    selectedRatio: AspectRatioPreset,
    onSelectRatio: (AspectRatioPreset) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        AspectRatioPreset.entries.forEach { preset ->
            val selected = preset == selectedRatio
            Surface(
                onClick = { onSelectRatio(preset) },
                shape = RoundedCornerShape(12.dp),
                color = if (selected) StudioCyan.copy(alpha = 0.2f) else StudioSurfaceElevated,
                border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) StudioCyan else StudioBorder),
                modifier = Modifier
                    .size(width = 76.dp, height = 66.dp)
                    .testTag("ratio_${preset.label}")
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    val boxW = (18f * (preset.widthRatio / maxOf(preset.widthRatio, preset.heightRatio))).dp
                    val boxH = (18f * (preset.heightRatio / maxOf(preset.widthRatio, preset.heightRatio))).dp
                    Box(
                        modifier = Modifier
                            .size(width = boxW.coerceAtLeast(10.dp), height = boxH.coerceAtLeast(10.dp))
                            .border(1.5.dp, if (selected) StudioCyan else StudioTextMuted, RoundedCornerShape(3.dp))
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = preset.label,
                        style = MonoTimecodeStyle.copy(fontWeight = FontWeight.Bold),
                        color = if (selected) StudioCyan else StudioTextPrimary
                    )
                }
            }
        }
    }
}

@Composable
private fun StickerToolPanel(
    onAddSticker: (String, Long, Long) -> Unit
) {
    val badges = listOf(
        Triple("● REC", 0xFFFFFFFFL, 0xFFFF3366L),
        Triple("4K ULTRA", 0xFF0A0B10L, 0xFF00E5FFL),
        Triple("LIVE", 0xFFFFFFFFL, 0xFFA855F7L),
        Triple("NEW REEL", 0xFF0A0B10L, 0xFFF59E0BL),
        Triple("THYCUT", 0xFF00E5FFL, 0xCC0A0B10L),
        Triple("100%", 0xFF0A0B10L, 0xFF10B981L)
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        badges.forEach { (label, fg, bg) ->
            Surface(
                onClick = { onAddSticker(label, fg, bg) },
                shape = RoundedCornerShape(20.dp),
                color = Color(bg),
                border = BorderStroke(1.dp, StudioBorder),
                modifier = Modifier.testTag("sticker_$label")
            ) {
                Text(
                    text = label,
                    style = MonoTimecodeStyle.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold),
                    color = Color(fg),
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                )
            }
        }
    }
}
