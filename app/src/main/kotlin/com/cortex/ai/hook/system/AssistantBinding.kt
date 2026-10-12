package com.cortex.ai.hook.system

import com.cortex.ai.config.PowerAssistantTarget
import com.cortex.ai.core.ModuleConfig

internal data class AssistantBinding(
    val target: PowerAssistantTarget,
    val packageName: String,
    val componentName: String,
    val displayName: String,
)

internal fun assistantBindingFor(target: PowerAssistantTarget): AssistantBinding? = when (target) {
    PowerAssistantTarget.OEM -> null
    PowerAssistantTarget.GEMINI -> AssistantBinding(
        target = target,
        packageName = ModuleConfig.GOOGLE_PACKAGE,
        componentName = ModuleConfig.GOOGLE_ASSISTANT_COMPONENT,
        displayName = "Gemini",
    )
    PowerAssistantTarget.ETA -> AssistantBinding(
        target = target,
        packageName = ModuleConfig.ETA_PACKAGE,
        componentName = ModuleConfig.ETA_VOICE_INTERACTION_COMPONENT,
        displayName = "Cortex",
    )
}

internal fun shouldConfigureAssistant(
    autoConfigEnabled: Boolean,
    target: PowerAssistantTarget,
): Boolean = autoConfigEnabled && target != PowerAssistantTarget.OEM

internal enum class AssistantSelectionAction {
    NONE,
    CONFIGURE_MANAGED,
    RESTORE_OEM,
}

internal fun assistantSelectionAction(
    autoConfigEnabled: Boolean,
    target: PowerAssistantTarget,
): AssistantSelectionAction = when {
    target == PowerAssistantTarget.OEM -> AssistantSelectionAction.RESTORE_OEM
    shouldConfigureAssistant(autoConfigEnabled, target) ->
        AssistantSelectionAction.CONFIGURE_MANAGED
    else -> AssistantSelectionAction.NONE
}

internal fun isAssistantConfigurationCurrent(
    autoConfigEnabled: Boolean,
    expectedTarget: PowerAssistantTarget,
    currentTarget: PowerAssistantTarget,
): Boolean = shouldConfigureAssistant(autoConfigEnabled, currentTarget) &&
    expectedTarget == currentTarget
