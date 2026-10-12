package com.cortex.ai.agent.agy

import android.content.Context
import android.net.Uri
import android.util.Base64
import com.cortex.ai.agent.model.AgentFileReference
import com.cortex.ai.agent.model.AgentFileReferenceKind
import com.cortex.ai.ui.model.PendingImageUi
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

internal object AgentAttachmentBridge {
    private const val IMAGES_HEADER = "# Images attached by the user:"
    private const val FILES_HEADER = "# Files mentioned by the user:"
    private const val REQUEST_HEADER = "## My request:"
    private const val ENTRY_PREFIX = "## "

    /**
     * 将用户选中的待发送图片持久化至 Debian chroot 可访问的物理目录，并返回绝对路径列表。
     */
    fun persistPendingImages(context: Context, images: List<PendingImageUi>): List<String> {
        if (images.isEmpty()) return emptyList()

        val targetDir = runCatching {
            val external = context.getExternalFilesDir("images")
            if (external != null && (external.exists() || external.mkdirs())) {
                external
            } else {
                File("/data/local/tmp/eta_images").apply { mkdirs() }
            }
        }.getOrNull() ?: File(context.cacheDir, "images").apply { mkdirs() }

        val paths = mutableListOf<String>()
        images.forEachIndexed { index, pending ->
            val ext = when {
                pending.mimeType.contains("png", ignoreCase = true) -> "png"
                pending.mimeType.contains("webp", ignoreCase = true) -> "webp"
                pending.mimeType.contains("gif", ignoreCase = true) -> "gif"
                else -> "jpg"
            }
            val fileName = "user_img_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}.$ext"
            val file = File(targetDir, fileName)

            val bytes = runCatching { extractBytes(context, pending) }.getOrNull()
            if (bytes != null && bytes.isNotEmpty()) {
                runCatching {
                    FileOutputStream(file).use { it.write(bytes); it.flush() }
                    // 确保权限为全局可读
                    file.setReadable(true, false)
                    val chrootPath = AgyProjectRepository.toChrootPath(file.absolutePath)
                    paths.add(chrootPath)
                }
            }
        }
        return paths
    }

    private fun extractBytes(context: Context, pending: PendingImageUi): ByteArray? {
        val dataUrl = pending.dataUrl
        if (dataUrl.startsWith("data:image/", ignoreCase = true)) {
            val marker = "base64,"
            val idx = dataUrl.indexOf(marker, ignoreCase = true)
            if (idx >= 0) {
                val b64 = dataUrl.substring(idx + marker.length)
                return Base64.decode(b64, Base64.DEFAULT)
            }
        }
        val uriStr = pending.uri
        if (uriStr.isNotBlank()) {
            val uri = Uri.parse(uriStr)
            if (uri.scheme == "content") {
                return context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            }
            if (uri.scheme == "file") {
                val f = uri.path?.let(::File)
                if (f != null && f.isFile) return f.readBytes()
            }
            val f = File(uriStr)
            if (f.isFile) return f.readBytes()
        }
        return null
    }

    /**
     * 将请求文本、文件引用与图片路径格式化为底层 Agy CLI 与模型清晰可读的结构化 Prompt。
     */
    fun formatPrompt(
        request: String,
        fileReferences: List<AgentFileReference>,
        imagePaths: List<String>,
    ): String {
        val cleanRequest = request.trim()
        val uniqueFiles = fileReferences.distinctBy { it.absolutePath }
        val uniqueImages = imagePaths.distinct()

        if (uniqueFiles.isEmpty() && uniqueImages.isEmpty()) {
            return cleanRequest
        }

        return buildString {
            if (uniqueImages.isNotEmpty()) {
                appendLine(IMAGES_HEADER)
                appendLine()
                uniqueImages.forEachIndexed { i, path ->
                    appendLine("- [图片 ${i + 1}] $path")
                }
                appendLine()
            }

            if (uniqueFiles.isNotEmpty()) {
                appendLine(FILES_HEADER)
                appendLine()
                uniqueFiles.forEachIndexed { index, reference ->
                    if (index > 0) appendLine()
                    append(ENTRY_PREFIX)
                    append(reference.displayName)
                    if (reference.kind == AgentFileReferenceKind.Directory) append("/")
                    append(": ")
                    appendLine(reference.absolutePath)
                }
                appendLine()
            }

            appendLine(REQUEST_HEADER)
            if (cleanRequest.isNotEmpty()) {
                append(cleanRequest)
            } else if (uniqueImages.isNotEmpty() && uniqueFiles.isNotEmpty()) {
                append("请结合以上图片与文件内容进行分析并回答。")
            } else if (uniqueImages.isNotEmpty()) {
                append("请查看并分析以上图片内容。")
            } else {
                append("请查看并分析以上文件内容。")
            }
        }
    }
}
