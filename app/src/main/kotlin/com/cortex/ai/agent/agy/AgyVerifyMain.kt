package com.cortex.ai.agent.agy

import com.cortex.ai.agent.model.AgentModelClient
import com.cortex.ai.agent.runtime.AgentRuntimeSession
import com.cortex.ai.agent.runtime.AgentRuntimeWire
import org.json.JSONArray
import org.json.JSONObject

object AgyVerifyMain {
    @JvmStatic
    fun main(args: Array<String>) {
        if (args.isNotEmpty() && args[0] == "session-isolation") {
            runSessionIsolationVerification()
            return
        }

        println("=== [AgyVerifyMain] Formal Runtime Model Verification Start ===")
        val modelSlug = if (args.isNotEmpty()) args[0] else "gemini-3.8-flash-high"
        val prompt = if (args.size > 1) args.slice(1 until args.size).joinToString(" ") else "Return exactly: CORTEX_AGY_FORMAL_RUNTIME_OK"
        println("[AgyVerifyMain] Model: $modelSlug, Prompt: $prompt")

        val session = AgentRuntimeSession(
            runId = "test-run-${System.currentTimeMillis()}",
        )
        val request = AgentRuntimeWire.RunRequest(
            runId = session.runId,
            operation = AgentRuntimeWire.OP_CHAT,
            config = AgyBridgePolicy.config().copy(model = modelSlug),
            prompt = prompt,
            images = emptyList(),
        )

        val testTools = JSONArray()
        val testExecutor = AgentModelClient.ToolExecutor { toolCall ->
            AgentModelClient.ToolResult(content = "{}")
        }

        try {
            val response = AgyRuntime.execute(
                context = null,
                session = session,
                request = request,
                runTools = testTools,
                toolExecutor = testExecutor,
            ) { event ->
                println("[AgyVerifyMain] Stream Event: ${event.javaClass.simpleName}")
            }
            println("=== [AgyVerifyMain] Final Response ===")
            println(response.content)
            println("=== [AgyVerifyMain] Formal Runtime Model Verification Complete ===")
        } catch (e: Throwable) {
            System.err.println("[AgyVerifyMain] Verification Failed: ${e.message}")
            e.printStackTrace()
            System.exit(1)
        }
    }

    private fun runSessionIsolationVerification() {
        println("=== [AgyVerifyMain] Session Isolation Verification Start ===")
        val convAId = "cortex-conv-a-${System.currentTimeMillis()}"
        val convBId = "cortex-conv-b-${System.currentTimeMillis()}"

        val tools = com.cortex.ai.agent.model.AgentToolCatalog.build(
            terminalTools = true,
            browserTools = true,
            deviceDirectTools = true,
            deviceSensitiveReadTools = true,
            deviceSensitiveActionTools = true,
        )
        val dummyExecutor = AgentModelClient.ToolExecutor {
            AgentModelClient.ToolResult(content = "{}")
        }

        val config = AgyBridgePolicy.config().copy(
            terminalTools = true,
            browserTools = true,
            deviceDirectTools = true,
            deviceSensitiveReadTools = true,
            deviceSensitiveActionTools = true,
        )

        println("[Step 1] Conversation A - Turn 1 (convId=$convAId)")
        val reqA1 = AgentRuntimeWire.RunRequest(
            runId = "run-a1-${System.currentTimeMillis()}",
            operation = AgentRuntimeWire.OP_CHAT,
            config = config,
            prompt = "你好，请记住我的测试代号是 ALPHA-42。",
            handoff = AgentRuntimeWire.EntryHandoff(
                id = "run-a1",
                source = AgentRuntimeWire.AGENT_UI_HANDOFF_SOURCE,
                payload = convAId,
            ),
        )
        val sessionKeyA1 = AgyBridgePolicy.sessionKey(reqA1)
        println("  -> Resolved SessionKey A1: $sessionKeyA1")
        val sessionA = AgentRuntimeSession(runId = reqA1.runId)
        val respA1 = AgyRuntime.execute(
            context = null,
            session = sessionA,
            request = reqA1,
            runTools = tools,
            toolExecutor = dummyExecutor,
        ) { }
        println("  -> Response A1: ${respA1.content.trim()}")

        println("[Step 2] Conversation A - Turn 2 (convId=$convAId)")
        val reqA2 = AgentRuntimeWire.RunRequest(
            runId = "run-a2-${System.currentTimeMillis()}",
            operation = AgentRuntimeWire.OP_CHAT,
            config = config,
            prompt = "我刚才给你的测试代号是多少？只回复代号本身。",
            handoff = AgentRuntimeWire.EntryHandoff(
                id = "run-a2",
                source = AgentRuntimeWire.AGENT_UI_HANDOFF_SOURCE,
                payload = convAId,
            ),
        )
        val sessionKeyA2 = AgyBridgePolicy.sessionKey(reqA2)
        println("  -> Resolved SessionKey A2: $sessionKeyA2")
        val respA2 = AgyRuntime.execute(
            context = null,
            session = sessionA,
            request = reqA2,
            runTools = tools,
            toolExecutor = dummyExecutor,
        ) { }
        println("  -> Response A2: ${respA2.content.trim()}")

        println("[Step 3] Conversation B - Turn 1 (convId=$convBId, fresh session)")
        val reqB1 = AgentRuntimeWire.RunRequest(
            runId = "run-b1-${System.currentTimeMillis()}",
            operation = AgentRuntimeWire.OP_CHAT,
            config = config,
            prompt = "你现在能操作我手机上的应用吗？",
            handoff = AgentRuntimeWire.EntryHandoff(
                id = "run-b1",
                source = AgentRuntimeWire.AGENT_UI_HANDOFF_SOURCE,
                payload = convBId,
            ),
        )
        val sessionKeyB1 = AgyBridgePolicy.sessionKey(reqB1)
        println("  -> Resolved SessionKey B1: $sessionKeyB1")
        val sessionB = AgentRuntimeSession(runId = reqB1.runId)
        val respB1 = AgyRuntime.execute(
            context = null,
            session = sessionB,
            request = reqB1,
            runTools = tools,
            toolExecutor = dummyExecutor,
        ) { }
        println("  -> Response B1 (Raw Answer):")
        println(respB1.content)

        println("=== [AgyVerifyMain] Session Isolation Verification Complete ===")
    }
}
