package com.cortex.ai.agent.agy

import com.cortex.ai.agent.model.AgentModelClient
import java.net.Socket
import java.net.URI
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class CortexMcpServerTest {
    private val catalog = JSONArray("""[{"type":"function","function":{"name":"echo","description":"Echo","parameters":{"type":"object"}}}]""")
    private fun request(server: CortexMcpServer, method: String, params: JSONObject = JSONObject(), path: String? = null): String {
        val body = JSONObject().put("jsonrpc", "2.0").put("id", 1).put("method", method).put("params", params).toString()
        return raw(server, body, path)
    }
    private fun raw(server: CortexMcpServer, body: String, path: String? = null, length: Int = body.toByteArray().size): String {
        val uri = URI(server.endpoint)
        return Socket(uri.host, uri.port).use { socket ->
            socket.soTimeout = 3000
            socket.getOutputStream().write(("POST ${path ?: uri.path} HTTP/1.1\r\nHost: ${uri.host}:${uri.port}\r\nContent-Type: application/json\r\nContent-Length: $length\r\n\r\n$body").toByteArray())
            socket.getInputStream().bufferedReader().readText()
        }
    }
    private fun json(response: String) = JSONObject(response.substringAfter("\r\n\r\n"))
    @Test fun initializesAndListsAndExecutesOnlyCatalogTools() = CortexMcpServer().use { server ->
        assertTrue(json(request(server, "initialize")).getJSONObject("result").has("capabilities"))
        server.bind(catalog) { AgentModelClient.ToolResult(it.argumentsJson) }.use {
            assertEquals("echo", json(request(server, "tools/list")).getJSONObject("result").getJSONArray("tools").getJSONObject(0).getString("name"))
            val params = JSONObject().put("name", "echo").put("arguments", JSONObject().put("a", 2))
            assertTrue(json(request(server, "tools/call", params)).getJSONObject("result").getJSONArray("content").getJSONObject(0).getString("text").contains("2"))
            assertTrue(json(request(server, "tools/call", JSONObject().put("name", "forbidden"))).has("error"))
        }
        assertTrue(json(request(server, "tools/call", JSONObject().put("name", "echo"))).has("error"))
    }
    @Test fun closingOldBindingDoesNotClearNewBinding() = CortexMcpServer().use { server ->
        val old = server.bind(catalog) { AgentModelClient.ToolResult("old") }
        server.bind(catalog) { AgentModelClient.ToolResult("new") }.use {
            old.close()
            assertTrue(request(server, "tools/call", JSONObject().put("name", "echo")).contains("new"))
        }
    }
    @Test fun rejectsWrongPathAndOversizeBody() = CortexMcpServer().use { server ->
        assertTrue(request(server, "ping", path = "/wrong").startsWith("HTTP/1.1 404"))
        assertTrue(raw(server, "", length = 2_000_000).startsWith("HTTP/1.1 413"))
    }
    @Test fun returnsTextAndImagesAndReportsUnsupportedImages() = CortexMcpServer().use { server ->
        server.bind(catalog) { AgentModelClient.ToolResult("text", listOf(AgentModelClient.ModelImage("data:image/png;base64,AQID", "image/png", 3))) }.use {
            val content = json(request(server, "tools/call", JSONObject().put("name", "echo"))).getJSONObject("result").getJSONArray("content")
            assertEquals(2, content.length())
            assertEquals("AQID", content.getJSONObject(1).getString("data"))
        }
        server.bind(catalog) { AgentModelClient.ToolResult("text", listOf(AgentModelClient.ModelImage("/private/image.png", "image/png", 3))) }.use {
            assertTrue(json(request(server, "tools/call", JSONObject().put("name", "echo"))).getJSONObject("result").getBoolean("isError"))
        }
    }
    @Test fun routesToSpecificSessionByKeyAndAlias() = CortexMcpServer().use { server ->
        val uri = URI(server.endpoint)
        val s1Binding = server.bind("session-1", catalog) { call ->
            AgentModelClient.ToolResult("session-1: ${call.argumentsJson}")
        }
        val s2Binding = server.bind("session-2", catalog) { call ->
            AgentModelClient.ToolResult("session-2: ${call.argumentsJson}")
        }

        try {
            // 1. Direct subpath routing
            val r1 = request(server, "tools/call", JSONObject().put("name", "echo").put("arguments", JSONObject().put("k", "v1")), path = "${uri.path}/session-1")
            assertTrue(r1, json(r1).getJSONObject("result").getJSONArray("content").getJSONObject(0).getString("text").contains("session-1: {\"k\":\"v1\"}"))

            val r2 = request(server, "tools/call", JSONObject().put("name", "echo").put("arguments", JSONObject().put("k", "v2")), path = "${uri.path}/session-2")
            assertTrue(r2, json(r2).getJSONObject("result").getJSONArray("content").getJSONObject(0).getString("text").contains("session-2: {\"k\":\"v2\"}"))

            // 2. Metadata conversation_id alias routing on root path
            server.registerAlias("conv-s1", "session-1")
            val metaParams = JSONObject()
                .put("name", "echo")
                .put("arguments", JSONObject().put("alias", "test"))
                .put("_meta", JSONObject().put("antigravity.google/conversation_id", "conv-s1"))
            val rMeta = request(server, "tools/call", metaParams)
            assertTrue(rMeta, json(rMeta).getJSONObject("result").getJSONArray("content").getJSONObject(0).getString("text").contains("session-1: {\"alias\":\"test\"}"))
        } finally {
            s1Binding.close()
            s2Binding.close()
        }
    }
}

