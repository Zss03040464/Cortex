package com.cortex.ai.ui.cortex

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle

val EtaBuiltinSlashCommands = listOf(
    "compact" to "Manually compact the session context",
    "mirage" to "Launch or manage Mirage virtual display",
    "screen" to "Capture and observe virtual screen",
    "terminal" to "Switch to Debian terminal console",
    "skills" to "View and toggle active agent skills",
    "clear" to "Clear conversation history",
)

data class SlashCommandSuggestion(
    val command: String,
    val description: String,
    val icon: SlashCommandIcon = SlashCommandIcon.Command,
    val argumentHint: String = "",
)

enum class SlashCommandIcon { Command, Skill, Extension }

fun slashCommandSuggestions(
    input: String,
    customCommands: List<SlashCommandSuggestion> = emptyList(),
): List<SlashCommandSuggestion> {
    if (input.isEmpty() || input.first() != '/') return emptyList()
    val commandToken = input.drop(1).takeWhile { !it.isWhitespace() }
    val exactCommand = EtaBuiltinSlashCommands.any { it.first.equals(commandToken, ignoreCase = true) }
    if (input.drop(1 + commandToken.length).trimStart().isNotEmpty() && exactCommand) return emptyList()
    val prefix = commandToken.lowercase()
    return (EtaBuiltinSlashCommands.map { (name, description) ->
        SlashCommandSuggestion("/$name", description)
    } + customCommands.map { it.copy(icon = SlashCommandIcon.Extension) })
        .filter {
            val candidate = it.command.removePrefix("/").lowercase()
            candidate.startsWith(prefix) || prefix.startsWith(candidate)
        }
        .distinctBy { it.command.lowercase() }
        .take(50)
}

fun slashDisplayName(command: String): String = command.removePrefix("/")
    .replace('-', ' ')
    .replace(':', ' ')
    .trim()
    .replaceFirstChar { it.titlecase() }

fun slashHighlightedName(command: String, input: String): AnnotatedString = buildAnnotatedString {
    val label = slashDisplayName(command)
    val query = input.drop(1).takeWhile { !it.isWhitespace() }.replace('-', ' ')
    val match = label.lowercase().indexOf(query.lowercase().trim())
    if (match >= 0 && query.isNotBlank()) {
        append(label.substring(0, match))
        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
            append(label.substring(match, (match + query.length).coerceAtMost(label.length)))
        }
        append(label.drop((match + query.length).coerceAtMost(label.length)))
    } else append(label)
}
