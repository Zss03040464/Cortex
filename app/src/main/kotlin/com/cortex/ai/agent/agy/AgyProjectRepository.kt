package com.cortex.ai.agent.agy

import android.database.sqlite.SQLiteDatabase
import com.cortex.ai.ui.model.AgentChatMessageUi
import com.cortex.ai.ui.model.AgentMessageUi
import com.cortex.ai.ui.model.AgyProjectUi
import com.cortex.ai.ui.model.ConversationModeUi
import com.cortex.ai.ui.model.ConversationSummaryUi
import com.cortex.ai.ui.model.ThinkingMessageUi
import com.cortex.ai.ui.model.ToolActivityMessageUi
import com.cortex.ai.ui.model.ToolActivityStatusUi
import com.cortex.ai.ui.model.UserMessageUi
import java.io.File
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

object AgyProjectRepository {
    private const val DEBIAN_ROOTFS = "/data/data/com.cortex.ai/files/terminal/debian/rootfs"
    private val SUMMARIES_DB_FILE = File("$DEBIAN_ROOTFS/root/.gemini/antigravity-cli/conversation_summaries.db")
    private val PROJECTS_DIR = File("$DEBIAN_ROOTFS/root/.gemini/config/projects")
    private val LOCAL_PROJECTS_DIR = File("/data/data/com.cortex.ai/files/agy_projects")
    private val ACCOUNT_INFO_FILE = File("$DEBIAN_ROOTFS/root/.gemini/account_info.json")

    const val DEFAULT_PROJECT_ID = "default-cli-project"
    const val DEFAULT_PROJECT_NAME = "默认工作区"
    const val DEFAULT_WORKSPACE_PATH = "/root"

    fun isDefaultOrNoProjectId(id: String?): Boolean {
        if (id.isNullOrBlank()) return true
        val lower = id.trim().lowercase()
        return lower == DEFAULT_PROJECT_ID ||
            lower == "outside-of-project" ||
            lower == "outside_of_project" ||
            lower == "none" ||
            lower == "default" ||
            lower == "no-project"
    }

    @Volatile
    var activeProjectId: String = DEFAULT_PROJECT_ID

    private val conversationProjectBindings = ConcurrentHashMap<String, String>()
    private val cachedProjects = ConcurrentHashMap<String, AgyProjectUi>()
    private val allLoadedConversations = ConcurrentHashMap<String, ConversationSummaryUi>()
    private val ARCHIVED_CONVERSATIONS_FILE = File("/data/data/com.cortex.ai/files/archived_conversations.json")
    private val archivedConversationIds = ConcurrentHashMap.newKeySet<String>()
    private val DELETED_CONVERSATIONS_FILE = File("/data/data/com.cortex.ai/files/deleted_conversations.json")
    private val deletedConversationIds = ConcurrentHashMap.newKeySet<String>()

    init {
        loadArchivedConversationsFromFile()
        loadDeletedConversationsFromFile()
    }

    private fun loadArchivedConversationsFromFile() {
        if (!ARCHIVED_CONVERSATIONS_FILE.exists() || !ARCHIVED_CONVERSATIONS_FILE.canRead()) return
        runCatching {
            val json = JSONObject(ARCHIVED_CONVERSATIONS_FILE.readText())
            val keys = json.keys()
            while (keys.hasNext()) {
                archivedConversationIds.add(keys.next())
            }
        }
    }

    private fun saveArchivedConversationsToFile() {
        runCatching {
            ARCHIVED_CONVERSATIONS_FILE.parentFile?.mkdirs()
            val json = JSONObject()
            archivedConversationIds.forEach { id ->
                json.put(id, true)
            }
            ARCHIVED_CONVERSATIONS_FILE.writeText(json.toString())
        }
    }

    private fun loadDeletedConversationsFromFile() {
        if (!DELETED_CONVERSATIONS_FILE.exists() || !DELETED_CONVERSATIONS_FILE.canRead()) return
        runCatching {
            val json = JSONObject(DELETED_CONVERSATIONS_FILE.readText())
            val keys = json.keys()
            while (keys.hasNext()) {
                deletedConversationIds.add(keys.next())
            }
        }
    }

    private fun saveDeletedConversationsToFile() {
        runCatching {
            DELETED_CONVERSATIONS_FILE.parentFile?.mkdirs()
            val json = JSONObject()
            deletedConversationIds.forEach { id ->
                json.put(id, true)
            }
            DELETED_CONVERSATIONS_FILE.writeText(json.toString())
        }
    }

    fun isArchived(conversationId: String): Boolean = archivedConversationIds.contains(conversationId)

    fun isDeleted(conversationId: String): Boolean = deletedConversationIds.contains(conversationId)

    fun archiveConversation(conversationId: String, archive: Boolean) {
        if (archive) {
            archivedConversationIds.add(conversationId)
        } else {
            archivedConversationIds.remove(conversationId)
        }
        saveArchivedConversationsToFile()
    }

    fun getArchivedConversations(projectId: String): List<ConversationSummaryUi> {
        val targetProj = if (isDefaultOrNoProjectId(projectId)) DEFAULT_PROJECT_ID else projectId
        return archivedConversationIds.mapNotNull { id ->
            if (deletedConversationIds.contains(id)) return@mapNotNull null
            val conv = allLoadedConversations[id] ?: return@mapNotNull null
            val pId = if (isDefaultOrNoProjectId(conv.projectId)) DEFAULT_PROJECT_ID else conv.projectId
            if (pId == targetProj) conv else null
        }.sortedByDescending { it.updatedAtMillis }
    }

    fun removeConversationFromMemory(conversationId: String) {
        deletedConversationIds.add(conversationId)
        saveDeletedConversationsToFile()
        conversationProjectBindings.remove(conversationId)
        allLoadedConversations.remove(conversationId)
        archivedConversationIds.remove(conversationId)
        saveArchivedConversationsToFile()
    }

    suspend fun deleteConversation(conversationId: String): Boolean = withContext(Dispatchers.IO) {
        if (conversationId.isBlank()) return@withContext false
        removeConversationFromMemory(conversationId)

        // 1. 从官方 conversation_summaries.db 中删除该会话记录
        runCatching {
            if (SUMMARIES_DB_FILE.exists()) {
                var db: SQLiteDatabase? = null
                try {
                    db = SQLiteDatabase.openDatabase(SUMMARIES_DB_FILE.absolutePath, null, SQLiteDatabase.OPEN_READWRITE)
                    db.delete("conversation_summaries", "conversation_id = ?", arrayOf(conversationId))
                    android.util.Log.i("AgyProjectRepo", "Deleted $conversationId from SUMMARIES_DB_FILE")
                } finally {
                    db?.close()
                }
            }
        }.onFailure {
            android.util.Log.e("AgyProjectRepo", "Failed to delete $conversationId from SUMMARIES_DB_FILE", it)
        }

        // 2. 清理临时缓存 DB
        runCatching {
            val tempDbFile = File("/data/data/com.cortex.ai/cache/temp_summaries.db")
            if (tempDbFile.exists()) {
                var db: SQLiteDatabase? = null
                try {
                    db = SQLiteDatabase.openDatabase(tempDbFile.absolutePath, null, SQLiteDatabase.OPEN_READWRITE)
                    db.delete("conversation_summaries", "conversation_id = ?", arrayOf(conversationId))
                } finally {
                    db?.close()
                }
            }
        }

        // 3. 安全删除该会话专属的 brain 日志目录（严格限制为指定 conversationId 目录，绝对不触碰项目目录和用户文件）
        listOf(
            File("$DEBIAN_ROOTFS/root/.gemini/antigravity-cli/brain/$conversationId"),
            File("$DEBIAN_ROOTFS/root/.gemini/antigravity/brain/$conversationId"),
        ).forEach { dir ->
            if (dir.exists()) {
                runCatching {
                    dir.deleteRecursively()
                    android.util.Log.i("AgyProjectRepo", "Deleted brain directory: ${dir.absolutePath}")
                }
            }
        }

        // 4. 清理内存状态与归档记录
        conversationProjectBindings.remove(conversationId)
        allLoadedConversations.remove(conversationId)
        archiveConversation(conversationId, false)

        true
    }

    data class AccountInfo(
        val email: String,
        val name: String,
    )

    fun loadAccountInfo(): AccountInfo? {
        if (!ACCOUNT_INFO_FILE.exists() || !ACCOUNT_INFO_FILE.canRead()) return null
        return runCatching {
            val json = JSONObject(ACCOUNT_INFO_FILE.readText())
            val email = json.optString("email", "").trim()
            val name = json.optString("name", "").trim()
            if (email.isNotBlank()) AccountInfo(email, name) else null
        }.getOrNull()
    }

    fun toChrootPath(hostPath: String): String {
        val clean = hostPath.trim()
        if (clean.isBlank()) return "/root"
        return when {
            clean.startsWith("/storage/emulated/0") -> clean.replaceFirst("/storage/emulated/0", "/sdcard")
            clean.startsWith("/sdcard") -> clean
            clean.startsWith("/data/local/tmp") -> clean
            clean.startsWith("/root") -> clean
            clean.startsWith("/") -> clean
            else -> "/sdcard/$clean"
        }
    }

    fun extractWorkspacePath(workspaceUris: String): String {
        if (workspaceUris.isBlank()) return "/root"
        return runCatching {
            val array = org.json.JSONArray(workspaceUris)
            val firstUri = array.optString(0, "").trim()
            if (firstUri.isBlank()) return@runCatching "/root"
            val path = if (firstUri.startsWith("file://")) {
                firstUri.removePrefix("file://")
            } else {
                firstUri
            }
            if (path.isBlank()) return@runCatching "/root"
            toChrootPath(path)
        }.getOrNull()?.ifBlank { "/root" } ?: "/root"
    }

    fun bindConversationProject(conversationId: String, projectId: String) {
        if (conversationId.isNotBlank() && projectId.isNotBlank()) {
            conversationProjectBindings[conversationId] = projectId
        }
    }

    fun getProjectForConversation(conversationId: String): String? {
        return conversationProjectBindings[conversationId]
    }

    fun getProject(id: String): AgyProjectUi? {
        return cachedProjects[id]
    }

    fun hasConversationInDb(conversationId: String): Boolean {
        if (!SUMMARIES_DB_FILE.exists() || !SUMMARIES_DB_FILE.canRead()) return false
        var db: SQLiteDatabase? = null
        return try {
            db = SQLiteDatabase.openDatabase(SUMMARIES_DB_FILE.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
            val cursor = db.rawQuery(
                "SELECT 1 FROM conversation_summaries WHERE conversation_id = ? LIMIT 1",
                arrayOf(conversationId)
            )
            cursor.use { it.moveToFirst() }
        } catch (_: Exception) {
            false
        } finally {
            db?.close()
        }
    }

    suspend fun loadProjects(cortexConversations: List<ConversationSummaryUi> = emptyList()): List<AgyProjectUi> =
        withContext(Dispatchers.IO) {
            val projectsMap = mutableMapOf<String, AgyProjectUi>()

            fun readProjectsFromDir(dir: File) {
                if (dir.exists() && dir.isDirectory) {
                    dir.listFiles()?.filter { it.extension == "json" }?.forEach { file ->
                        runCatching {
                            val json = JSONObject(file.readText())
                            val id = json.optString("id", file.nameWithoutExtension)
                            if (isDefaultOrNoProjectId(id) && id != DEFAULT_PROJECT_ID) {
                                return@runCatching
                            }
                            val name = json.optString("name", id)
                            val workspacePath = json.optString("workspacePath", "/root")
                            projectsMap[id] = AgyProjectUi(
                                id = id,
                                name = if (id == DEFAULT_PROJECT_ID) DEFAULT_PROJECT_NAME else name,
                                workspacePath = workspacePath,
                                conversations = emptyList(),
                            )
                        }
                    }
                }
            }

            // 1. 读取应用私有存储中的项目定义（不受 rootfs 权限限制）
            readProjectsFromDir(LOCAL_PROJECTS_DIR)

            // 2. 读取官方 rootfs 已定义项目
            readProjectsFromDir(PROJECTS_DIR)

            // 确保默认项目存在
            if (!projectsMap.containsKey(DEFAULT_PROJECT_ID)) {
                projectsMap[DEFAULT_PROJECT_ID] = AgyProjectUi(
                    id = DEFAULT_PROJECT_ID,
                    name = DEFAULT_PROJECT_NAME,
                    workspacePath = DEFAULT_WORKSPACE_PATH,
                    conversations = emptyList(),
                )
            }

            // 3. 从 SQLite 读取官方 agy 历史会话
            val dbConversations = loadConversationsFromDb()

            // 4. 合并 Cortex 内部会话与 DB 会话，并基于 workspace_uris 智能匹配项目
            val conversationMap = mutableMapOf<String, ConversationSummaryUi>()

            for (dbItem in dbConversations) {
                if (dbItem.title.isBlank() && dbItem.preview.isBlank()) continue
                val boundProj = conversationProjectBindings[dbItem.conversationId]
                val effectiveProjId = if (!boundProj.isNullOrBlank() && !isDefaultOrNoProjectId(boundProj)) {
                    boundProj
                } else {
                    // 解析真实工作区目录
                    val cleanPath = extractWorkspacePath(dbItem.workspaceUris)
                    val matched = projectsMap.values.firstOrNull { p ->
                        p.id != DEFAULT_PROJECT_ID && toChrootPath(p.workspacePath).trimEnd('/') == cleanPath.trimEnd('/')
                    }
                    if (matched != null) {
                        matched.id
                    } else if (cleanPath != "/root" && cleanPath.isNotBlank()) {
                        // 发现带具体目录的独立会话，自动派生项目
                        val folderName = cleanPath.trimEnd('/').substringAfterLast('/')
                        val autoId = "proj-" + folderName.lowercase().replace(Regex("[^a-z0-9_-]"), "-")
                        val autoProj = AgyProjectUi(
                            id = autoId,
                            name = folderName.ifBlank { "项目 " + autoId.take(8) },
                            workspacePath = cleanPath,
                            conversations = emptyList(),
                        )
                        projectsMap[autoId] = autoProj
                        autoId
                    } else {
                        DEFAULT_PROJECT_ID
                    }
                }

                conversationProjectBindings[dbItem.conversationId] = effectiveProjId

                conversationMap[dbItem.conversationId] = ConversationSummaryUi(
                    id = dbItem.conversationId,
                    title = dbItem.title.ifBlank { "会话 " + dbItem.conversationId.take(8) },
                    preview = dbItem.preview,
                    timeLabel = dbItem.timeLabel,
                    updatedAtMillis = dbItem.lastModifiedTimeMillis,
                    mode = ConversationModeUi.Chat,
                    projectId = effectiveProjId,
                )
            }

            // 5. 融合 Cortex 前端内部的活跃会话
            for (cortexItem in cortexConversations) {
                if (cortexItem.title.isBlank() && cortexItem.preview.isBlank()) continue
                val rawProj = conversationProjectBindings[cortexItem.id] ?: cortexItem.projectId.ifBlank { DEFAULT_PROJECT_ID }
                val projId = if (isDefaultOrNoProjectId(rawProj)) DEFAULT_PROJECT_ID else rawProj
                val merged = cortexItem.copy(projectId = projId)
                conversationMap[cortexItem.id] = merged
            }

            // 同步保存至全量会话缓存，供归档管理查询
            conversationMap.forEach { (id, item) ->
                allLoadedConversations[id] = item
            }

            // 过滤掉已归档会话与无内容空会话（避免空会话占位）
            val visibleConversations = conversationMap.values.filter { conv ->
                !isArchived(conv.id) &&
                    (conv.title.isNotBlank() || conv.preview.isNotBlank()) &&
                    (conv.preview.isNotBlank() || !conv.title.matches(Regex("^(会话|conv-)[0-9a-fA-F-]{4,36}$")))
            }

            // 按 project_id 聚合分组（将 outside-of-project 等无项目统一折叠到默认工作区）
            val grouped = visibleConversations.groupBy {
                if (isDefaultOrNoProjectId(it.projectId)) DEFAULT_PROJECT_ID else it.projectId
            }

            // 补齐缺失的项目定义
            grouped.keys.forEach { projId ->
                if (projId.isNotBlank() && !isDefaultOrNoProjectId(projId) && !projectsMap.containsKey(projId)) {
                    projectsMap[projId] = AgyProjectUi(
                        id = projId,
                        name = projId,
                        workspacePath = "/root",
                        conversations = emptyList(),
                    )
                }
            }

            val currentActiveId = activeProjectId
            val result = projectsMap.values.map { project ->
                val convs = grouped[project.id].orEmpty()
                    .sortedByDescending { it.updatedAtMillis }
                val updated = project.copy(conversations = convs)
                cachedProjects[project.id] = updated
                updated
            }.sortedWith(
                // 1. 项目与项目之间按最近活跃时间倒序排（最近产生会话活动的排在最上面）
                // 2. 无会话的项目保持稳定排在后面，按名称排序
                compareByDescending<AgyProjectUi> { project ->
                    project.conversations.maxOfOrNull { it.updatedAtMillis } ?: 0L
                }.thenBy { it.name }
            )

            android.util.Log.i(
                "AgyProjectRepo",
                "loadProjects complete: active=$currentActiveId, dbConvs=${dbConversations.size}, cortexConvs=${cortexConversations.size}, projects=${result.map { "${it.name}(${it.id}): ${it.conversations.size} convs" }}"
            )

            result
        }

    fun createProject(name: String, workspacePath: String): AgyProjectUi {
        val sanitized = name.trim().lowercase().replace(Regex("[^a-z0-9_-]"), "-").trim('-')
        val id = if (sanitized.isNotBlank()) "proj-$sanitized" else "proj-${System.currentTimeMillis()}"
        val ui = AgyProjectUi(
            id = id,
            name = name.trim().ifBlank { "新建项目" },
            workspacePath = workspacePath.trim().ifBlank { "/root" },
            conversations = emptyList(),
        )
        cachedProjects[id] = ui
        activeProjectId = id

        val json = JSONObject().apply {
            put("id", id)
            put("name", ui.name)
            put("workspacePath", ui.workspacePath)
            put("projectResources", JSONObject())
        }

        // 1. 优先可靠写入应用沙箱私有目录
        runCatching {
            LOCAL_PROJECTS_DIR.mkdirs()
            File(LOCAL_PROJECTS_DIR, "$id.json").writeText(json.toString(2))
        }

        // 2. 尝试写入 rootfs config 目录
        runCatching {
            PROJECTS_DIR.mkdirs()
            File(PROJECTS_DIR, "$id.json").writeText(json.toString(2))
        }

        return ui
    }

    private data class DbConversationItem(
        val conversationId: String,
        val title: String,
        val preview: String,
        val projectId: String,
        val workspaceUris: String,
        val timeLabel: String,
        val lastModifiedTimeMillis: Long,
    )

    private fun loadConversationsFromDb(): List<DbConversationItem> {
        if (!SUMMARIES_DB_FILE.exists()) {
            android.util.Log.w("AgyProjectRepo", "SUMMARIES_DB_FILE does not exist: ${SUMMARIES_DB_FILE.absolutePath}")
            return emptyList()
        }
        val items = mutableListOf<DbConversationItem>()
        val tempDbFile = File("/data/data/com.cortex.ai/cache/temp_summaries.db")
        var db: SQLiteDatabase? = null
        try {
            runCatching {
                tempDbFile.parentFile?.mkdirs()
                SUMMARIES_DB_FILE.copyTo(tempDbFile, overwrite = true)
            }
            val targetFile = if (tempDbFile.exists() && tempDbFile.length() > 0) tempDbFile else SUMMARIES_DB_FILE
            db = SQLiteDatabase.openDatabase(
                targetFile.absolutePath,
                null,
                if (targetFile == tempDbFile) SQLiteDatabase.OPEN_READWRITE else SQLiteDatabase.OPEN_READONLY
            )
            val cursor = db.rawQuery(
                "SELECT conversation_id, title, preview, project_id, workspace_uris, last_modified_time FROM conversation_summaries ORDER BY last_modified_time DESC",
                null
            )
            cursor.use { c ->
                val idIdx = c.getColumnIndex("conversation_id")
                val titleIdx = c.getColumnIndex("title")
                val prevIdx = c.getColumnIndex("preview")
                val projIdx = c.getColumnIndex("project_id")
                val uriIdx = c.getColumnIndex("workspace_uris")
                val timeIdx = c.getColumnIndex("last_modified_time")

                while (c.moveToNext()) {
                    val id = if (idIdx >= 0) c.getString(idIdx).orEmpty() else ""
                    if (id.isBlank() || deletedConversationIds.contains(id)) continue
                    val title = if (titleIdx >= 0) c.getString(titleIdx).orEmpty() else ""
                    val preview = if (prevIdx >= 0) c.getString(prevIdx).orEmpty() else ""
                    if (title.isBlank() && preview.isBlank()) continue
                    val rawProjId = if (projIdx >= 0) c.getString(projIdx).orEmpty() else ""
                    val projId = if (isDefaultOrNoProjectId(rawProjId)) DEFAULT_PROJECT_ID else rawProjId
                    val uri = if (uriIdx >= 0) c.getString(uriIdx).orEmpty() else ""
                    val timeStr = if (timeIdx >= 0) c.getString(timeIdx).orEmpty() else ""

                    val timeMillis = parseTimeMillis(timeStr)
                    val timeLabel = formatRelativeTime(timeMillis)

                    conversationProjectBindings[id] = projId

                    items.add(
                        DbConversationItem(
                            conversationId = id,
                            title = title,
                            preview = preview,
                            projectId = projId,
                            workspaceUris = uri,
                            timeLabel = timeLabel,
                            lastModifiedTimeMillis = timeMillis,
                        )
                    )
                }
            }
            android.util.Log.i("AgyProjectRepo", "Successfully read ${items.size} conversations from DB snapshot")
        } catch (e: Exception) {
            android.util.Log.e("AgyProjectRepo", "Failed to load DB conversations", e)
        } finally {
            db?.close()
        }
        return items
    }

    private fun parseTimeMillis(isoString: String): Long {
        if (isoString.isBlank()) return System.currentTimeMillis()
        return try {
            OffsetDateTime.parse(isoString.trim(), DateTimeFormatter.ISO_OFFSET_DATE_TIME).toInstant().toEpochMilli()
        } catch (_: Exception) {
            try {
                val normalized = isoString.trim().replace(" ", "T")
                OffsetDateTime.parse(normalized, DateTimeFormatter.ISO_OFFSET_DATE_TIME).toInstant().toEpochMilli()
            } catch (_: Exception) {
                System.currentTimeMillis()
            }
        }
    }

    private fun formatRelativeTime(millis: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - millis
        val minutes = diff / 60000
        val hours = diff / 3600000
        val days = diff / 86400000
        return when {
            minutes < 1 -> "刚刚"
            minutes < 60 -> "${minutes}分钟前"
            hours < 24 -> "${hours}小时前"
            days == 1L -> "昨天"
            days < 7 -> "${days}天前"
            else -> java.text.SimpleDateFormat("MM月dd日", java.util.Locale.CHINA).format(java.util.Date(millis))
        }
    }

    fun parseTranscriptJsonl(jsonlContent: String): List<AgentChatMessageUi> {
        val messages = mutableListOf<AgentChatMessageUi>()
        var stepCounter = 0
        jsonlContent.lineSequence().forEach { line ->
            val trimmed = line.trim()
            if (trimmed.isNotBlank()) {
                runCatching {
                    val json = JSONObject(trimmed)
                    val type = json.optString("type", "")
                    val stepIndex = json.optInt("step_index", stepCounter++)
                    when (type) {
                        "USER_INPUT" -> {
                            val content = json.optString("content", "")
                            val text = if (content.contains("<USER_REQUEST>")) {
                                content.substringAfter("<USER_REQUEST>").substringBefore("</USER_REQUEST>").trim()
                            } else {
                                content.trim()
                            }
                            if (text.isNotBlank()) {
                                messages.add(
                                    UserMessageUi(
                                        id = "user-$stepIndex",
                                        content = text,
                                    )
                                )
                            }
                        }
                        "PLANNER_RESPONSE" -> {
                            val thinking = json.optString("thinking", "").trim()
                            if (thinking.isNotBlank()) {
                                messages.add(
                                    ThinkingMessageUi(
                                        id = "think-$stepIndex",
                                        content = thinking,
                                        isStreaming = false,
                                        collapsed = true,
                                    )
                                )
                            }
                            val toolCalls = json.optJSONArray("tool_calls")
                            if (toolCalls != null) {
                                for (i in 0 until toolCalls.length()) {
                                    val tool = toolCalls.optJSONObject(i) ?: continue
                                    val toolName = tool.optString("name", "tool")
                                    val argsObj = tool.optJSONObject("args")
                                    val summary = argsObj?.optString("toolSummary")?.removeSurrounding("\"")?.ifBlank { null }
                                        ?: argsObj?.optString("toolAction")?.removeSurrounding("\"")?.ifBlank { null }
                                        ?: argsObj?.optString("CommandLine")?.removeSurrounding("\"")?.ifBlank { null }
                                        ?: toolName
                                    val command = argsObj?.optString("CommandLine")?.removeSurrounding("\"")
                                    messages.add(
                                        ToolActivityMessageUi(
                                            id = "tool-$stepIndex-$i",
                                            toolName = toolName,
                                            status = ToolActivityStatusUi.Success,
                                            argumentsSummary = summary,
                                            command = command,
                                        )
                                    )
                                }
                            }
                            val content = json.optString("content", "").trim()
                            if (content.isNotBlank()) {
                                messages.add(
                                    AgentMessageUi(
                                        id = "agent-$stepIndex",
                                        content = content,
                                        isStreaming = false,
                                        renderMarkdown = true,
                                    )
                                )
                            }
                        }
                        "GENERIC" -> {
                            val content = json.optString("content", "").trim()
                            if (content.isNotBlank()) {
                                val lastToolIdx = messages.indexOfLast { it is ToolActivityMessageUi }
                                if (lastToolIdx >= 0) {
                                    val lastTool = messages[lastToolIdx] as ToolActivityMessageUi
                                    messages[lastToolIdx] = lastTool.copy(
                                        resultSummary = content.take(300),
                                        status = ToolActivityStatusUi.Success,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        return messages
    }

    fun loadTranscriptMessages(conversationId: String): List<AgentChatMessageUi>? {
        if (conversationId.isBlank()) return null
        val candidates = listOf(
            File("$DEBIAN_ROOTFS/root/.gemini/antigravity-cli/brain/$conversationId/.system_generated/logs/transcript.jsonl"),
            File("$DEBIAN_ROOTFS/root/.gemini/antigravity-cli/brain/$conversationId/.system_generated/logs/transcript_full.jsonl"),
            File("$DEBIAN_ROOTFS/root/.gemini/antigravity/brain/$conversationId/.system_generated/logs/transcript.jsonl"),
            File("$DEBIAN_ROOTFS/root/.gemini/antigravity/brain/$conversationId/.system_generated/logs/transcript_full.jsonl"),
        )
        val targetFile = candidates.firstOrNull { it.exists() && it.length() > 0 } ?: return null
        return runCatching {
            parseTranscriptJsonl(targetFile.readText())
        }.getOrNull()
    }
}
