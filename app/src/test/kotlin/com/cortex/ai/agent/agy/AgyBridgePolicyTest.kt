package com.cortex.ai.agent.agy

import com.cortex.ai.agent.runtime.AgentExternalArchivePayload
import com.cortex.ai.agent.runtime.AgentRuntimeWire
import org.junit.Assert.*
import org.junit.Test

class AgyBridgePolicyTest {
    @Test fun configDoesNotRequireAnApiKey() {
        val config = AgyBridgePolicy.config()
        assertEquals("agy", config.providerId)
        assertEquals("", config.apiKey)
        assertEquals("", config.baseUrl)
    }

    @Test fun sameAgentUiConversationIdUsesSameSessionKeyAcrossTurns() {
        val convId = "conv-ui-test-1"
        val turn1 = AgentRuntimeWire.RunRequest(
            runId = "run-turn-1",
            prompt = "hello",
            config = AgyBridgePolicy.config(),
            handoff = AgentRuntimeWire.EntryHandoff(
                id = "run-turn-1",
                source = AgentRuntimeWire.AGENT_UI_HANDOFF_SOURCE,
                payload = convId,
            ),
        )
        val turn2 = AgentRuntimeWire.RunRequest(
            runId = "run-turn-2",
            prompt = "follow up",
            config = AgyBridgePolicy.config(),
            handoff = AgentRuntimeWire.EntryHandoff(
                id = "run-turn-2",
                source = AgentRuntimeWire.AGENT_UI_HANDOFF_SOURCE,
                payload = convId,
            ),
        )
        val continuationTurn = AgentRuntimeWire.RunRequest(
            runId = "run-turn-3",
            prompt = "continued",
            config = AgyBridgePolicy.config(),
            handoff = AgentRuntimeWire.EntryHandoff(
                id = "run-turn-3",
                source = AgentRuntimeWire.AGENT_UI_HANDOFF_SOURCE,
                payload = com.cortex.ai.agent.runtime.AgentUiHandoffPayload(conversationId = convId).toJson(),
            ),
        )
        assertEquals(convId, AgyBridgePolicy.sessionKey(turn1))
        assertEquals(AgyBridgePolicy.sessionKey(turn1), AgyBridgePolicy.sessionKey(turn2))
        assertEquals(AgyBridgePolicy.sessionKey(turn1), AgyBridgePolicy.sessionKey(continuationTurn))
    }

    @Test fun differentAgentUiConversationIdsUseDifferentSessionKeys() {
        val convA = AgentRuntimeWire.RunRequest(
            runId = "run-a",
            prompt = "query a",
            config = AgyBridgePolicy.config(),
            handoff = AgentRuntimeWire.EntryHandoff(
                id = "run-a",
                source = AgentRuntimeWire.AGENT_UI_HANDOFF_SOURCE,
                payload = "conv-alpha",
            ),
        )
        val convB = AgentRuntimeWire.RunRequest(
            runId = "run-b",
            prompt = "query b",
            config = AgyBridgePolicy.config(),
            handoff = AgentRuntimeWire.EntryHandoff(
                id = "run-b",
                source = AgentRuntimeWire.AGENT_UI_HANDOFF_SOURCE,
                payload = "conv-beta",
            ),
        )
        assertEquals("conv-alpha", AgyBridgePolicy.sessionKey(convA))
        assertEquals("conv-beta", AgyBridgePolicy.sessionKey(convB))
        assertNotEquals(AgyBridgePolicy.sessionKey(convA), AgyBridgePolicy.sessionKey(convB))
    }

    @Test fun breenoJsonHandoffRetainsOriginalBehavior() {
        val breenoSessionId = "breeno-1773340871302"
        val breenoPayload = AgentExternalArchivePayload(
            userText = "帮我打开小红书搜索k40",
            conversationKey = breenoSessionId,
            title = "小红书搜索",
        ).toJson()
        val breenoRequest = AgentRuntimeWire.RunRequest(
            runId = "run-breeno-1",
            prompt = "帮我打开小红书搜索k40",
            config = AgyBridgePolicy.config(),
            handoff = AgentRuntimeWire.EntryHandoff(
                id = "run-breeno-1",
                source = "breeno",
                payload = breenoPayload,
            ),
        )
        assertEquals(breenoSessionId, AgyBridgePolicy.sessionKey(breenoRequest))
    }

    @Test fun invalidOrMissingPayloadHasSafeFallbackWithoutMergingNormalSessions() {
        val missingHandoff1 = AgentRuntimeWire.RunRequest(
            runId = "run-missing-1",
            prompt = "hi",
            config = AgyBridgePolicy.config(),
            handoff = null,
        )
        val missingHandoff2 = AgentRuntimeWire.RunRequest(
            runId = "run-missing-2",
            prompt = "hi",
            config = AgyBridgePolicy.config(),
            handoff = null,
        )
        // Missing handoffs fallback safely and are isolated per runId, NOT merged into static "voice"
        assertNotEquals("voice", AgyBridgePolicy.sessionKey(missingHandoff1))
        assertNotEquals(AgyBridgePolicy.sessionKey(missingHandoff1), AgyBridgePolicy.sessionKey(missingHandoff2))

        val emptyPayloadRequest = AgentRuntimeWire.RunRequest(
            runId = "run-empty-1",
            prompt = "hi",
            config = AgyBridgePolicy.config(),
            handoff = AgentRuntimeWire.EntryHandoff(
                id = "run-empty-1",
                source = AgentRuntimeWire.AGENT_UI_HANDOFF_SOURCE,
                payload = "   ",
            ),
        )
        assertEquals("run-empty-1", AgyBridgePolicy.sessionKey(emptyPayloadRequest))

        val malformedJsonRequest = AgentRuntimeWire.RunRequest(
            runId = "run-malformed-1",
            prompt = "hi",
            config = AgyBridgePolicy.config(),
            handoff = AgentRuntimeWire.EntryHandoff(
                id = "run-malformed-1",
                source = AgentRuntimeWire.AGENT_UI_HANDOFF_SOURCE,
                payload = "{}",
            ),
        )
        assertEquals("run-malformed-1", AgyBridgePolicy.sessionKey(malformedJsonRequest))

        // Normal UI conversation must never collide with fallback sessions
        val normalUi = AgentRuntimeWire.RunRequest(
            runId = "run-normal",
            prompt = "hi",
            config = AgyBridgePolicy.config(),
            handoff = AgentRuntimeWire.EntryHandoff(
                id = "run-normal",
                source = AgentRuntimeWire.AGENT_UI_HANDOFF_SOURCE,
                payload = "conv-normal-ui",
            ),
        )
        assertNotEquals(AgyBridgePolicy.sessionKey(normalUi), AgyBridgePolicy.sessionKey(missingHandoff1))
        assertNotEquals(AgyBridgePolicy.sessionKey(normalUi), AgyBridgePolicy.sessionKey(emptyPayloadRequest))
        assertNotEquals(AgyBridgePolicy.sessionKey(normalUi), AgyBridgePolicy.sessionKey(malformedJsonRequest))
    }

    @Test fun supportsImageInputViaAttachmentBridge() {
        AgyBridgePolicy.requireSupported(operation = "run", imageCount = 1)
        AgyBridgePolicy.requireSupported(operation = "run", imageCount = 3)
    }

    @Test fun keepsUnsupportedOperationsOutOfTheCli() {
        assertThrows(IllegalArgumentException::class.java) {
            AgyBridgePolicy.requireSupported(AgentRuntimeWire.OP_REWRITE_REPLY, 0)
        }
    }

    @Test fun agentRulesContainCompletionVerificationPrinciples() {
        val rules = AgyRuntime.AGENT_RULES_CONTENT
        assertTrue(rules.contains("输入文字 ≠ 任务完成"))
        assertTrue(rules.contains("搜索类任务完成准则"))
        assertTrue(rules.contains("observe_screen"))
        assertTrue(rules.contains("tap_element"))
        assertTrue(rules.contains("overlay_display_devices"))
        assertTrue(rules.contains("virtual_display"))
        assertTrue(rules.contains("Anti-0ms Tap"))
        assertTrue(rules.contains("绝对禁止执行任何形式的重启"))
        assertTrue(rules.contains("多模态图片与文件附件交互规范"))
        assertTrue(rules.contains("view_file"))
    }
}
