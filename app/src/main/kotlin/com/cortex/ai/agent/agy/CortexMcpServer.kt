package com.cortex.ai.agent.agy

import com.cortex.ai.agent.model.AgentModelClient
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.security.SecureRandom
import java.util.Base64
import java.util.concurrent.Executors
import org.json.JSONArray
import org.json.JSONObject

/** A loopback-only, stateless MCP endpoint scoped to the currently executing Cortex run. */
internal class CortexMcpServer : AutoCloseable {
    data class Binding(
        val key: String,
        val tools: JSONArray,
        val execute: (AgentModelClient.ToolCall) -> AgentModelClient.ToolResult
    ) {
        @Volatile var isClosed = false
    }
    private val lock = Any()
    private val bindings = java.util.concurrent.ConcurrentHashMap<String, Binding>()
    private val activeBindings = java.util.concurrent.CopyOnWriteArrayList<Binding>()
    @Volatile private var closed = false
    private val socket = ServerSocket(0, 16, InetAddress.getByName("127.0.0.1"))
    private val path = "/" + ByteArray(32).also { SecureRandom().nextBytes(it) }.joinToString("") { "%02x".format(it) }
    val endpoint = "http://127.0.0.1:${socket.localPort}$path"
    private val workers = Executors.newFixedThreadPool(4) { runnable -> Thread(runnable, "cortex-mcp-http").apply { isDaemon = true } }
    private val acceptor = Thread({
        while (!closed) {
            try {
                val client = socket.accept()
                if (closed) client.close() else workers.execute { client.use { serve(it) } }
            } catch (_: java.io.IOException) {
                if (!closed) close()
            } catch (_: java.util.concurrent.RejectedExecutionException) { break }
        }
    }, "cortex-mcp-accept").apply { isDaemon = true; start() }

    fun bind(tools: JSONArray, execute: (AgentModelClient.ToolCall) -> AgentModelClient.ToolResult): AutoCloseable =
        bind("", tools, execute)

    fun bind(
        key: String,
        tools: JSONArray,
        execute: (AgentModelClient.ToolCall) -> AgentModelClient.ToolResult
    ): AutoCloseable {
        val entry = Binding(key, JSONArray(tools.toString()), execute)
        synchronized(lock) {
            check(!closed)
            if (key.isNotBlank()) {
                bindings[key] = entry
            }
            activeBindings.add(entry)
        }
        return AutoCloseable {
            entry.isClosed = true
            synchronized(lock) {
                if (key.isNotBlank()) {
                    bindings.remove(key, entry)
                }
                activeBindings.remove(entry)
            }
        }
    }

    fun registerAlias(aliasKey: String, targetKey: String) {
        if (aliasKey.isBlank() || targetKey.isBlank()) return
        val existing = bindings[targetKey] ?: activeBindings.lastOrNull { !it.isClosed } ?: return
        bindings[aliasKey] = existing
    }

    override fun close() {
        closed = true
        socket.close()
        workers.shutdownNow()
        synchronized(lock) {
            bindings.clear()
            activeBindings.clear()
        }
    }

    private fun serve(client: Socket) {
        val input = client.getInputStream()
        try {
            client.soTimeout = 5000
            var headerBytes = 0
            fun line(): String {
                val bytes = java.io.ByteArrayOutputStream()
                while (true) {
                    val b = input.read()
                    if (b < 0) throw java.io.EOFException()
                    if (++headerBytes > 16384) throw HttpFailure(431)
                    if (b == 10) break
                    bytes.write(b)
                }
                return bytes.toString("US-ASCII").removeSuffix("\r")
            }
            val request = line().split(' ')
            if (request.size != 3 || request[2] != "HTTP/1.1") throw HttpFailure(400)
            val headers = mutableMapOf<String, String>()
            while (true) {
                val line = line()
                if (line.isEmpty()) break
                val colon = line.indexOf(':')
                if (colon <= 0) throw HttpFailure(400)
                val key = line.substring(0, colon).lowercase(java.util.Locale.ROOT)
                if (headers.put(key, line.substring(colon + 1).trim()) != null) throw HttpFailure(400)
            }
            if (headers.containsKey("origin") || headers["host"] != "127.0.0.1:${socket.localPort}") throw HttpFailure(403)
            val requestPath = request[1]
            if (requestPath != path && !requestPath.startsWith("$path/")) throw HttpFailure(404)
            val subPath = requestPath.removePrefix(path).removePrefix("/")
            if (request[0] != "POST") throw HttpFailure(405)
            if (headers.containsKey("transfer-encoding")) throw HttpFailure(400)
            if (headers["content-type"]?.substringBefore(';')?.trim() != "application/json") throw HttpFailure(415)
            val length = headers["content-length"]?.toLongOrNull() ?: throw HttpFailure(411)
            if (length < 0) throw HttpFailure(400)
            if (length > 1_048_576) throw HttpFailure(413)
            // Capture at admission so a queued request cannot execute against a later run.
            val admitted = synchronized(lock) {
                if (subPath.isNotBlank()) bindings[subPath]?.takeUnless { it.isClosed }
                else activeBindings.lastOrNull { !it.isClosed }
            }
            val body = ByteArray(length.toInt())
            var offset = 0
            while (offset < body.size) {
                val read = input.read(body, offset, body.size - offset)
                if (read < 0) throw HttpFailure(400)
                offset += read
            }
            val message = try { JSONObject(String(body, Charsets.UTF_8)) } catch (_: Exception) {
                respond(client, 200, error(JSONObject.NULL, -32700, "Invalid JSON")); return
            }
            val id = message.opt("id") ?: JSONObject.NULL
            if (message.optString("jsonrpc") != "2.0" || !message.has("method")) {
                respond(client, 200, error(id, -32600, "Invalid request")); return
            }
            if (!message.has("id")) { respond(client, 202); return }
            val result = synchronized(lock) { dispatch(message, admitted) }
            respond(client, 200, result)
        } catch (failure: HttpFailure) {
            runCatching {
                while (input.available() > 0) {
                    if (input.read() < 0) break
                }
                respond(client, failure.status)
            }
        } catch (_: java.io.IOException) {
            // Peer disconnects and read timeouts terminate this connection.
        } catch (_: Exception) {
            runCatching { respond(client, 500) }
        }
    }

    private fun resolveBinding(message: JSONObject, admitted: Binding?): Binding? {
        val params = message.optJSONObject("params")
        val meta = params?.optJSONObject("_meta")
        val convId = meta?.optString("antigravity.google/conversation_id")
        if (!convId.isNullOrBlank()) {
            bindings[convId]?.takeUnless { it.isClosed }?.let { return it }
        }
        if (admitted != null && !admitted.isClosed) {
            return admitted
        }
        return activeBindings.lastOrNull { !it.isClosed }
    }

    private fun dispatch(message: JSONObject, admitted: Binding?): JSONObject {
        println("[CortexMcpServer-RPC] >>> IN: $message")
        val id = message.opt("id") ?: JSONObject.NULL
        fun success(result: JSONObject) = JSONObject().put("jsonrpc", "2.0").put("id", id).put("result", result)
        val response = when (message.getString("method")) {
            "initialize" -> success(JSONObject().put("protocolVersion", "2025-03-26")
                .put("capabilities", JSONObject().put("tools", JSONObject().put("listChanged", false)))
                .put("serverInfo", JSONObject().put("name", "cortex-android").put("version", "1.0")))
            "ping" -> success(JSONObject())
            "tools/list" -> {
                val target = resolveBinding(message, admitted)
                success(JSONObject().put("tools", catalog(target)))
            }
            "tools/call" -> {
                val active = resolveBinding(message, admitted)
                if (active == null || active.isClosed) return error(id, -32000, "No active run or run changed")
                val params = message.optJSONObject("params") ?: return error(id, -32602, "Missing parameters")
                val name = params.optString("name")
                val tools = catalog(active)
                if ((0 until tools.length()).none { tools.getJSONObject(it).getString("name") == name }) return error(id, -32602, "Unknown tool")
                if (params.has("arguments") && params.optJSONObject("arguments") == null) return error(id, -32602, "Arguments must be an object")
                try {
                    val result = active.execute(AgentModelClient.ToolCall(id.toString(), name, (params.optJSONObject("arguments") ?: JSONObject()).toString()))
                    val content = JSONArray().put(JSONObject().put("type", "text").put("text", result.content))
                    var invalidImage = false
                    result.images.forEach { image ->
                        val match = Regex("^data:(image/[a-zA-Z0-9.+-]+);base64,([A-Za-z0-9+/]*={0,2})$").matchEntire(image.reference)
                        val valid = match != null && runCatching { Base64.getDecoder().decode(match.groupValues[2]) }.isSuccess
                        if (valid) content.put(JSONObject().put("type", "image").put("mimeType", match.groupValues[1]).put("data", match.groupValues[2]))
                        else { invalidImage = true; content.put(JSONObject().put("type", "text").put("text", "Unsupported image reference: Cortex must supply a base64 image data URL.")) }
                    }
                    success(JSONObject().put("content", content).put("isError", invalidImage))
                } catch (_: Exception) {
                    success(JSONObject().put("content", JSONArray().put(JSONObject().put("type", "text").put("text", "Cortex tool execution failed."))).put("isError", true))
                }
            }
            else -> error(id, -32601, "Method not found")
        }
        println("[CortexMcpServer-RPC] <<< OUT: $response")
        return response
    }

    private fun catalog(entry: Binding?): JSONArray = JSONArray().apply {
        if (entry != null) for (i in 0 until entry.tools.length()) {
            val item = entry.tools.getJSONObject(i)
            val function = item.optJSONObject("function") ?: item
            put(JSONObject().put("name", function.getString("name"))
                .put("description", function.optString("description"))
                .put("inputSchema", function.optJSONObject("parameters") ?: JSONObject().put("type", "object")))
        }
    }
    private fun error(id: Any, code: Int, text: String) = JSONObject().put("jsonrpc", "2.0").put("id", id)
        .put("error", JSONObject().put("code", code).put("message", text))
    private fun respond(client: Socket, status: Int, body: JSONObject? = null) {
        val bytes = body?.toString()?.toByteArray(Charsets.UTF_8) ?: ByteArray(0)
        client.getOutputStream().apply {
            write("HTTP/1.1 $status Response\r\nContent-Type: application/json\r\nContent-Length: ${bytes.size}\r\nConnection: close\r\n\r\n".toByteArray(Charsets.US_ASCII))
            write(bytes); flush()
        }
    }
    private class HttpFailure(val status: Int) : Exception()
}
