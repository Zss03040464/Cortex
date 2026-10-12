package com.cortex.ai.agent.agy

import org.junit.Assert.*
import org.junit.Test

class AgyProjectRepositoryTest {

    @Test
    fun toChrootPathConvertsStorageEmulated0ToSdcard() {
        assertEquals("/sdcard/Download", AgyProjectRepository.toChrootPath("/storage/emulated/0/Download"))
        assertEquals("/sdcard", AgyProjectRepository.toChrootPath("/storage/emulated/0"))
        assertEquals("/sdcard/Projects/foo", AgyProjectRepository.toChrootPath("/storage/emulated/0/Projects/foo"))
    }

    @Test
    fun toChrootPathPreservesRootAndSdcardAndDataPaths() {
        assertEquals("/root", AgyProjectRepository.toChrootPath("/root"))
        assertEquals("/root/subfolder", AgyProjectRepository.toChrootPath("/root/subfolder"))
        assertEquals("/sdcard/my_code", AgyProjectRepository.toChrootPath("/sdcard/my_code"))
        assertEquals("/data/local/tmp/test", AgyProjectRepository.toChrootPath("/data/local/tmp/test"))
    }

    @Test
    fun toChrootPathHandlesRelativePathsGracefully() {
        assertEquals("/sdcard/my-project", AgyProjectRepository.toChrootPath("my-project"))
    }

    @Test
    fun bindConversationProjectStoresAndRetrievesMapping() {
        val convId = "conv-test-${System.currentTimeMillis()}"
        val projId = "proj-demo"

        assertNull(AgyProjectRepository.getProjectForConversation(convId))
        AgyProjectRepository.bindConversationProject(convId, projId)
        assertEquals(projId, AgyProjectRepository.getProjectForConversation(convId))
    }

    @Test
    fun createProjectCreatesValidProjectUiObject() {
        val proj = AgyProjectRepository.createProject("My Test Workspace", "/storage/emulated/0/Work")
        assertTrue(proj.id.startsWith("proj-"))
        assertEquals("My Test Workspace", proj.name)
        assertEquals("/storage/emulated/0/Work", proj.workspacePath)
        assertEquals(proj.id, AgyProjectRepository.activeProjectId)
        assertEquals(proj, AgyProjectRepository.getProject(proj.id))
    }

    @Test
    fun projectActiveStateAndResetToDefaultWorkAsExpected() {
        AgyProjectRepository.activeProjectId = AgyProjectRepository.DEFAULT_PROJECT_ID
        assertEquals(AgyProjectRepository.DEFAULT_PROJECT_ID, AgyProjectRepository.activeProjectId)

        val proj = AgyProjectRepository.createProject("Custom Project", "/sdcard/Custom")
        assertEquals(proj.id, AgyProjectRepository.activeProjectId)

        AgyProjectRepository.activeProjectId = AgyProjectRepository.DEFAULT_PROJECT_ID
        assertEquals(AgyProjectRepository.DEFAULT_PROJECT_ID, AgyProjectRepository.activeProjectId)
    }

    @Test
    fun extractWorkspacePathExtractsFilePathProperly() {
        assertEquals("/sdcard/Download", AgyProjectRepository.extractWorkspacePath("[\"file:///sdcard/Download\"]"))
        assertEquals("/sdcard/Download", AgyProjectRepository.extractWorkspacePath("[\"file:///storage/emulated/0/Download\"]"))
        assertEquals("/root", AgyProjectRepository.extractWorkspacePath("[\"file:///root\"]"))
        assertEquals("/root", AgyProjectRepository.extractWorkspacePath("[]"))
        assertEquals("/root", AgyProjectRepository.extractWorkspacePath(""))
    }

    @Test
    fun isDefaultOrNoProjectIdRecognizesSpecialAndEmptyIds() {
        assertTrue(AgyProjectRepository.isDefaultOrNoProjectId(null))
        assertTrue(AgyProjectRepository.isDefaultOrNoProjectId(""))
        assertTrue(AgyProjectRepository.isDefaultOrNoProjectId("   "))
        assertTrue(AgyProjectRepository.isDefaultOrNoProjectId("default-cli-project"))
        assertTrue(AgyProjectRepository.isDefaultOrNoProjectId("outside-of-project"))
        assertTrue(AgyProjectRepository.isDefaultOrNoProjectId("outside_of_project"))
        assertTrue(AgyProjectRepository.isDefaultOrNoProjectId("none"))
        assertTrue(AgyProjectRepository.isDefaultOrNoProjectId("default"))
        assertTrue(AgyProjectRepository.isDefaultOrNoProjectId("no-project"))

        assertFalse(AgyProjectRepository.isDefaultOrNoProjectId("proj-download"))
        assertFalse(AgyProjectRepository.isDefaultOrNoProjectId("proj-my-app"))
    }

    @Test
    fun parseTranscriptJsonlParsesUserAgentThinkingAndToolMessages() {
        val jsonl = """
            {"step_index":0,"source":"USER_EXPLICIT","type":"USER_INPUT","content":"<USER_REQUEST>\n请核验此校招信息\n</USER_REQUEST>\n<ADDITIONAL_METADATA>\nignore me\n</ADDITIONAL_METADATA>"}
            {"step_index":1,"source":"MODEL","type":"PLANNER_RESPONSE","thinking":"先核查该链接来源","tool_calls":[{"name":"read_url_content","args":{"Url":"https://example.com","toolAction":"核查页面内容","toolSummary":"页面验证"}}],"content":"正在获取该页面..."}
            {"step_index":2,"source":"MODEL","type":"GENERIC","content":"网页抓取成功，状态码 200"}
        """.trimIndent()

        val messages = AgyProjectRepository.parseTranscriptJsonl(jsonl)
        assertEquals(4, messages.size)

        // Message 0: UserMessageUi
        val userMsg = messages[0] as com.cortex.ai.ui.model.UserMessageUi
        assertEquals("请核验此校招信息", userMsg.content)

        // Message 1: ThinkingMessageUi
        val thinkMsg = messages[1] as com.cortex.ai.ui.model.ThinkingMessageUi
        assertEquals("先核查该链接来源", thinkMsg.content)

        // Message 2: ToolActivityMessageUi with resultSummary from step 2 GENERIC
        val toolMsg = messages[2] as com.cortex.ai.ui.model.ToolActivityMessageUi
        assertEquals("read_url_content", toolMsg.toolName)
        assertEquals("页面验证", toolMsg.argumentsSummary)
        assertEquals("网页抓取成功，状态码 200", toolMsg.resultSummary)

        // Message 3: AgentMessageUi
        val agentMsg = messages[3] as com.cortex.ai.ui.model.AgentMessageUi
        assertEquals("正在获取该页面...", agentMsg.content)
    }
}
