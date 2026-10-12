package com.cortex.ai.ui.screens.terminal

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.cortex.ai.R
import com.cortex.ai.agent.terminal.TerminalEnvironment
import com.cortex.ai.agent.terminal.TerminalScreenBuffer
import com.cortex.ai.ui.app.ConsoleStore
import com.cortex.ai.ui.app.ConsoleUiState
import com.cortex.ai.ui.app.UserTerminalStore
import com.cortex.ai.ui.components.toSpanStyle
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 终端入口：块式终端为本体；PTY 可用时状态栏提供控制台模式切换。 */
@Composable
internal fun TerminalEntryScreen(
    terminalStore: UserTerminalStore,
    consoleStore: ConsoleStore,
    onOpenEnvironment: () -> Unit,
) {
    val consoleState by consoleStore.uiState.collectAsState()
    var consoleMode by rememberSaveable { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        terminalStore.refreshLinuxReady()
        consoleStore.refreshLinuxEnvironment()
        consoleStore.probePtySupport()
    }
    when {
        consoleMode && consoleState.ptySupported != false -> ConsoleScreen(
            store = consoleStore,
            terminalStore = terminalStore,
            onExitConsole = { consoleMode = false },
            onOpenEnvironment = onOpenEnvironment,
        )
        else -> UserTerminalScreen(
            store = terminalStore,
            onOpenEnvironment = onOpenEnvironment,
            onOpenConsole = if (consoleState.ptySupported != false) {
                { consoleMode = true }
            } else {
                null
            },
        )
    }
}

/**
 * 控制台：PTY 全屏视图，面向 TUI 与交互式 CLI。
 * 网格渲染按行复用（Line.id + version）；软键盘经隐藏输入框捕获，特殊键由键条补齐。
 */
@Composable
internal fun ConsoleScreen(
    store: ConsoleStore,
    terminalStore: UserTerminalStore,
    onExitConsole: () -> Unit,
    onOpenEnvironment: () -> Unit,
) {
    val state by store.uiState.collectAsState()
    val terminalState by terminalStore.uiState.collectAsState()
    val keyboardController = LocalSoftwareKeyboardController.current
    val clipboardManager = LocalClipboardManager.current
    var showTasks by remember { mutableStateOf(false) }
    var showSessions by remember { mutableStateOf(false) }
    var ctrlActive by remember { mutableStateOf(false) }
    var fieldValue by remember { mutableStateOf(TextFieldValue("")) }
    val focusRequester = remember { FocusRequester() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MiuixTheme.colorScheme.surface)
            .imePadding()
            .navigationBarsPadding(),
    ) {
        ConsoleStatusBar(
            environment = state.environment,
            linuxEnvironment = state.linuxEnvironment,
            onSwitchEnvironment = store::switchEnvironment,
            onOpenTasks = {
                terminalStore.refreshDaemonTasks()
                showTasks = true
            },
            onOpenSessions = { showSessions = true },
            onExitConsole = onExitConsole,
        )
        ConsoleGrid(
            state = state,
            store = store,
            focusRequester = focusRequester,
            fieldValue = fieldValue,
            onFieldChange = { newValue ->
                val inserted = diffInserted(fieldValue.text, newValue.text)
                if (inserted.isNotEmpty()) {
                    val bytes = if (ctrlActive && inserted.length == 1 && inserted[0].isLetter()) {
                        ctrlActive = false
                        (inserted[0].lowercaseChar().code and 0x1F).toChar().toString()
                    } else {
                        inserted
                    }
                    store.write(bytes.replace("\n", "\r"))
                }
                val deleted = diffDeleted(fieldValue.text, newValue.text)
                repeat(deleted) { store.write("\u007F") }
                fieldValue = if (newValue.text.length > 64) TextFieldValue("") else newValue
            },
            onOpenEnvironment = onOpenEnvironment,
        )
        ConsoleKeyBar(
            ctrlActive = ctrlActive,
            onCtrl = { ctrlActive = !ctrlActive },
            onKey = { store.write(it) },
            onPaste = {
                val clip = clipboardManager.getText()?.text
                if (!clip.isNullOrEmpty()) {
                    store.write(clip.replace("\n", "\r"))
                }
            },
            onToggleKeyboard = {
                focusRequester.requestFocus()
                keyboardController?.show()
            },
        )
    }

    if (showTasks) {
        DaemonTasksDialog(
            tasks = terminalState.daemonTasks,
            onDismiss = { showTasks = false },
            onStop = terminalStore::stopDaemonTask,
            onLoadLogs = terminalStore::daemonLogs,
        )
    }

    if (showSessions) {
        SessionListDialog(
            rows = state.sessions.map { session ->
                SessionDialogRow(
                    id = session.id,
                    environment = session.environment,
                    subtitle = "",
                    active = session.id == state.activeSessionId,
                    running = !session.exited,
                    alive = !session.exited,
                )
            },
            onDismiss = { showSessions = false },
            onSelect = store::switchSession,
            onRestart = store::restartSession,
            onClose = store::closeSession,
            onNew = store::newSession,
        )
    }
}

@Composable
private fun ConsoleStatusBar(
    environment: TerminalEnvironment,
    linuxEnvironment: TerminalEnvironment,
    onSwitchEnvironment: (TerminalEnvironment) -> Unit,
    onOpenTasks: () -> Unit,
    onOpenSessions: () -> Unit,
    onExitConsole: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        EnvironmentTab(
            label = "Android",
            selected = environment == TerminalEnvironment.ANDROID,
            onClick = { onSwitchEnvironment(TerminalEnvironment.ANDROID) },
        )
        EnvironmentTab(
            label = if (linuxEnvironment == TerminalEnvironment.ALPINE) "Alpine" else "Debian",
            selected = environment == linuxEnvironment,
            onClick = { onSwitchEnvironment(linuxEnvironment) },
        )
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onOpenSessions) {
                Icon(
                    imageVector = Icons.Rounded.Layers,
                    contentDescription = stringResource(R.string.terminal_sessions),
                    modifier = Modifier.size(18.dp),
                    tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
            }
            IconButton(onClick = onOpenTasks) {
                Icon(
                    imageVector = Icons.Rounded.Insights,
                    contentDescription = stringResource(R.string.terminal_daemon_tasks),
                    modifier = Modifier.size(18.dp),
                    tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
            }
            TextButton(
                text = stringResource(R.string.terminal_block_mode),
                onClick = onExitConsole,
            )
        }
    }
}

@Composable
private fun ColumnScope.ConsoleGrid(
    state: ConsoleUiState,
    store: ConsoleStore,
    focusRequester: FocusRequester,
    fieldValue: TextFieldValue,
    onFieldChange: (TextFieldValue) -> Unit,
    onOpenEnvironment: () -> Unit,
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()
    val cellStyle = MiuixTheme.textStyles.footnote1.copy(fontFamily = FontFamily.Monospace)
    val defaultColor = MiuixTheme.colorScheme.onSurface
    val cursorColor = MiuixTheme.colorScheme.onSurface
    val cursorTextColor = MiuixTheme.colorScheme.surface

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
            .clickable(
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null,
            ) {
                focusRequester.requestFocus()
                keyboardController?.show()
            },
    ) {
        val cellSize = remember(cellStyle) { measureCell(textMeasurer, cellStyle) }
        val cols = with(density) { (maxWidth.toPx() / cellSize.first).toInt() }
        val rows = with(density) { (maxHeight.toPx() / cellSize.second).toInt() }
        if (cols > 0 && rows > 0) {
            LaunchedEffect(cols, rows) {
                store.open(state.environment, cols, rows)
            }
        }

        val frame = state.frame
        val listState = rememberLazyListState()
        val atBottom by remember {
            derivedStateOf {
                val layoutInfo = listState.layoutInfo
                val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: Int.MAX_VALUE
                lastVisible >= layoutInfo.totalItemsCount - 1
            }
        }
        LaunchedEffect(frame) {
            if (atBottom && frame.lines.isNotEmpty()) {
                listState.scrollToItem(frame.lines.lastIndex)
            }
        }
        val cursorLineIndex = frame.lines.size - frame.screenRows + frame.cursorRow
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 8.dp),
        ) {
            itemsIndexed(
                items = frame.lines,
                key = { _, line -> line.id },
                contentType = { _, _ -> "console-line" },
            ) { index, line ->
                val cursorCol = if (state.connected && frame.cursorVisible && index == cursorLineIndex) {
                    frame.cursorCol
                } else {
                    null
                }
                ConsoleLine(
                    line = line,
                    lineVersion = line.version,
                    cursorCol = cursorCol,
                    defaultColor = defaultColor,
                    cursorColor = cursorColor,
                    cursorTextColor = cursorTextColor,
                    style = cellStyle,
                )
            }
        }

        BasicTextField(
            value = fieldValue,
            onValueChange = onFieldChange,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.None),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .alpha(0.001f)
                .focusRequester(focusRequester),
        )
        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
            keyboardController?.show()
        }

        if (state.exited || state.failMessage != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MiuixTheme.colorScheme.surface.copy(alpha = 0.92f)),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = state.failMessage ?: stringResource(R.string.terminal_session_closed),
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    textAlign = TextAlign.Center,
                )
                TextButton(
                    text = stringResource(R.string.terminal_reconnect),
                    onClick = store::reconnect,
                    modifier = Modifier.padding(top = 12.dp),
                )
                if (state.failMessage != null) {
                    TextButton(
                        text = stringResource(R.string.terminal_open_environment),
                        onClick = onOpenEnvironment,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ConsoleLine(
    line: TerminalScreenBuffer.Line,
    lineVersion: Long,
    cursorCol: Int?,
    defaultColor: Color,
    cursorColor: Color,
    cursorTextColor: Color,
    style: TextStyle,
) {
    val text = remember(line.id, lineVersion, cursorCol, defaultColor) {
        lineToAnnotated(line, cursorCol, cursorColor, cursorTextColor)
    }
    Text(
        text = text,
        style = style.copy(color = defaultColor),
        softWrap = false,
        maxLines = 1,
    )
}

/** 网格行 → AnnotatedString：连续同样式单元格合并为一个 span；光标格反色。 */
private fun lineToAnnotated(
    line: TerminalScreenBuffer.Line,
    cursorCol: Int?,
    cursorColor: Color,
    cursorTextColor: Color,
): AnnotatedString {
    val cells = line.snapshot()
    return buildAnnotatedString {
        var i = 0
        while (i < cells.size) {
            val cell = cells[i]
            if (cell.continuation) {
                i++
                continue
            }
            val isCursor = i == cursorCol
            var j = i + 1
            while (j < cells.size &&
                !cells[j].continuation &&
                cells[j].style == cell.style &&
                (j == cursorCol) == isCursor
            ) {
                j++
            }
            val span: SpanStyle = if (isCursor) {
                SpanStyle(background = cursorColor, color = cursorTextColor)
            } else {
                cell.style.toSpanStyle()
            }
            if (isCursor || !cell.style.isPlain) {
                withStyle(span) {
                    for (k in i until j) if (!cells[k].continuation) append(cells[k].text)
                }
            } else {
                for (k in i until j) if (!cells[k].continuation) append(cells[k].text)
            }
            i = j
        }
    }
}

@Composable
private fun ConsoleKeyBar(
    ctrlActive: Boolean,
    onCtrl: () -> Unit,
    onKey: (String) -> Unit,
    onPaste: () -> Unit,
    onToggleKeyboard: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp)
            .padding(bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        KeyChip(label = "粘贴", onClick = onPaste)
        KeyChip(label = "回车", onClick = { onKey("\r") })
        KeyChip(label = "键盘", onClick = onToggleKeyboard)
        KeyChip(label = "Esc", onClick = { onKey("\u001B") })
        KeyChip(
            label = "Ctrl",
            active = ctrlActive,
            onClick = onCtrl,
        )
        KeyChip(label = "Tab", onClick = { onKey("\t") })
        KeyChip(label = "←", onClick = { onKey("\u001B[D") })
        KeyChip(label = "↑", onClick = { onKey("\u001B[A") })
        KeyChip(label = "↓", onClick = { onKey("\u001B[B") })
        KeyChip(label = "→", onClick = { onKey("\u001B[C") })
    }
}

@Composable
private fun KeyChip(
    label: String,
    modifier: Modifier = Modifier,
    active: Boolean = false,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .background(
                if (active) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.surfaceVariant,
                RoundedCornerShape(8.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MiuixTheme.textStyles.footnote1,
            color = if (active) {
                MiuixTheme.colorScheme.onPrimary
            } else {
                MiuixTheme.colorScheme.onSurfaceVariantSummary
            },
            fontWeight = if (active) FontWeight.SemiBold else null,
        )
    }
}

@Composable
private fun EnvironmentTab(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Text(
        text = label,
        style = MiuixTheme.textStyles.footnote1,
        color = if (selected) {
            MiuixTheme.colorScheme.primary
        } else {
            MiuixTheme.colorScheme.onSurfaceVariantSummary
        },
        fontWeight = if (selected) FontWeight.SemiBold else null,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
    )
}

private fun measureCell(textMeasurer: TextMeasurer, style: TextStyle): Pair<Int, Int> {
    val result = textMeasurer.measure("M", style)
    return result.size.width to result.size.height
}

/** 提取新插入的文本；非追加式变化（输入法重组）返回空，由调用方维持现状。 */
private fun diffInserted(old: String, new: String): String =
    if (new.length > old.length && new.startsWith(old)) new.substring(old.length) else ""

/** 提取删除的字符数（仅末尾删除）。 */
private fun diffDeleted(old: String, new: String): Int =
    if (new.length < old.length && old.startsWith(new)) old.length - new.length else 0
