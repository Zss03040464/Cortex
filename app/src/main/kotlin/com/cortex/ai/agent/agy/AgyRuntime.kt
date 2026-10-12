package com.cortex.ai.agent.agy

import android.content.Context
import com.cortex.ai.agent.model.AgentModelClient
import com.cortex.ai.agent.runtime.AgentEvent
import com.cortex.ai.agent.runtime.AgentRuntimeSession
import com.cortex.ai.agent.runtime.AgentRuntimeWire
import com.cortex.ai.core.AndroidAgentLogger
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import org.json.JSONArray
import org.json.JSONObject

internal object AgyRuntime {
    private val lock = Any()
    private var mcpServer: CortexMcpServer? = null
    private val sessions = ConcurrentHashMap<String, AgyStreamSession>()

    private const val CHROOT_RUNNER = "/data/local/tmp/run_chroot.sh"
    private const val AGY_WRAPPER = "/usr/local/bin/agy-wrapper"
    private val MCP_CONFIG_FILE = File("/data/data/com.cortex.ai/files/terminal/debian/rootfs/root/.gemini/config/mcp_config.json")
    private val AGENTS_RULES_FILE = File("/data/data/com.cortex.ai/files/terminal/debian/rootfs/root/.gemini/config/AGENTS.md")

    internal val AGENT_RULES_CONTENT = """
        # 全局 Agent 工程规范与执行协议

        ## 一、 执行过程透明度（强制交替步进）
        严禁在消息正文中留空并连续堆叠工具调用（严禁出现整屏无上下文的灰色工具条）。
        复杂或多步骤任务必须严格按照“判断 -> 动作 -> 事实 -> 下一步”交替执行：
        1. **调用前输出意图**：发起关键命令或文件编辑前，必须在正文中输出 1~2 句客观分析（当前技术判断、为什么做、准备做什么）。
        2. **调用后输出事实**：工具执行完毕后，必须在正文中提炼命令/代码返回的真实事实与根因结论。
        3. **推进下一步**：基于事实阐明后续动作，再触发下一个工具。
        4. **异常主动同步**：界面闪烁、重启、断联、配置变化等副作用须主动说明原因、影响及补救。确需用户配合时只提出明确、单点的问题。

        ## 二、 工程师独立判断与对等协作（杜绝盲从）
        1. **事实优先于用户假设**：对用户提出的排查方向或方案，必须先进行客观的技术可行性与逻辑校验。若用户的假设存在技术事实错误、冲突或会导致系统损坏，必须先客观指出问题，不得顺水推舟盲从执行。
        2. **零情绪与客观语态**：保持中立工程师视角，专注于完成任务。不考虑用户情绪，不为用户留虚假面子，严禁使用任何谄媚、迎合或低信息密度的客套用语。
        3. **严禁空口绝对化保证**：禁止使用“百分百”、“绝对没有残留”等未经验证的断言。所有结论必须以实际命令输出、状态码或目录检查日志为证据。

        ## 三、 出错处理纪律（严禁表演性道歉）
        1. **禁止道歉与反省小作文**：无论执行失败还是被指出错误，严禁说“对不起”、“向您致歉”、“这是我的失误”等情绪安抚废话，严禁编造看似深刻的心理反省。
        2. **纯事实闭环**：发生错误或违规时，仅允许输出以下三项：
           - 违规/故障的客观事实（涉及的具体文件、路径、报错）；
           - 导致该问题的真实技术因果；
           - 实际修复措施与验收结果。

        ## 四、 文件生命周期与严格物理边界
        1. **严禁在项目外散落文件**：所有临时脚本、中间测试文件必须仅在明确允许的临时或项目目录中创建。
        2. **用后即清与物理证据**：任务交付前必须主动执行目录检查命令（如检查未跟踪文件），确认无残留后输出命令证据，严禁在未检查的情况下口头宣称已清理。保持项目根目录纯净，长期文件按实际用途渐进归档。

        ## 五、 端到端自主闭环与真实验收
        1. **可证必证（事实优先于经验）**：凡是可以通过运行命令、查看代码、探测设备（手机/PC/环境）、调阅日志、网络检索或查阅官方文档求证的事项，严禁仅凭模型先验固化知识直接给出确定性结论。遇到现状询问、功能答疑或故障诊断，默认视为现场探查任务，必须优先调用工具获取第一手真实证据；仅在完全无验证条件时才允许基于先验知识推测，且必须显式声明为“未经实测验证的经验推测”，严禁把假设包装为定论。
        2. **自主推进**：具备工具与权限的任务默认由 Agent 独立执行到底；异常自行定位处理，不得将本可自行完成的步骤反抛用户。用户明确要求仅提供代码/命令/教程，或必须本人完成登录、授权、物理操作时除外。
        3. **后台防挂死**：派发后台任务或命令并等待异步唤醒时，严禁无限期被动干等；必须设定合理超时或检查机制（如定时器或定期存活核验），主动监测进程是否卡死、假死或无输出，及时介入与止损。
        4. **真实验收**：执行结束不等于完成。必须按任务实际成功条件核验；失败则自行修复并重新验收，直至通过。服务至少核验进程、端口、关键日志及实际访问；GUI 如需操作，须实际打开并验证核心交互。未通过不得宣称完成。

        ## 六、 手机本机环境与 GUI/设备操作（移动端适配特化）
        1. **宿主执行环境与本地回路**：当前 Agent 运行在手机宿主系统的 Debian Linux 容器环境（Root 权限），具备本机回环 ADB（127.0.0.1:5555）与系统级命令能力。
        2. **物理主屏零干扰底线**：物理主屏（Display 0）属于用户日常操作空间，默认假定用户正在使用手机，严禁任何抢占前台、夺取软键盘输入法焦点、黑屏或闪烁行为。
        3. **分级决策阶梯（无界面后台/API → Mirage 虚拟副屏 → 物理主屏）**：
           - 凡能通过后台 ADB、Linux 命令、系统广播/Intent、文件/数据库修改完成的任务，强制纯后台执行，严禁启动任何 GUI 界面；
           - 凡需点击、滑动、输入、截图核验的 Android GUI 交互任务，强制在 ColorOS Mirage 幻镜虚拟副屏（PC模式，slot 1/2）中隔离执行；
           - 严格遵循 `android` Skill 规范：严禁使用 `settings put global overlay_display_devices`（避免产生画中画浮窗与键盘抢占），严禁使用原生裸 `scrcpy --new-display`（避免触发黑屏或主屏死区），严禁暴力 `am force-stop`，启动应用必须使用十六进制 Flag `-f 0x18000000`，操作完毕必须显式销毁副屏释放资源；
           - 只有当任务必须作用于物理主屏且用户明确知晓或要求时，方可调度物理主屏。
        4. **GUI 操作与搜索类任务完成准则**：
           - 输入文字 ≠ 任务完成，必须显式提交（点击搜索/发送/确定按钮）并使用 observe_screen 重新观察屏幕核验结果；未点击提交按钮的任务 100% 未完成；
           - 界面交互动作统一通过 tap_element、virtual_display 等规范接口驱动；
           - 严禁连续 0ms 瞬间点击（Anti-0ms Tap），使用 50ms~120ms 自然微延迟与微位移模拟手势触控；
           - 绝对禁止执行任何形式的重启（reboot / shutdown），保护系统持续可用性。

        ## 七、 多模态图片与文件附件交互规范
        1. **附件识别**：当用户请求中包含 `# Images attached by the user:` 或 `# Files mentioned by the user:` 时，表明用户附带了本地图片或文件。
        2. **多模态图文解析**：对于用户附带的图片（如 `/sdcard/Android/data/com.cortex.ai/files/images/...`），必须优先使用 `view_file` 查看分析图片内容，结合用户意图进行准确理解与回答。
        3. **文件与路径处理**：对于附带的文件或目录路径，直接基于本地文件系统（`/sdcard/...` 或项目根目录）读取分析，严禁声称无法访问本地文件。
    """.trimIndent()

    fun execute(
        context: Context? = null,
        session: AgentRuntimeSession,
        request: AgentRuntimeWire.RunRequest,
        runTools: JSONArray,
        toolExecutor: AgentModelClient.ToolExecutor,
        onEvent: (AgentEvent) -> Unit,
    ): AgentModelClient.ModelResponse.Text {
        AgyBridgePolicy.requireSupported(request.operation, request.images.size)
        val sessionKey = AgyBridgePolicy.sessionKey(request)

        val server = synchronized(lock) {
            mcpServer?.takeUnless { false } ?: CortexMcpServer().also { mcpServer = it }
        }

        writeMcpConfig(server.endpoint)
        writeAgentRules()

        onEvent(
            AgentEvent.RunStarted(
                initialImages = request.images.size,
                initialImageBytes = request.images.sumOf { it.bytes },
                toolCount = runTools.length(),
                terminalTools = request.config.terminalTools,
            )
        )

        return server.bind(sessionKey, runTools) { call ->
            onEvent(
                AgentEvent.ToolStarted(
                    round = 1,
                    toolCallId = call.id,
                    name = call.name,
                    argsPreview = call.argumentsJson.take(200),
                )
            )
            val res = toolExecutor.execute(call)
            onEvent(
                AgentEvent.ToolFinished(
                    round = 1,
                    toolCallId = call.id,
                    name = call.name,
                    resultSummary = res.content.take(200),
                    imageCount = res.images.size,
                    imageBytes = res.images.sumOf { it.bytes },
                    success = !res.content.startsWith("{\"error\":"),
                )
            )
            res
        }.use {
            val agySession = sessions.compute(sessionKey) { _, existing ->
                if (existing != null) existing
                else {
                    val projId = AgyProjectRepository.getProjectForConversation(sessionKey) ?: AgyProjectRepository.activeProjectId
                    val proj = AgyProjectRepository.getProject(projId)
                    val workspaceHost = proj?.workspacePath ?: AgyProjectRepository.DEFAULT_WORKSPACE_PATH
                    val workspaceInChroot = AgyProjectRepository.toChrootPath(workspaceHost)

                    val chrootCmd = StringBuilder()
                    if (workspaceInChroot.isNotBlank()) {
                        chrootCmd.append("export AGY_WORKSPACE=\"$workspaceInChroot\" && cd \"$workspaceInChroot\" && ")
                    }
                    chrootCmd.append("exec $AGY_WRAPPER --input-format stream-json --output-format stream-json --dangerously-skip-permissions")
                    val model = if (request.config.model.isNotBlank() && request.config.model != "agy") {
                        request.config.model
                    } else {
                        "gemini-3.8-flash-high"
                    }
                    chrootCmd.append(" --model $model")
                    request.config.reasoningEffort?.wireValue?.takeIf { it in setOf("low", "medium", "high") }?.let { effort ->
                        chrootCmd.append(" --effort $effort")
                    }
                    if (workspaceInChroot.isNotBlank()) {
                        chrootCmd.append(" --add-dir \"$workspaceInChroot\"")
                    }
                    if (AgyProjectRepository.hasConversationInDb(sessionKey)) {
                        chrootCmd.append(" --conversation $sessionKey")
                    }

                    val cmd = listOf(
                        "su",
                        "-c",
                        "$CHROOT_RUNNER sh -c '${chrootCmd.toString().replace("'", "'\\''")}'"
                    )
                    AgyStreamSession(
                        startProcess = { ProcessBuilder(cmd).start() },
                        stopProcess = { it.destroy() },
                    )
                }
            }!!

            session.controller.register {
                agySession.cancel()
                sessions.remove(sessionKey)
            }

            var currentTextBlockIndex: Int? = null
            var thinkingBlockStarted = true
            var activeConvId: String? = agySession.conversationId
            val currentBlockTextBuilder = StringBuilder()
            val allTurnTextBuilder = StringBuilder()
            val reasoningBuilder = StringBuilder()

            // 任务启动伊始即派发 Thinking 状态，向用户提供即时思考反馈
            thinkingBlockStarted = true
            onEvent(
                AgentEvent.AssistantBlockStart(
                    round = 1,
                    kind = AgentEvent.AssistantBlockKind.THINKING,
                    index = 0,
                )
            )
            val initialThinking = "Thinking..."
            reasoningBuilder.append(initialThinking)
            onEvent(
                AgentEvent.AssistantBlockDelta(
                    round = 1,
                    kind = AgentEvent.AssistantBlockKind.THINKING,
                    index = 0,
                    deltaChars = initialThinking.length,
                    delta = initialThinking,
                )
            )

            fun refreshThinkingFromTranscript(convId: String?) {
                val targetId = convId?.ifBlank { null } ?: activeConvId ?: return
                val realThinking = readLatestThinking(targetId)
                if (!realThinking.isNullOrBlank() && realThinking != reasoningBuilder.toString()) {
                    reasoningBuilder.clear()
                    reasoningBuilder.append(realThinking)
                    onEvent(
                        AgentEvent.AssistantBlockEnd(
                            round = 1,
                            kind = AgentEvent.AssistantBlockKind.THINKING,
                            index = 0,
                            contentChars = realThinking.length,
                            replacementContent = realThinking,
                        )
                    )
                }
            }

            fun closeThinkingIfRunning(convId: String?) {
                if (thinkingBlockStarted) {
                    val targetId = convId?.ifBlank { null } ?: activeConvId
                    val realThinking = readLatestThinking(targetId)
                    if (!realThinking.isNullOrBlank()) {
                        reasoningBuilder.clear()
                        reasoningBuilder.append(realThinking)
                    }
                    onEvent(
                        AgentEvent.AssistantBlockEnd(
                            round = 1,
                            kind = AgentEvent.AssistantBlockKind.THINKING,
                            index = 0,
                            contentChars = reasoningBuilder.length,
                            replacementContent = realThinking?.takeIf { it.isNotBlank() },
                        )
                    )
                    thinkingBlockStarted = false
                }
            }

            fun closeCurrentTextBlockIfOpen() {
                val idx = currentTextBlockIndex ?: return
                onEvent(
                    AgentEvent.AssistantBlockEnd(
                        round = 1,
                        kind = AgentEvent.AssistantBlockKind.TEXT,
                        index = idx,
                        contentChars = currentBlockTextBuilder.length,
                    )
                )
                currentTextBlockIndex = null
                currentBlockTextBuilder.clear()
            }

            val resultPayload = try {
                agySession.ask(request.prompt, timeoutMillis = 3_600_000L, idleTimeoutMillis = 180_000L) { eventJson ->
                    val eventType = eventJson.optString("event")
                    if (eventType == "init") {
                        val initObj = eventJson.optJSONObject("init") ?: eventJson
                        val convId = eventJson.optString("conversation_id", initObj.optString("conversation_id"))
                        if (convId.isNotBlank()) {
                            activeConvId = convId
                            server.registerAlias(convId, sessionKey)
                            val currentProj = AgyProjectRepository.getProjectForConversation(sessionKey) ?: AgyProjectRepository.activeProjectId
                            AgyProjectRepository.bindConversationProject(convId, currentProj)
                        }
                    }
                    if (eventType == "step_update") {
                        val stepUpdate = eventJson.optJSONObject("step_update") ?: JSONObject()
                        val stepType = stepUpdate.optString("step_type")
                        val stepIndex = stepUpdate.optInt("step_index", -1)
                        val stepState = stepUpdate.optString("state")
                        val convId = stepUpdate.optString("conversation_id").ifBlank {
                            eventJson.optString("conversation_id")
                        }.ifBlank { agySession.conversationId }
                        if (!convId.isNullOrBlank()) activeConvId = convId

                        // 1. 处理工具调用事件，映射为 HostedToolStarted / HostedToolFinished 胶囊
                        if (stepType == "tool") {
                            closeThinkingIfRunning(activeConvId)
                            closeCurrentTextBlockIfOpen()

                            val rawToolName = stepUpdate.optString("tool_name").ifBlank {
                                stepUpdate.optJSONObject("tool_info")?.optString("name") ?: "tool"
                            }
                            val displayName = formatToolDisplayName(rawToolName)
                            val toolCallId = "tool_$stepIndex"

                            if (stepState == "ACTIVE") {
                                onEvent(
                                    AgentEvent.HostedToolStarted(
                                        round = 1,
                                        toolCallId = toolCallId,
                                        name = displayName,
                                    )
                                )
                            } else if (stepState == "DONE") {
                                onEvent(
                                    AgentEvent.HostedToolFinished(
                                        round = 1,
                                        toolCallId = toolCallId,
                                        name = displayName,
                                        success = true,
                                    )
                                )
                                refreshThinkingFromTranscript(activeConvId)
                            }
                        }

                        // 2. 处理文本流式增量与跨 Step 独立消息块
                        val textDelta = stepUpdate.optString("text_delta")
                        if (textDelta.isNotEmpty()) {
                            closeThinkingIfRunning(activeConvId)

                            val effectiveIndex = if (stepIndex >= 0) stepIndex else 0
                            if (currentTextBlockIndex == null || currentTextBlockIndex != effectiveIndex) {
                                closeCurrentTextBlockIfOpen()
                                currentTextBlockIndex = effectiveIndex
                                onEvent(
                                    AgentEvent.AssistantBlockStart(
                                        round = 1,
                                        kind = AgentEvent.AssistantBlockKind.TEXT,
                                        index = effectiveIndex,
                                    )
                                )
                            }

                            currentBlockTextBuilder.append(textDelta)
                            allTurnTextBuilder.append(textDelta)
                            onEvent(
                                AgentEvent.AssistantBlockDelta(
                                    round = 1,
                                    kind = AgentEvent.AssistantBlockKind.TEXT,
                                    index = effectiveIndex,
                                    deltaChars = textDelta.length,
                                    delta = textDelta,
                                )
                            )
                        } else if (stepState == "DONE") {
                            refreshThinkingFromTranscript(activeConvId)
                        }
                    }
                }
            } catch (e: Exception) {
                sessions.remove(sessionKey)
                throw e
            }

            closeThinkingIfRunning(activeConvId)
            closeCurrentTextBlockIfOpen()
            refreshThinkingFromTranscript(activeConvId)

            val finalContent = allTurnTextBuilder.toString().ifBlank { resultPayload.optString("response") }

            AgentModelClient.ModelResponse.Text(
                content = finalContent.trimEnd(),
                reasoningContent = reasoningBuilder.toString(),
                transcript = listOf(
                    AgentModelClient.ConversationMessage(
                        role = "user",
                        content = request.prompt,
                    ),
                    AgentModelClient.ConversationMessage(
                        role = "assistant",
                        content = finalContent.trimEnd(),
                        reasoningContent = reasoningBuilder.toString(),
                    ),
                ),
            )
        }
    }

    private fun writeMcpConfig(endpoint: String) {
        val currentJson = try {
            if (MCP_CONFIG_FILE.exists()) JSONObject(MCP_CONFIG_FILE.readText(Charsets.UTF_8)) else JSONObject()
        } catch (_: Exception) { JSONObject() }
        val servers = currentJson.optJSONObject("mcpServers") ?: JSONObject()
        servers.put(
            "cortex",
            JSONObject()
                .put("disabled", false)
                .put("serverUrl", endpoint)
        )
        currentJson.put("mcpServers", servers)
        val json = currentJson.toString()
        try {
            MCP_CONFIG_FILE.parentFile?.mkdirs()
            MCP_CONFIG_FILE.writeText(json, Charsets.UTF_8)
        } catch (_: Exception) {
            runCatching {
                val escaped = json.replace("'", "'\\''")
                ProcessBuilder("su", "-c", "echo '$escaped' > \"${MCP_CONFIG_FILE.absolutePath}\"").start().waitFor()
            }
        }
    }

    private fun writeAgentRules() {
        val targets = listOf(
            AGENTS_RULES_FILE,
            File(AGENTS_RULES_FILE.parentFile, "GEMINI.md"),
            File("/data/data/com.cortex.ai/files/terminal/debian/rootfs/root/AGENTS.md"),
            File("/data/data/com.cortex.ai/files/terminal/debian/rootfs/root/.gemini/AGENTS.md"),
        )
        for (file in targets) {
            try {
                file.parentFile?.mkdirs()
                file.writeText(AGENT_RULES_CONTENT, Charsets.UTF_8)
            } catch (_: Exception) {
                runCatching {
                    val escaped = AGENT_RULES_CONTENT.replace("'", "'\\''")
                    ProcessBuilder("su", "-c", "echo '$escaped' > \"${file.absolutePath}\"").start().waitFor()
                }
            }
        }
    }

    fun reset() {
        synchronized(lock) {
            mcpServer?.close()
            mcpServer = null
            sessions.values.forEach { runCatching { it.close() } }
            sessions.clear()
        }
    }

    private fun formatToolDisplayName(rawName: String): String = when (rawName) {
        "run_command" -> "执行命令"
        "view_file" -> "查看文件"
        "replace_file_content", "multi_replace_file_content", "sed_file" -> "编辑文件"
        "write_to_file" -> "写入文件"
        "search_web" -> "网页搜索"
        "read_url_content", "read_browser_page" -> "读取网页"
        "grep_search" -> "代码搜索"
        "find_by_name", "list_dir" -> "浏览目录"
        "ask_question" -> "询问用户"
        else -> rawName
    }

    private fun readLatestThinking(conversationId: String?): String? {
        if (conversationId.isNullOrBlank()) return null
        val file = File("/data/data/com.cortex.ai/files/terminal/debian/rootfs/root/.gemini/antigravity-cli/brain/$conversationId/.system_generated/logs/transcript.jsonl")
        if (!file.exists() || !file.canRead()) return null
        return runCatching {
            val thinkings = mutableListOf<String>()
            file.useLines { lines ->
                for (line in lines) {
                    if (line.isBlank()) continue
                    val obj = JSONObject(line)
                    val t = obj.optString("thinking").trim()
                    if (t.isNotEmpty() && !thinkings.contains(t)) {
                        thinkings.add(t)
                    }
                }
            }
            if (thinkings.isNotEmpty()) thinkings.joinToString("\n\n---\n\n") else null
        }.getOrNull()
    }
}
