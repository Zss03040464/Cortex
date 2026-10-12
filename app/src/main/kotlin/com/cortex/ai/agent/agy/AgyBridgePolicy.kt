package com.cortex.ai.agent.agy

import com.cortex.ai.agent.model.AgentModelClient
import com.cortex.ai.agent.runtime.AgentExternalArchivePayload
import com.cortex.ai.agent.runtime.AgentRuntimeWire

import com.cortex.ai.agent.runtime.AgentUiHandoffPayload

internal object AgyBridgePolicy {
    fun config(): AgentModelClient.ModelConfig =
        AgentModelClient.ModelConfig(
            providerId = "agy",
            apiKey = "",
            baseUrl = "",
            model = "agy",
            systemPrompt = "",
        )

    fun sessionKey(request: AgentRuntimeWire.RunRequest): String {
        request.handoff?.let { handoff ->
            if (handoff.source == AgentRuntimeWire.AGENT_UI_HANDOFF_SOURCE) {
                val convId = AgentUiHandoffPayload.from(handoff.payload).conversationId.trim()
                if (convId.isNotBlank() && !convId.startsWith("{")) {
                    return convId
                }
            }
            AgentExternalArchivePayload.from(handoff.payload)?.conversationKey?.takeIf { it.isNotBlank() }?.let { return it }
            if (handoff.source != AgentRuntimeWire.AGENT_UI_HANDOFF_SOURCE) {
                handoff.payload.trim().takeIf { it.isNotBlank() && !it.startsWith("{") }?.let { return it }
            }
        }
        return request.modelSessionId.trim().takeIf { it.isNotBlank() }
            ?: request.runId.trim().takeIf { it.isNotBlank() }
            ?: "session-fallback"
    }

    fun requireSupported(operation: String, imageCount: Int) {
        require(operation in setOf(AgentRuntimeWire.OP_CHAT, "run")) { "Unsupported operation: $operation" }
    }

    fun isAgyActive(config: AgentModelClient.ModelConfig? = null): Boolean = true
}
