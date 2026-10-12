package com.cortex.ai.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.AnchoredDraggableDefaults
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.AbsoluteRoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Settings
import com.cortex.ai.agent.agy.AgyProjectRepository
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.zIndex
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.cortex.ai.R
import com.cortex.ai.ui.cortex.*
import com.cortex.ai.ui.model.AgyProjectUi
import com.cortex.ai.ui.model.ConversationPaneUiState
import com.cortex.ai.ui.model.ConversationSummaryUi
import java.io.File
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.collectLatest
import top.yukonga.miuix.kmp.basic.DropdownDefaults
import top.yukonga.miuix.kmp.basic.DropdownImpl
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.InputField
import top.yukonga.miuix.kmp.basic.ListPopupColumn
import top.yukonga.miuix.kmp.basic.ListPopupDefaults
import top.yukonga.miuix.kmp.basic.PopupPositionProvider
import top.yukonga.miuix.kmp.basic.SearchBar
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.blur.BlendColorEntry
import top.yukonga.miuix.kmp.blur.BlurDefaults
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.textureBlur
import top.yukonga.miuix.kmp.nav.core.rememberNavSystemCornerRadius
import top.yukonga.miuix.kmp.squircle.absoluteSquircleClip
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic
import top.yukonga.miuix.kmp.window.WindowListPopup

private object DrawerMetrics {
    val PaneMaxWidth = 340.dp
    const val PaneWidthFraction = 0.84f
    val PaneHorizontalPadding = 14.dp
    val TopInset = 8.dp
    val AfterActionBar = 6.dp
    val BottomInset = 6.dp
    val DockTopGap = 6.dp
    val RowGap = 2.dp
    val RowMinHeight = 44.dp
    val RowHorizontalPadding = 10.dp
    val RowVerticalPadding = 6.dp
    val RowCornerRadius = 10.dp
    val ActiveDotSize = 6.dp
    val ActiveDotGap = 6.dp
    val ActionIconSize = 16.dp
    val EmptyVerticalPadding = 12.dp
    val ForegroundCornerRadiusFallback = 28.dp
    val ForegroundShadowRadius = 16.dp
    const val ForegroundShadowAlpha = 0.16f
    const val SettleDampingRatio = 0.88f
    const val SettleStiffness = 380f
    const val SettleVisibilityThresholdPx = 0.5f
    const val SettlePositionThresholdFraction = 0.42f
    val DockEntryCornerRadius = 10.dp
    val DockEntryIconSize = 20.dp
    val DockEntryLabelGap = 3.dp
    val DockEntryVerticalPadding = 5.dp
}

private enum class ConversationPaneAnchor {
    Closed,
    Open,
}

@Composable
fun ConversationSidePaneScaffold(
    state: ConversationPaneUiState,
    visible: Boolean,
    backHandlerEnabled: Boolean,
    onOpen: () -> Unit,
    onDismiss: () -> Unit,
    onSearchChange: (String) -> Unit,
    onConversationSelected: (String) -> Unit,
    onNewConversation: () -> Unit = {},
    onSelectProject: (String) -> Unit = {},
    onCreateProject: (name: String, workspacePath: String) -> Unit = { _, _ -> },
    onConversationRename: (ConversationSummaryUi) -> Unit,
    onConversationExport: (ConversationSummaryUi) -> Unit,
    onConversationDelete: (ConversationSummaryUi) -> Unit,
    onConversationArchive: (ConversationSummaryUi) -> Unit = {},
    onConversationUnarchive: (String) -> Unit = {},
    onOpenSettings: () -> Unit,
    onOpenModelProviders: () -> Unit,
    onOpenTools: () -> Unit,
    onOpenSkills: () -> Unit,
    onOpenTerminal: () -> Unit,
    onOpenPermissions: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val sceneLifecycleState by LocalLifecycleOwner.current.lifecycle.currentStateFlow.collectAsState()
    val navigationEventState = rememberNavigationEventState(NavigationEventInfo.None)

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val paneWidth = minOf(maxWidth * DrawerMetrics.PaneWidthFraction, DrawerMetrics.PaneMaxWidth)
        val paneWidthPx = with(density) { paneWidth.toPx() }
        val anchors = remember(paneWidthPx) {
            DraggableAnchors {
                ConversationPaneAnchor.Closed at 0f
                ConversationPaneAnchor.Open at paneWidthPx
            }
        }
        val paneDragState = remember {
            AnchoredDraggableState(
                initialValue = if (visible) ConversationPaneAnchor.Open else ConversationPaneAnchor.Closed,
                anchors = anchors,
            )
        }
        val settleAnimation = remember {
            spring<Float>(
                dampingRatio = DrawerMetrics.SettleDampingRatio,
                stiffness = DrawerMetrics.SettleStiffness,
                visibilityThreshold = DrawerMetrics.SettleVisibilityThresholdPx,
            )
        }
        val flingBehavior = AnchoredDraggableDefaults.flingBehavior(
            state = paneDragState,
            positionalThreshold = { distance ->
                distance * DrawerMetrics.SettlePositionThresholdFraction
            },
            animationSpec = settleAnimation,
        )
        val currentVisible by rememberUpdatedState(visible)
        val currentOnOpen by rememberUpdatedState(onOpen)
        val currentOnDismiss by rememberUpdatedState(onDismiss)
        val shouldClipForeground by remember(paneDragState) {
            derivedStateOf {
                val offset = paneDragState.offset
                !offset.isNaN() && offset > 0.5f
            }
        }
        val systemCornerRadius = rememberNavSystemCornerRadius()
        val foregroundCornerRadius = systemCornerRadius.takeIf { it > 0.dp }
            ?: DrawerMetrics.ForegroundCornerRadiusFallback

        SideEffect {
            paneDragState.updateAnchors(anchors)
        }

        LaunchedEffect(visible, paneWidthPx) {
            val target = if (visible) ConversationPaneAnchor.Open else ConversationPaneAnchor.Closed
            if (paneDragState.targetValue != target || paneDragState.settledValue != target) {
                paneDragState.animateTo(target, settleAnimation)
            }
        }

        LaunchedEffect(paneDragState) {
            snapshotFlow { paneDragState.settledValue }.collectLatest { settledValue ->
                val settledOpen = settledValue == ConversationPaneAnchor.Open
                if (settledOpen != currentVisible) {
                    if (settledOpen) currentOnOpen() else currentOnDismiss()
                }
            }
        }

        NavigationBackHandler(
            state = navigationEventState,
            isBackEnabled = visible &&
                backHandlerEnabled &&
                sceneLifecycleState == Lifecycle.State.RESUMED,
            onBackCompleted = onDismiss,
        )

        ConversationPanePanel(
            state = state,
            width = paneWidth,
            onSearchChange = onSearchChange,
            onConversationSelected = {
                onConversationSelected(it)
                onDismiss()
            },
            onNewConversation = {
                onNewConversation()
                onDismiss()
            },
            onSelectProject = onSelectProject,
            onCreateProject = onCreateProject,
            onConversationRename = onConversationRename,
            onConversationExport = onConversationExport,
            onConversationDelete = onConversationDelete,
            onConversationArchive = onConversationArchive,
            onConversationUnarchive = onConversationUnarchive,
            onOpenSettings = onOpenSettings,
            onOpenModelProviders = onOpenModelProviders,
            onOpenTools = onOpenTools,
            onOpenSkills = onOpenSkills,
            onOpenTerminal = onOpenTerminal,
            onOpenPermissions = onOpenPermissions,
            modifier = Modifier.zIndex(0f),
        )

        Box(
            modifier = Modifier
                .width(paneWidth)
                .fillMaxHeight()
                .graphicsLayer {
                    val offset = paneDragState.offset.takeUnless(Float::isNaN) ?: 0f
                    val progress = if (paneWidthPx > 0f) {
                        (offset / paneWidthPx).coerceIn(0f, 1f)
                    } else {
                        0f
                    }
                    alpha = 1f - progress
                }
                .background(MiuixTheme.colorScheme.windowDimming)
                .zIndex(0.5f),
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset {
                    val offset = paneDragState.offset.takeUnless(Float::isNaN)
                        ?: if (visible) paneWidthPx else 0f
                    IntOffset(offset.roundToInt(), 0)
                }
                .then(
                    if (shouldClipForeground) {
                        Modifier
                            .dropShadow(
                                shape = AbsoluteRoundedCornerShape(
                                    topLeft = foregroundCornerRadius,
                                    topRight = 0.dp,
                                    bottomRight = 0.dp,
                                    bottomLeft = foregroundCornerRadius,
                                ),
                                shadow = Shadow(
                                    radius = DrawerMetrics.ForegroundShadowRadius,
                                    color = Color.Black,
                                    alpha = DrawerMetrics.ForegroundShadowAlpha,
                                ),
                            )
                            .absoluteSquircleClip(
                                topLeft = foregroundCornerRadius,
                                topRight = 0.dp,
                                bottomRight = 0.dp,
                                bottomLeft = foregroundCornerRadius,
                            )
                    } else {
                        Modifier
                    },
                )
                .anchoredDraggable(
                    state = paneDragState,
                    reverseDirection = false,
                    orientation = Orientation.Horizontal,
                    enabled = backHandlerEnabled,
                    flingBehavior = flingBehavior,
                )
                .zIndex(1f),
        ) {
            content()
            if (visible) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(onClick = onDismiss),
                )
            }
        }
    }
}

@Composable
private fun ConversationPanePanel(
    state: ConversationPaneUiState,
    width: androidx.compose.ui.unit.Dp,
    onSearchChange: (String) -> Unit,
    onConversationSelected: (String) -> Unit,
    onNewConversation: () -> Unit,
    onSelectProject: (String) -> Unit,
    onCreateProject: (name: String, workspacePath: String) -> Unit,
    onConversationRename: (ConversationSummaryUi) -> Unit,
    onConversationExport: (ConversationSummaryUi) -> Unit,
    onConversationDelete: (ConversationSummaryUi) -> Unit,
    onConversationArchive: (ConversationSummaryUi) -> Unit,
    onConversationUnarchive: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenModelProviders: () -> Unit,
    onOpenTools: () -> Unit,
    onOpenSkills: () -> Unit,
    onOpenTerminal: () -> Unit,
    onOpenPermissions: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val query = state.searchQuery.trim()
    val density = LocalDensity.current

    var showFolderPicker by remember { mutableStateOf(false) }
    var showProjectSwitcher by remember { mutableStateOf(false) }
    var viewingArchivedProjectId by remember { mutableStateOf<String?>(null) }

    val activeProject = remember(state.projects, state.selectedProjectId) {
        state.projects.find { it.id == state.selectedProjectId }
            ?: state.projects.firstOrNull()
            ?: AgyProjectUi(id = "default-cli-project", name = "默认工作区", workspacePath = "/root")
    }

    var expandedProjectIds by remember(state.projects, activeProject.id) {
        mutableStateOf(setOf(activeProject.id))
    }

    Surface(
        modifier = modifier
            .width(width)
            .fillMaxHeight(),
        color = MiuixTheme.colorScheme.surface,
        contentColor = MiuixTheme.colorScheme.onSurface,
    ) {
        val listState = rememberLazyListState()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(
                    WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal),
                ),
        ) {
            // 顶部固定 Header（工作区卡片、新会话/选目录、搜索栏）
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = DrawerMetrics.PaneHorizontalPadding),
            ) {
                Spacer(modifier = Modifier.height(DrawerMetrics.TopInset))
                AntigravityProjectCard(
                    project = activeProject,
                    onClick = { showProjectSwitcher = true },
                )
                Spacer(modifier = Modifier.height(8.dp))
                AntigravityActionButtons(
                    onNewChat = onNewConversation,
                    onPickFolder = { showFolderPicker = true },
                )
                Spacer(modifier = Modifier.height(8.dp))
                PaneActionBar(
                    query = state.searchQuery,
                    onSearchChange = onSearchChange,
                )
                Spacer(modifier = Modifier.height(DrawerMetrics.AfterActionBar))
            }

            // 中间会话列表区域（物理独立，绝不与 Header/Dock 重叠）
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = DrawerMetrics.PaneHorizontalPadding)
                    .scrollEndHaptic()
                    .overScrollVertical(),
                verticalArrangement = Arrangement.spacedBy(DrawerMetrics.RowGap),
                overscrollEffect = null,
            ) {
                if (query.isNotBlank()) {
                    if (state.conversations.isEmpty()) {
                        item { EmptyConversations(isSearching = true) }
                    } else {
                        items(state.conversations, key = { it.id }) { conversation ->
                            ConversationTextRow(
                                conversation = conversation,
                                selected = conversation.id == state.selectedConversationId,
                                onClick = { onConversationSelected(conversation.id) },
                                onRename = { onConversationRename(conversation) },
                                onExport = { onConversationExport(conversation) },
                                onDelete = { onConversationDelete(conversation) },
                                onArchive = { onConversationArchive(conversation) },
                            )
                        }
                    }
                } else {
                    if (state.projects.isEmpty()) {
                        item { EmptyConversations(isSearching = false) }
                    } else {
                        state.projects.forEach { project ->
                            val isExpanded = expandedProjectIds.contains(project.id)
                            item(key = "proj-${project.id}") {
                                ProjectSectionHeader(
                                    project = project,
                                    isExpanded = isExpanded,
                                    isActive = project.id == activeProject.id,
                                    onToggle = {
                                        expandedProjectIds = if (isExpanded) {
                                            expandedProjectIds - project.id
                                        } else {
                                            expandedProjectIds + project.id
                                        }
                                        if (project.id != activeProject.id) {
                                            onSelectProject(project.id)
                                        }
                                    },
                                    onSelect = { onSelectProject(project.id) },
                                    onViewArchived = { viewingArchivedProjectId = project.id },
                                )
                            }
                            if (isExpanded) {
                                if (project.conversations.isEmpty()) {
                                    item(key = "proj-${project.id}-empty") {
                                        Text(
                                            text = "暂无会话，点击「+ 新对话」开始",
                                            style = MiuixTheme.textStyles.footnote1,
                                            color = MiuixTheme.colorScheme.onSurfaceVariantActions,
                                            modifier = Modifier.padding(start = 28.dp, top = 4.dp, bottom = 8.dp),
                                        )
                                    }
                                } else {
                                    items(project.conversations, key = { it.id }) { conversation ->
                                        ConversationTextRow(
                                            conversation = conversation,
                                            selected = conversation.id == state.selectedConversationId,
                                            onClick = { onConversationSelected(conversation.id) },
                                            onRename = { onConversationRename(conversation) },
                                            onExport = { onConversationExport(conversation) },
                                            onDelete = { onConversationDelete(conversation) },
                                            onArchive = { onConversationArchive(conversation) },
                                        )
                                    }
                                }
                                // 在项目下方提供按项目查看归档会话入口
                                item(key = "proj-${project.id}-archived-entry") {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable(onClick = { viewingArchivedProjectId = project.id })
                                            .padding(start = 28.dp, top = 4.dp, bottom = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Inventory2,
                                            contentDescription = null,
                                            tint = MiuixTheme.colorScheme.onSurfaceVariantActions,
                                            modifier = Modifier.size(14.dp),
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "查看已归档会话",
                                            style = MiuixTheme.textStyles.footnote2,
                                            color = MiuixTheme.colorScheme.onSurfaceVariantActions,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 底部固定 Dock
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = DrawerMetrics.PaneHorizontalPadding),
            ) {
                Spacer(modifier = Modifier.height(DrawerMetrics.DockTopGap))

                AntigravityAccountStatus(
                    email = state.accountEmail,
                )
                Spacer(modifier = Modifier.height(6.dp))

                AntigravityModelTag(
                    modelName = state.activeModelName,
                    onClick = onOpenModelProviders,
                )
                Spacer(modifier = Modifier.height(8.dp))

                PaneDock(
                    onOpenSettings = onOpenSettings,
                    onOpenModelProviders = onOpenModelProviders,
                    onOpenTools = onOpenTools,
                    onOpenSkills = onOpenSkills,
                    onOpenTerminal = onOpenTerminal,
                    onOpenPermissions = onOpenPermissions,
                )
                Spacer(modifier = Modifier.height(DrawerMetrics.BottomInset))
            }
        }
    }

    if (showFolderPicker) {
        FolderPickerDialog(
            onDismiss = { showFolderPicker = false },
            onConfirm = { name, path ->
                showFolderPicker = false
                onCreateProject(name, path)
            },
        )
    }

    if (showProjectSwitcher) {
        ProjectSwitcherDialog(
            projects = state.projects,
            selectedProjectId = activeProject.id,
            onDismiss = { showProjectSwitcher = false },
            onSelect = { projId ->
                showProjectSwitcher = false
                onSelectProject(projId)
            },
            onAddProject = {
                showProjectSwitcher = false
                showFolderPicker = true
            },
        )
    }

    if (viewingArchivedProjectId != null) {
        val projId = viewingArchivedProjectId!!
        val proj = state.projects.find { it.id == projId }
        val projName = proj?.name ?: "项目"
        ArchivedConversationsDialog(
            projectName = projName,
            conversations = AgyProjectRepository.getArchivedConversations(projId),
            onDismissRequest = { viewingArchivedProjectId = null },
            onSelectConversation = { convId ->
                viewingArchivedProjectId = null
                onConversationSelected(convId)
            },
            onUnarchive = { convId ->
                onConversationUnarchive(convId)
            },
            onDelete = { convId ->
                val conv = AgyProjectRepository.getArchivedConversations(projId).find { it.id == convId }
                if (conv != null) {
                    onConversationDelete(conv)
                }
            },
        )
    }
}

@Composable
private fun AntigravityProjectCard(
    project: AgyProjectUi,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        color = MiuixTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Rounded.Folder,
                contentDescription = null,
                tint = MiuixTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = project.name,
                    style = MiuixTheme.textStyles.body2,
                    fontWeight = FontWeight.SemiBold,
                    color = MiuixTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = project.workspacePath,
                    style = MiuixTheme.textStyles.footnote2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantActions,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(
                imageVector = Icons.Rounded.KeyboardArrowDown,
                contentDescription = null,
                tint = MiuixTheme.colorScheme.onSurfaceVariantActions,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun AntigravityActionButtons(
    onNewChat: () -> Unit,
    onPickFolder: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            modifier = Modifier
                .weight(1f)
                .height(38.dp)
                .clip(RoundedCornerShape(19.dp))
                .clickable(onClick = onNewChat),
            color = MiuixTheme.colorScheme.primary,
            shape = RoundedCornerShape(19.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = null,
                    tint = MiuixTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "新对话",
                    style = MiuixTheme.textStyles.body2,
                    fontWeight = FontWeight.SemiBold,
                    color = MiuixTheme.colorScheme.onPrimary,
                )
            }
        }

        Surface(
            modifier = Modifier
                .height(38.dp)
                .clip(RoundedCornerShape(19.dp))
                .clickable(onClick = onPickFolder),
            color = MiuixTheme.colorScheme.surfaceContainerHigh,
            shape = RoundedCornerShape(19.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(horizontal = 14.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Download,
                    contentDescription = null,
                    tint = MiuixTheme.colorScheme.onSurface,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "选择目录",
                    style = MiuixTheme.textStyles.body2,
                    fontWeight = FontWeight.Medium,
                    color = MiuixTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

@Composable
private fun ProjectSectionHeader(
    project: AgyProjectUi,
    isExpanded: Boolean,
    isActive: Boolean,
    onToggle: () -> Unit,
    onSelect: () -> Unit,
    onViewArchived: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onToggle)
            .padding(vertical = 6.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = if (isExpanded) Icons.Rounded.KeyboardArrowDown else Icons.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = MiuixTheme.colorScheme.onSurfaceVariantActions,
            modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(4.dp))
        Icon(
            imageVector = Icons.Rounded.Inventory2,
            contentDescription = null,
            tint = if (isActive) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.onSurfaceVariantActions,
            modifier = Modifier.size(16.dp),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = project.name,
            style = MiuixTheme.textStyles.body2,
            fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Medium,
            color = if (isActive) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = project.conversations.size.toString(),
            style = MiuixTheme.textStyles.footnote2,
            color = MiuixTheme.colorScheme.onSurfaceVariantActions,
            modifier = Modifier
                .clip(CircleShape)
                .background(MiuixTheme.colorScheme.surfaceContainerHigh)
                .padding(horizontal = 6.dp, vertical = 2.dp),
        )
        Spacer(modifier = Modifier.width(4.dp))
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(CircleShape)
                .clickable(onClick = onViewArchived),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.Inventory2,
                contentDescription = "查看已归档会话",
                tint = MiuixTheme.colorScheme.onSurfaceVariantActions,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

@Composable
private fun AntigravityAccountStatus(
    email: String?,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MiuixTheme.colorScheme.surfaceContainer)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Rounded.AccountCircle,
            contentDescription = null,
            tint = MiuixTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = email?.ifBlank { "Cortex Local Runtime" } ?: "Cortex Local Runtime",
                style = MiuixTheme.textStyles.footnote1,
                fontWeight = FontWeight.SemiBold,
                color = MiuixTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = if (!email.isNullOrBlank()) "主脑授权账号已就绪" else "本地具身引擎已就绪",
                style = MiuixTheme.textStyles.footnote2,
                color = MiuixTheme.colorScheme.onSurfaceVariantActions,
            )
        }
    }
}

@Composable
private fun AntigravityModelTag(
    modelName: String,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        color = MiuixTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(8.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Rounded.AutoAwesome,
                contentDescription = null,
                tint = MiuixTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = modelName,
                style = MiuixTheme.textStyles.footnote1,
                color = MiuixTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "切换",
                style = MiuixTheme.textStyles.footnote2,
                color = MiuixTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun FolderPickerDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, path: String) -> Unit,
) {
    var currentDir by remember { mutableStateOf(File("/storage/emulated/0")) }
    var projectName by remember { mutableStateOf(currentDir.name.ifBlank { "项目工作区" }) }

    val subDirs = remember(currentDir) {
        runCatching {
            currentDir.listFiles()?.filter { it.isDirectory && !it.name.startsWith(".") }?.sortedBy { it.name.lowercase() }
        }.getOrNull().orEmpty()
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp)),
            color = MiuixTheme.colorScheme.surface,
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
            ) {
                Text(
                    text = "选择项目工作区目录",
                    style = MiuixTheme.textStyles.title2,
                    fontWeight = FontWeight.Bold,
                    color = MiuixTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    listOf(
                        "内部存储" to "/storage/emulated/0",
                        "下载" to "/storage/emulated/0/Download",
                        "文档" to "/storage/emulated/0/Documents",
                        "根目录" to "/data/data/com.cortex.ai/files/terminal/debian/rootfs/root",
                    ).forEach { (label, path) ->
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    val f = File(path)
                                    if (f.exists()) {
                                        currentDir = f
                                        projectName = f.name.ifBlank { "项目工作区" }
                                    }
                                },
                            color = MiuixTheme.colorScheme.surfaceContainerHigh,
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Text(
                                text = label,
                                style = MiuixTheme.textStyles.footnote2,
                                color = MiuixTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "当前路径: ${currentDir.absolutePath}",
                    style = MiuixTheme.textStyles.footnote1,
                    color = MiuixTheme.colorScheme.onSurfaceVariantActions,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MiuixTheme.colorScheme.surfaceContainer)
                        .padding(6.dp),
                ) {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        if (currentDir.parentFile != null && currentDir.absolutePath != "/") {
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(6.dp))
                                        .clickable {
                                            currentDir = currentDir.parentFile ?: currentDir
                                            projectName = currentDir.name.ifBlank { "项目工作区" }
                                        }
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Folder,
                                        contentDescription = null,
                                        tint = MiuixTheme.colorScheme.onSurfaceVariantActions,
                                        modifier = Modifier.size(18.dp),
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = ".. (返回上一级)",
                                        style = MiuixTheme.textStyles.body2,
                                        color = MiuixTheme.colorScheme.onSurface,
                                    )
                                }
                            }
                        }
                        items(subDirs) { dir ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable {
                                        currentDir = dir
                                        projectName = dir.name
                                    }
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Folder,
                                    contentDescription = null,
                                    tint = MiuixTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = dir.name,
                                    style = MiuixTheme.textStyles.body2,
                                    color = MiuixTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "项目显示名称",
                    style = MiuixTheme.textStyles.footnote1,
                    fontWeight = FontWeight.Medium,
                    color = MiuixTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(4.dp))
                InputField(
                    query = projectName,
                    onQueryChange = { projectName = it },
                    onSearch = {},
                    expanded = false,
                    onExpandedChange = {},
                    label = "输入项目名称",
                )
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "取消",
                        style = MiuixTheme.textStyles.body2,
                        color = MiuixTheme.colorScheme.onSurfaceVariantActions,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(onClick = onDismiss)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                onConfirm(projectName.ifBlank { currentDir.name }, currentDir.absolutePath)
                            },
                        color = MiuixTheme.colorScheme.primary,
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Text(
                            text = "确定选择",
                            style = MiuixTheme.textStyles.body2,
                            fontWeight = FontWeight.SemiBold,
                            color = MiuixTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProjectSwitcherDialog(
    projects: List<AgyProjectUi>,
    selectedProjectId: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit,
    onAddProject: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp)),
            color = MiuixTheme.colorScheme.surface,
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
            ) {
                Text(
                    text = "切换项目工作区",
                    style = MiuixTheme.textStyles.title2,
                    fontWeight = FontWeight.Bold,
                    color = MiuixTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 240.dp),
                ) {
                    items(projects, key = { it.id }) { proj ->
                        val isSelected = proj.id == selectedProjectId
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) MiuixTheme.colorScheme.surfaceContainerHigh else Color.Transparent)
                                .clickable { onSelect(proj.id) }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Folder,
                                contentDescription = null,
                                tint = if (isSelected) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.onSurfaceVariantActions,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = proj.name,
                                    style = MiuixTheme.textStyles.body2,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                    color = if (isSelected) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = proj.workspacePath,
                                    style = MiuixTheme.textStyles.footnote2,
                                    color = MiuixTheme.colorScheme.onSurfaceVariantActions,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Rounded.Schedule,
                                    contentDescription = null,
                                    tint = MiuixTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "+ 绑定新文件夹",
                        style = MiuixTheme.textStyles.body2,
                        fontWeight = FontWeight.SemiBold,
                        color = MiuixTheme.colorScheme.primary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(onClick = onAddProject)
                            .padding(8.dp),
                    )
                    Text(
                        text = "关闭",
                        style = MiuixTheme.textStyles.body2,
                        color = MiuixTheme.colorScheme.onSurfaceVariantActions,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(onClick = onDismiss)
                            .padding(8.dp),
                    )
                }
            }
        }
    }
}

private val PaneDividerThickness = 0.5.dp
private const val PaneDividerAlpha = 0.5f
private const val PaneFrostBlurRadius = 25f
private const val PaneFrostSurfaceAlpha = 0.78f

@Composable
private fun PaneFrostRegion(
    backdrop: LayerBackdrop?,
    modifier: Modifier = Modifier,
    showDivider: Boolean = false,
    dividerAtTop: Boolean = false,
    content: @Composable () -> Unit,
) {
    val surfaceColor = MiuixTheme.colorScheme.surface
    val frostModifier = if (backdrop == null) {
        modifier.background(surfaceColor)
    } else {
        modifier.textureBlur(
            backdrop = backdrop,
            shape = RectangleShape,
            blurRadius = PaneFrostBlurRadius,
            colors = BlurDefaults.blurColors(
                blendColors = listOf(
                    BlendColorEntry(surfaceColor.copy(alpha = PaneFrostSurfaceAlpha)),
                ),
            ),
        )
    }
    Box(modifier = frostModifier) {
        content()
        AnimatedVisibility(
            visible = showDivider,
            modifier = Modifier
                .align(if (dividerAtTop) Alignment.TopCenter else Alignment.BottomCenter)
                .fillMaxWidth(),
            enter = fadeIn(animationSpec = tween(durationMillis = 140)),
            exit = fadeOut(animationSpec = tween(durationMillis = 140)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(PaneDividerThickness)
                    .background(MiuixTheme.colorScheme.outline.copy(alpha = PaneDividerAlpha)),
            )
        }
    }
}

@Composable
private fun PaneActionBar(
    query: String,
    onSearchChange: (String) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SearchBar(
            modifier = Modifier.weight(1f),
            expanded = false,
            onExpandedChange = {},
            inputField = {
                InputField(
                    query = query,
                    onQueryChange = onSearchChange,
                    onSearch = onSearchChange,
                    expanded = false,
                    onExpandedChange = {},
                    label = stringResource(R.string.conversation_search_hint),
                )
            },
            content = {},
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ConversationTextRow(
    conversation: ConversationSummaryUi,
    selected: Boolean,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onExport: () -> Unit,
    onDelete: () -> Unit,
    onArchive: () -> Unit,
) {
    var showActionMenu by remember { mutableStateOf(false) }
    var showActionDialog by remember { mutableStateOf(false) }
    val hapticFeedback = LocalHapticFeedback.current

    Box(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = DrawerMetrics.RowMinHeight)
                .clip(RoundedCornerShape(DrawerMetrics.RowCornerRadius))
                .background(
                    if (selected) {
                        MiuixTheme.colorScheme.primary.copy(alpha = 0.12f)
                    } else {
                        Color.Transparent
                    },
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 左侧会话内容主体（点击选中，长按呼出上下文菜单）
            Row(
                modifier = Modifier
                    .weight(1f)
                    .combinedClickable(
                        onClick = onClick,
                        onLongClick = {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                            showActionMenu = true
                        },
                    )
                    .padding(
                        start = DrawerMetrics.RowHorizontalPadding,
                        top = DrawerMetrics.RowVerticalPadding,
                        bottom = DrawerMetrics.RowVerticalPadding,
                        end = 4.dp,
                    ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // 选中的光标指示条：左侧垂直 pill
                if (selected) {
                    Box(
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .width(3.5.dp)
                            .height(20.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(MiuixTheme.colorScheme.primary),
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    val title = conversation.title.ifBlank { conversation.preview }
                    Text(
                        text = title,
                        color = if (selected) {
                            MiuixTheme.colorScheme.primary
                        } else {
                            MiuixTheme.colorScheme.onSurface
                        },
                        style = MiuixTheme.textStyles.body1,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        if (conversation.timeLabel.isNotBlank()) {
                            Text(
                                text = conversation.timeLabel,
                                style = MiuixTheme.textStyles.footnote1,
                                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            )
                        }
                        if (conversation.preview.isNotBlank() && conversation.preview != title) {
                            Text(
                                text = "· " + conversation.preview,
                                style = MiuixTheme.textStyles.footnote1,
                                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false),
                            )
                        }
                    }
                }

                if (conversation.isActiveRun) {
                    Box(
                        modifier = Modifier
                            .padding(horizontal = DrawerMetrics.ActiveDotGap)
                            .size(DrawerMetrics.ActiveDotSize)
                            .clip(CircleShape)
                            .background(MiuixTheme.colorScheme.primary),
                    )
                }
            }

            // 右侧 x 号按键：物理独立兄弟节点，热区 40dp，绝不冒泡触发选中/关闭抽屉
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable { showActionDialog = true },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "会话操作",
                    tint = MiuixTheme.colorScheme.onSurfaceVariantActions,
                    modifier = Modifier.size(16.dp),
                )
            }
        }

        // 弹窗询问：删除、归档、取消
        if (showActionDialog) {
            ConversationActionDialog(
                conversationTitle = conversation.title.ifBlank { conversation.preview },
                onDismissRequest = { showActionDialog = false },
                onDelete = onDelete,
                onArchive = onArchive,
            )
        }

        WindowListPopup(
            show = showActionMenu,
            popupPositionProvider = ListPopupDefaults.ContextMenuPositionProvider,
            alignment = PopupPositionProvider.Align.BottomEnd,
            onDismissRequest = { showActionMenu = false },
        ) {
            val renameText = stringResource(R.string.action_rename)
            val exportText = stringResource(R.string.action_export)
            val deleteText = stringResource(R.string.action_delete)
            val renameItem = remember(renameText) {
                DropdownItem(
                    text = renameText,
                    icon = { modifier ->
                        Icon(
                            imageVector = Icons.Rounded.Edit,
                            contentDescription = null,
                            modifier = modifier.size(DrawerMetrics.ActionIconSize),
                        )
                    },
                )
            }
            val exportItem = remember(exportText) {
                DropdownItem(
                    text = exportText,
                    icon = { modifier ->
                        Icon(
                            imageVector = Icons.Rounded.Download,
                            contentDescription = null,
                            modifier = modifier.size(DrawerMetrics.ActionIconSize),
                        )
                    },
                )
            }
            val deleteItem = remember(deleteText) {
                DropdownItem(
                    text = deleteText,
                    icon = { modifier ->
                        Icon(
                            imageVector = Icons.Rounded.Delete,
                            contentDescription = null,
                            modifier = modifier.size(DrawerMetrics.ActionIconSize),
                            tint = MiuixTheme.colorScheme.error,
                        )
                    },
                )
            }
            val deleteColors = DropdownDefaults.dropdownColors(
                contentColor = MiuixTheme.colorScheme.error,
                selectedContentColor = MiuixTheme.colorScheme.error,
                selectedIndicatorColor = MiuixTheme.colorScheme.error,
            )
            ListPopupColumn {
                DropdownImpl(
                    item = renameItem,
                    optionSize = 3,
                    isSelected = false,
                    index = 0,
                    onSelectedIndexChange = {
                        showActionMenu = false
                        onRename()
                    },
                )
                DropdownImpl(
                    item = exportItem,
                    optionSize = 3,
                    isSelected = false,
                    index = 1,
                    onSelectedIndexChange = {
                        showActionMenu = false
                        onExport()
                    },
                )
                DropdownImpl(
                    item = deleteItem,
                    optionSize = 3,
                    isSelected = false,
                    index = 2,
                    dropdownColors = deleteColors,
                    onSelectedIndexChange = {
                        showActionMenu = false
                        onDelete()
                    },
                )
            }
        }
    }
}

@Composable
private fun ConversationActionDialog(
    conversationTitle: String,
    onDismissRequest: () -> Unit,
    onDelete: () -> Unit,
    onArchive: () -> Unit,
) {
    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MiuixTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "会话操作",
                    style = MiuixTheme.textStyles.title4,
                    fontWeight = FontWeight.Bold,
                    color = MiuixTheme.colorScheme.onSurface,
                )
                Text(
                    text = conversationTitle.ifBlank { "当前会话" },
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "请选择对该会话的操作：",
                    style = MiuixTheme.textStyles.footnote1,
                    color = MiuixTheme.colorScheme.onSurfaceVariantActions,
                )

                Spacer(modifier = Modifier.height(2.dp))

                // 归档按钮
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MiuixTheme.colorScheme.surfaceContainerHigh)
                        .clickable {
                            onDismissRequest()
                            onArchive()
                        }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Inventory2,
                        contentDescription = null,
                        tint = MiuixTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "归档",
                            style = MiuixTheme.textStyles.body1,
                            fontWeight = FontWeight.SemiBold,
                            color = MiuixTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = "移至项目归档箱，随时可在归档列表中查看与恢复",
                            style = MiuixTheme.textStyles.footnote2,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        )
                    }
                }

                // 删除按钮
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MiuixTheme.colorScheme.surfaceContainerHigh)
                        .clickable {
                            onDismissRequest()
                            onDelete()
                        }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Delete,
                        contentDescription = null,
                        tint = MiuixTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "删除",
                            style = MiuixTheme.textStyles.body1,
                            fontWeight = FontWeight.SemiBold,
                            color = MiuixTheme.colorScheme.error,
                        )
                        Text(
                            text = "仅清理会话记录，绝对保留项目物理文件",
                            style = MiuixTheme.textStyles.footnote2,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        )
                    }
                }

                // 取消按钮
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(onClick = onDismissRequest)
                        .padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = "取消",
                        style = MiuixTheme.textStyles.body1,
                        color = MiuixTheme.colorScheme.onSurfaceVariantActions,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }
    }
}

@Composable
private fun ArchivedConversationsDialog(
    projectName: String,
    conversations: List<ConversationSummaryUi>,
    onDismissRequest: () -> Unit,
    onSelectConversation: (String) -> Unit,
    onUnarchive: (String) -> Unit,
    onDelete: (String) -> Unit,
) {
    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MiuixTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth().heightIn(max = 500.dp).padding(horizontal = 16.dp),
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Inventory2,
                        contentDescription = null,
                        tint = MiuixTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "归档箱 · $projectName",
                        style = MiuixTheme.textStyles.title4,
                        fontWeight = FontWeight.Bold,
                        color = MiuixTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onDismissRequest),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "关闭",
                            modifier = Modifier.size(18.dp),
                            tint = MiuixTheme.colorScheme.onSurfaceVariantActions,
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))

                if (conversations.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "该项目暂无已归档会话",
                            style = MiuixTheme.textStyles.footnote1,
                            color = MiuixTheme.colorScheme.onSurfaceVariantActions,
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().weight(1f, fill = false),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(conversations, key = { it.id }) { conv ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MiuixTheme.colorScheme.surfaceContainerHigh)
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            onDismissRequest()
                                            onSelectConversation(conv.id)
                                        },
                                ) {
                                    Text(
                                        text = conv.title.ifBlank { conv.preview },
                                        style = MiuixTheme.textStyles.body2,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MiuixTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    if (conv.timeLabel.isNotBlank()) {
                                        Text(
                                            text = conv.timeLabel,
                                            style = MiuixTheme.textStyles.footnote2,
                                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                // 恢复归档按钮
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(MiuixTheme.colorScheme.primary.copy(alpha = 0.12f))
                                        .clickable { onUnarchive(conv.id) }
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                ) {
                                    Text(
                                        text = "恢复",
                                        style = MiuixTheme.textStyles.footnote2,
                                        color = MiuixTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Medium,
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                // 彻底删除按钮
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .clickable { onDelete(conv.id) }
                                        .padding(4.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Delete,
                                        contentDescription = "删除",
                                        tint = MiuixTheme.colorScheme.error,
                                        modifier = Modifier.size(16.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyConversations(isSearching: Boolean) {
    Text(
        text = stringResource(
            if (isSearching) R.string.conversation_no_results else R.string.conversation_empty,
        ),
        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        style = MiuixTheme.textStyles.body2,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(
            horizontal = DrawerMetrics.RowHorizontalPadding,
            vertical = DrawerMetrics.EmptyVerticalPadding,
        ),
    )
}

@Composable
private fun PaneDock(
    onOpenSettings: () -> Unit,
    onOpenModelProviders: () -> Unit,
    onOpenTools: () -> Unit,
    onOpenSkills: () -> Unit,
    onOpenTerminal: () -> Unit,
    onOpenPermissions: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DockEntry(
            icon = Icons.Rounded.Settings,
            label = "设置",
            onClick = onOpenSettings,
            modifier = Modifier.weight(1f),
        )
        DockEntry(
            icon = Icons.Rounded.Inventory2,
            label = "工具",
            onClick = onOpenTools,
            modifier = Modifier.weight(1f),
        )
        DockEntry(
            icon = Icons.Rounded.AutoAwesome,
            label = "Skills",
            onClick = onOpenSkills,
            modifier = Modifier.weight(1f),
        )
        DockEntry(
            icon = LucideIcons.Terminal,
            label = "终端",
            onClick = onOpenTerminal,
            modifier = Modifier.weight(1f),
        )
        DockEntry(
            icon = LucideIcons.Shield,
            label = "权限",
            onClick = onOpenPermissions,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun DockEntry(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = CortexTheme.palette
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(DrawerMetrics.DockEntryCornerRadius))
            .clickable(onClickLabel = label, onClick = onClick)
            .padding(vertical = DrawerMetrics.DockEntryVerticalPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(DrawerMetrics.DockEntryIconSize),
            tint = palette.textSecondary,
        )
        Spacer(modifier = Modifier.height(DrawerMetrics.DockEntryLabelGap))
        Text(
            text = label,
            style = MiuixTheme.textStyles.footnote1,
            color = palette.textTertiary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
