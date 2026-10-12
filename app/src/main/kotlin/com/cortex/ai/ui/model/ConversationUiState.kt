package com.cortex.ai.ui.model

import androidx.compose.runtime.Immutable

@Immutable
data class ConversationPaneUiState(
    val conversations: List<ConversationSummaryUi>,
    val projects: List<AgyProjectUi> = emptyList(),
    val selectedProjectId: String? = null,
    val selectedConversationId: String?,
    val searchQuery: String,
    val accountEmail: String? = null,
    val activeModelName: String = "Gemini 3.8 Flash (High)",
)

@Immutable
data class AgyProjectUi(
    val id: String,
    val name: String,
    val workspacePath: String,
    val conversations: List<ConversationSummaryUi> = emptyList(),
    val isExpanded: Boolean = true,
)

@Immutable
data class ConversationSummaryUi(
    val id: String,
    val title: String,
    val preview: String,
    val timeLabel: String,
    val updatedAtMillis: Long = 0L,
    val mode: ConversationModeUi,
    val isPinned: Boolean = false,
    val isActiveRun: Boolean = false,
    val characterName: String? = null,
    val projectId: String = "default-cli-project",
)

@Immutable
enum class ConversationModeUi {
    Chat,
    PhoneAgent,
    Terminal,
    Automation,
}
