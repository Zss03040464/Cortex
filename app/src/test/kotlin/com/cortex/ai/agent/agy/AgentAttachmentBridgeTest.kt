package com.cortex.ai.agent.agy

import com.cortex.ai.agent.model.AgentFileReference
import com.cortex.ai.agent.model.AgentFileReferenceKind
import org.junit.Assert.*
import org.junit.Test

class AgentAttachmentBridgeTest {

    @Test
    fun formatPrompt_plainTextOnly() {
        val prompt = "请分析这个算法"
        val formatted = AgentAttachmentBridge.formatPrompt(
            request = prompt,
            fileReferences = emptyList(),
            imagePaths = emptyList(),
        )
        assertEquals("请分析这个算法", formatted)
    }

    @Test
    fun formatPrompt_withImages() {
        val prompt = "帮我看看这个截图"
        val imagePaths = listOf("/sdcard/Android/data/com.cortex.ai/files/images/user_img_1.jpg")
        val formatted = AgentAttachmentBridge.formatPrompt(
            request = prompt,
            fileReferences = emptyList(),
            imagePaths = imagePaths,
        )
        assertTrue(formatted.contains("# Images attached by the user:"))
        assertTrue(formatted.contains("- [图片 1] /sdcard/Android/data/com.cortex.ai/files/images/user_img_1.jpg"))
        assertTrue(formatted.contains("## My request:\n帮我看看这个截图"))
    }

    @Test
    fun formatPrompt_withImagesAndFiles() {
        val prompt = "请结合日志与截图排查"
        val imagePaths = listOf("/sdcard/Android/data/com.cortex.ai/files/images/user_img_1.jpg")
        val files = listOf(
            AgentFileReference(
                displayName = "crash.log",
                absolutePath = "/sdcard/crash.log",
                kind = AgentFileReferenceKind.File,
            )
        )
        val formatted = AgentAttachmentBridge.formatPrompt(
            request = prompt,
            fileReferences = files,
            imagePaths = imagePaths,
        )
        assertTrue(formatted.contains("# Images attached by the user:"))
        assertTrue(formatted.contains("# Files mentioned by the user:"))
        assertTrue(formatted.contains("## crash.log: /sdcard/crash.log"))
        assertTrue(formatted.contains("## My request:\n请结合日志与截图排查"))
    }

    @Test
    fun formatPrompt_emptyPromptWithImagesDefaultsCleanly() {
        val imagePaths = listOf("/sdcard/Android/data/com.cortex.ai/files/images/user_img_1.jpg")
        val formatted = AgentAttachmentBridge.formatPrompt(
            request = "",
            fileReferences = emptyList(),
            imagePaths = imagePaths,
        )
        assertTrue(formatted.contains("# Images attached by the user:"))
        assertTrue(formatted.contains("请查看并分析以上图片内容。"))
    }

    @Test
    fun formatPrompt_emptyPromptWithFilesDefaultsCleanly() {
        val files = listOf(
            AgentFileReference(
                displayName = "main.py",
                absolutePath = "/sdcard/main.py",
                kind = AgentFileReferenceKind.File,
            )
        )
        val formatted = AgentAttachmentBridge.formatPrompt(
            request = "",
            fileReferences = files,
            imagePaths = emptyList(),
        )
        assertTrue(formatted.contains("# Files mentioned by the user:"))
        assertTrue(formatted.contains("请查看并分析以上文件内容。"))
    }
}
