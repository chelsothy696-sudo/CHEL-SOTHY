package com.example.ui.screens

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterVintage
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MovieCreation
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Title
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.localization.AppLanguage
import com.example.localization.ThyCutStrings
import com.example.localization.stringsFor
import com.example.media.ThumbnailExtractor
import com.example.model.ExportRecord
import com.example.model.StudioTemplate
import com.example.model.StudioTemplates
import com.example.model.VideoProject
import com.example.ui.components.ExportedVideoPlayerDialog
import com.example.ui.components.formatFileSize
import com.example.ui.components.formatShortDuration
import com.example.ui.components.shareExportedVideo
import com.example.ui.theme.MonoTimecodeStyle
import com.example.ui.theme.StudioAmber
import com.example.ui.theme.StudioBg
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
import java.text.DateFormat
import java.util.Date

private enum class HomeTab {
    STUDIO,
    TEMPLATES,
    EXPORTS
}

@Composable
fun HomeScreen(
    language: AppLanguage,
    projects: List<VideoProject>,
    exports: List<ExportRecord>,
    isCreatingProject: Boolean,
    onToggleLanguage: () -> Unit,
    onCreateProject: (List<Uri>, StudioTemplate?, EditorToolTab?) -> Unit,
    onOpenProject: (String) -> Unit,
    onRenameProject: (VideoProject, String) -> Unit,
    onDuplicateProject: (VideoProject) -> Unit,
    onDeleteProject: (String) -> Unit,
    onDeleteExport: (ExportRecord) -> Unit
) {
    val context = LocalContext.current
    val strings = remember(language) { stringsFor(language) }
    var currentTab by remember { mutableStateOf(HomeTab.STUDIO) }

    var pendingTemplate by remember { mutableStateOf<StudioTemplate?>(null) }
    var pendingInitialTool by remember { mutableStateOf<EditorToolTab?>(null) }
    var renamingProject by remember { mutableStateOf<VideoProject?>(null) }
    var playingExport by remember { mutableStateOf<ExportRecord?>(null) }

    val mediaPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 12),
        onResult = { uris ->
            val tpl = pendingTemplate
            val tool = pendingInitialTool
            pendingTemplate = null
            pendingInitialTool = null
            if (uris.isNotEmpty()) {
                onCreateProject(uris, tpl, tool)
            }
        }
    )

    fun launchPicker(template: StudioTemplate? = null, tool: EditorToolTab? = null) {
        pendingTemplate = template
        pendingInitialTool = tool
        mediaPickerLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
        )
    }

    Scaffold(
        containerColor = StudioBg,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(StudioBg)
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_thycut_emblem),
                        contentDescription = "ThyCut Logo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                    )
                    Column {
                        Text(
                            text = strings.appName,
                            style = MaterialTheme.typography.titleLarge,
                            color = StudioTextPrimary
                        )
                        Text(
                            text = strings.studioTagline,
                            style = MaterialTheme.typography.labelSmall,
                            color = StudioTextMuted
                        )
                    }
                }

                Surface(
                    onClick = onToggleLanguage,
                    shape = RoundedCornerShape(20.dp),
                    color = StudioSurfaceVariant,
                    border = BorderStroke(1.dp, StudioBorder),
                    modifier = Modifier.testTag("language_toggle_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = "Switch Language",
                            tint = StudioCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = language.badgeLabel,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = StudioTextPrimary
                        )
                    }
                }
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = StudioSurface,
                windowInsets = WindowInsets.navigationBars
            ) {
                NavigationBarItem(
                    selected = currentTab == HomeTab.STUDIO,
                    onClick = { currentTab = HomeTab.STUDIO },
                    icon = { Icon(Icons.Default.MovieCreation, contentDescription = strings.homeTab) },
                    label = { Text(strings.homeTab) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = StudioCyan,
                        selectedTextColor = StudioCyan,
                        indicatorColor = StudioCyan.copy(alpha = 0.16f),
                        unselectedIconColor = StudioTextMuted,
                        unselectedTextColor = StudioTextMuted
                    ),
                    modifier = Modifier.testTag("nav_studio_tab")
                )
                NavigationBarItem(
                    selected = currentTab == HomeTab.TEMPLATES,
                    onClick = { currentTab = HomeTab.TEMPLATES },
                    icon = { Icon(Icons.Default.AutoAwesome, contentDescription = strings.templatesTab) },
                    label = { Text(strings.templatesTab) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = StudioCyan,
                        selectedTextColor = StudioCyan,
                        indicatorColor = StudioCyan.copy(alpha = 0.16f),
                        unselectedIconColor = StudioTextMuted,
                        unselectedTextColor = StudioTextMuted
                    ),
                    modifier = Modifier.testTag("nav_templates_tab")
                )
                NavigationBarItem(
                    selected = currentTab == HomeTab.EXPORTS,
                    onClick = { currentTab = HomeTab.EXPORTS },
                    icon = { Icon(Icons.Default.VideoLibrary, contentDescription = strings.exportsTab) },
                    label = { Text(strings.exportsTab) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = StudioCyan,
                        selectedTextColor = StudioCyan,
                        indicatorColor = StudioCyan.copy(alpha = 0.16f),
                        unselectedIconColor = StudioTextMuted,
                        unselectedTextColor = StudioTextMuted
                    ),
                    modifier = Modifier.testTag("nav_exports_tab")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            when (currentTab) {
                HomeTab.STUDIO -> StudioHomeContent(
                    strings = strings,
                    language = language,
                    projects = projects,
                    onNewProjectClick = { launchPicker() },
                    onQuickToolClick = { tool -> launchPicker(tool = tool) },
                    onTemplateClick = { tpl -> launchPicker(template = tpl) },
                    onOpenProject = onOpenProject,
                    onRenameProject = { renamingProject = it },
                    onDuplicateProject = onDuplicateProject,
                    onDeleteProject = onDeleteProject,
                    modifier = Modifier.widthIn(max = 680.dp)
                )

                HomeTab.TEMPLATES -> TemplatesContent(
                    strings = strings,
                    language = language,
                    onUseTemplate = { tpl -> launchPicker(template = tpl) },
                    modifier = Modifier.widthIn(max = 680.dp)
                )

                HomeTab.EXPORTS -> ExportsContent(
                    strings = strings,
                    exports = exports,
                    onPlay = { playingExport = it },
                    onShare = { shareExportedVideo(context, it) },
                    onDelete = onDeleteExport,
                    modifier = Modifier.widthIn(max = 680.dp)
                )
            }

            if (isCreatingProject) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.65f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = StudioCyan)
                }
            }
        }
    }

    renamingProject?.let { target ->
        var text by remember(target.id) { mutableStateOf(target.title) }
        AlertDialog(
            onDismissRequest = { renamingProject = null },
            containerColor = StudioSurfaceElevated,
            title = { Text(strings.renameProject, color = StudioTextPrimary) },
            text = {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onRenameProject(target, text)
                        renamingProject = null
                    }
                ) {
                    Text(strings.saveLabel)
                }
            },
            dismissButton = {
                TextButton(onClick = { renamingProject = null }) {
                    Text(strings.cancelLabel)
                }
            }
        )
    }

    playingExport?.let { record ->
        ExportedVideoPlayerDialog(
            record = record,
            strings = strings,
            onShare = { shareExportedVideo(context, it) },
            onDismiss = { playingExport = null }
        )
    }
}

@Composable
private fun StudioHomeContent(
    strings: ThyCutStrings,
    language: AppLanguage,
    projects: List<VideoProject>,
    onNewProjectClick: () -> Unit,
    onQuickToolClick: (EditorToolTab) -> Unit,
    onTemplateClick: (StudioTemplate) -> Unit,
    onOpenProject: (String) -> Unit,
    onRenameProject: (VideoProject) -> Unit,
    onDuplicateProject: (VideoProject) -> Unit,
    onDeleteProject: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Hero New Project Card
        item {
            Surface(
                onClick = onNewProjectClick,
                shape = RoundedCornerShape(22.dp),
                border = BorderStroke(
                    1.5.dp,
                    Brush.horizontalGradient(listOf(StudioCyan, StudioCoral))
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(172.dp)
                    .testTag("new_project_hero_card")
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Image(
                        painter = painterResource(id = R.drawable.img_studio_banner),
                        contentDescription = strings.newProject,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color(0x660A0B10),
                                        Color(0xE60A0B10)
                                    )
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = StudioCyan,
                            modifier = Modifier.size(46.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = strings.newProject,
                                    tint = Color.Black,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }

                        Column {
                            Text(
                                text = strings.newProject,
                                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                                color = StudioTextPrimary
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = strings.newProjectSubtitle,
                                style = MaterialTheme.typography.bodyMedium,
                                color = StudioTextMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // Quick Tools Strip
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = strings.quickTools,
                    style = MaterialTheme.typography.titleMedium,
                    color = StudioTextPrimary
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickToolButton(
                        icon = Icons.Default.ContentCut,
                        label = strings.toolEdit,
                        color = StudioCyan,
                        tag = "quick_tool_edit",
                        onClick = { onQuickToolClick(EditorToolTab.EDIT) }
                    )
                    QuickToolButton(
                        icon = Icons.Default.Speed,
                        label = strings.actionSpeed,
                        color = StudioCoral,
                        tag = "quick_tool_speed",
                        onClick = { onQuickToolClick(EditorToolTab.EDIT) }
                    )
                    QuickToolButton(
                        icon = Icons.Default.FilterVintage,
                        label = strings.toolFilters,
                        color = StudioViolet,
                        tag = "quick_tool_filters",
                        onClick = { onQuickToolClick(EditorToolTab.FILTERS) }
                    )
                    QuickToolButton(
                        icon = Icons.Default.Title,
                        label = strings.toolText,
                        color = StudioAmber,
                        tag = "quick_tool_text",
                        onClick = { onQuickToolClick(EditorToolTab.TEXT) }
                    )
                    QuickToolButton(
                        icon = Icons.Default.MusicNote,
                        label = strings.toolAudio,
                        color = StudioEmerald,
                        tag = "quick_tool_audio",
                        onClick = { onQuickToolClick(EditorToolTab.AUDIO) }
                    )
                }
            }
        }

        // Trending Templates Carousel
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = strings.templatesTitle,
                    style = MaterialTheme.typography.titleMedium,
                    color = StudioTextPrimary
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(StudioTemplates.all, key = { it.id }) { tpl ->
                        TemplateMiniCard(
                            template = tpl,
                            language = language,
                            useLabel = strings.useTemplate,
                            onClick = { onTemplateClick(tpl) }
                        )
                    }
                }
            }
        }

        // Saved Drafts Header
        item {
            Text(
                text = strings.myProjects,
                style = MaterialTheme.typography.titleMedium,
                color = StudioTextPrimary
            )
        }

        if (projects.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = StudioSurfaceVariant,
                    border = BorderStroke(1.dp, StudioBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MovieCreation,
                            contentDescription = null,
                            tint = StudioCyan,
                            modifier = Modifier.size(38.dp)
                        )
                        Text(
                            text = strings.emptyProjectsTitle,
                            style = MaterialTheme.typography.titleSmall,
                            color = StudioTextPrimary
                        )
                        Text(
                            text = strings.emptyProjectsSubtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = StudioTextMuted
                        )
                    }
                }
            }
        } else {
            items(projects, key = { it.id }) { project ->
                ProjectDraftCard(
                    project = project,
                    strings = strings,
                    onClick = { onOpenProject(project.id) },
                    onRename = { onRenameProject(project) },
                    onDuplicate = { onDuplicateProject(project) },
                    onDelete = { onDeleteProject(project.id) }
                )
            }
        }
    }
}

@Composable
private fun QuickToolButton(
    icon: ImageVector,
    label: String,
    color: Color,
    tag: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = StudioSurfaceVariant,
        border = BorderStroke(1.dp, StudioBorder),
        modifier = Modifier
            .size(width = 76.dp, height = 78.dp)
            .testTag(tag)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = color,
                    modifier = Modifier.size(19.dp)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = StudioTextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun TemplateMiniCard(
    template: StudioTemplate,
    language: AppLanguage,
    useLabel: String,
    onClick: () -> Unit
) {
    val title = if (language == AppLanguage.KHMER) template.titleKm else template.titleEn
    val tag = if (language == AppLanguage.KHMER) template.tagKm else template.tagEn
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, StudioBorder),
        modifier = Modifier
            .size(width = 148.dp, height = 200.dp)
            .testTag("template_card_${template.id}")
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                painter = painterResource(id = template.coverResId),
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color(0xCC0A0B10))
                        )
                    )
            )
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    color = StudioTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = tag,
                    style = MaterialTheme.typography.labelSmall,
                    color = StudioCyan
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = StudioCyan
                ) {
                    Text(
                        text = useLabel,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.Black,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ProjectDraftCard(
    project: VideoProject,
    strings: ThyCutStrings,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val firstClip = project.clips.firstOrNull()
    var thumb by remember(firstClip?.uriString) { mutableStateOf<Bitmap?>(null) }
    var menuExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(firstClip?.uriString) {
        if (firstClip != null) {
            thumb = ThumbnailExtractor.extractThumbnail(
                context = context,
                uriString = firstClip.uriString,
                isImage = firstClip.isImage,
                timeMs = firstClip.trimStartMs,
                targetWidth = 220
            )
        }
    }

    val dateStr = remember(project.updatedAt) {
        DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(project.updatedAt))
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = StudioSurfaceVariant,
        border = BorderStroke(1.dp, StudioBorder),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("project_card_${project.id}")
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(width = 82.dp, height = 64.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(StudioSurfaceElevated),
                contentAlignment = Alignment.Center
            ) {
                if (thumb != null) {
                    Image(
                        bitmap = thumb!!.asImageBitmap(),
                        contentDescription = project.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.MovieCreation,
                        contentDescription = null,
                        tint = StudioCyan
                    )
                }
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color.Black.copy(alpha = 0.72f),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                ) {
                    Text(
                        text = formatShortDuration(project.totalDurationMs),
                        style = MonoTimecodeStyle.copy(fontSize = 9.sp),
                        color = StudioTextPrimary,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = project.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = StudioTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${strings.clipsCount(project.clips.size)} • ${project.aspectRatio.label}",
                    style = MaterialTheme.typography.bodySmall,
                    color = StudioCyan
                )
                Text(
                    text = dateStr,
                    style = MaterialTheme.typography.labelSmall,
                    color = StudioTextMuted
                )
            }

            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Options",
                        tint = StudioTextMuted
                    )
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(strings.renameProject) },
                        leadingIcon = { Icon(Icons.Default.Edit, null) },
                        onClick = {
                            menuExpanded = false
                            onRename()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(strings.duplicateProject) },
                        leadingIcon = { Icon(Icons.Default.ContentCopy, null) },
                        onClick = {
                            menuExpanded = false
                            onDuplicate()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(strings.deleteProject, color = StudioCoral) },
                        leadingIcon = { Icon(Icons.Default.Delete, null, tint = StudioCoral) },
                        onClick = {
                            menuExpanded = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun TemplatesContent(
    strings: ThyCutStrings,
    language: AppLanguage,
    onUseTemplate: (StudioTemplate) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = strings.templatesTitle,
                style = MaterialTheme.typography.headlineSmall,
                color = StudioTextPrimary
            )
        }
        items(StudioTemplates.all, key = { it.id }) { tpl ->
            val title = if (language == AppLanguage.KHMER) tpl.titleKm else tpl.titleEn
            val tag = if (language == AppLanguage.KHMER) tpl.tagKm else tpl.tagEn
            Surface(
                onClick = { onUseTemplate(tpl) },
                shape = RoundedCornerShape(18.dp),
                color = StudioSurfaceVariant,
                border = BorderStroke(1.dp, StudioBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
            ) {
                Row(modifier = Modifier.fillMaxSize()) {
                    Image(
                        painter = painterResource(id = tpl.coverResId),
                        contentDescription = title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .width(120.dp)
                            .fillMaxHeight()
                    )
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(14.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleMedium,
                                color = StudioTextPrimary
                            )
                            Text(
                                text = tag,
                                style = MaterialTheme.typography.labelMedium,
                                color = StudioCyan
                            )
                        }
                        Button(
                            onClick = { onUseTemplate(tpl) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = StudioCyan,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(strings.useTemplate, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExportsContent(
    strings: ThyCutStrings,
    exports: List<ExportRecord>,
    onPlay: (ExportRecord) -> Unit,
    onShare: (ExportRecord) -> Unit,
    onDelete: (ExportRecord) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = strings.exportedVideos,
                style = MaterialTheme.typography.headlineSmall,
                color = StudioTextPrimary
            )
        }
        if (exports.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = StudioSurfaceVariant,
                    border = BorderStroke(1.dp, StudioBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.VideoLibrary,
                            contentDescription = null,
                            tint = StudioCyan,
                            modifier = Modifier.size(40.dp)
                        )
                        Text(
                            text = strings.emptyExportsTitle,
                            style = MaterialTheme.typography.titleSmall,
                            color = StudioTextPrimary
                        )
                        Text(
                            text = strings.emptyExportsSubtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = StudioTextMuted
                        )
                    }
                }
            }
        } else {
            items(exports, key = { it.id }) { record ->
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = StudioSurfaceVariant,
                    border = BorderStroke(1.dp, StudioBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPlay(record) }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = StudioCyan.copy(alpha = 0.16f),
                            modifier = Modifier.size(52.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = strings.playExported,
                                    tint = StudioCyan,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = record.projectTitle,
                                style = MaterialTheme.typography.titleMedium,
                                color = StudioTextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${record.resolutionLabel} • ${formatShortDuration(record.durationMs)} • ${formatFileSize(record.fileSizeBytes)}",
                                style = MonoTimecodeStyle.copy(fontSize = 11.sp),
                                color = StudioCyan
                            )
                        }
                        IconButton(onClick = { onShare(record) }) {
                            Icon(Icons.Default.Share, contentDescription = strings.shareVideo, tint = StudioTextPrimary)
                        }
                        IconButton(onClick = { onDelete(record) }) {
                            Icon(Icons.Default.Delete, contentDescription = strings.confirmDelete, tint = StudioCoral)
                        }
                    }
                }
            }
        }
    }
}
