package com.cortex.ai.ui.cortex

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class ToolCardStatus {
    RUNNING,
    SUCCESS,
    ERROR,
}

@Composable
fun CortexToolCard(
    toolName: String,
    status: ToolCardStatus,
    command: String? = null,
    output: String? = null,
    durationMs: Long? = null,
    modifier: Modifier = Modifier,
) {
    val palette = CortexTheme.palette
    var expanded by remember { mutableStateOf(status == ToolCardStatus.RUNNING) }
    val arrowRotation by animateFloatAsState(
        targetValue = if (expanded) 90f else 0f,
        animationSpec = tween(durationMillis = 260),
        label = "tool_arrow_rotation",
    )

    val icon: ImageVector = when (toolName.lowercase()) {
        "terminal", "bash", "shell" -> LucideIcons.Terminal
        "screen", "mirage", "android" -> LucideIcons.Bot
        "search", "web" -> LucideIcons.Search
        "settings" -> LucideIcons.Settings
        else -> LucideIcons.Sliders
    }

    val statusIcon = when (status) {
        ToolCardStatus.RUNNING -> null
        ToolCardStatus.SUCCESS -> Icons.Rounded.CheckCircle
        ToolCardStatus.ERROR -> Icons.Rounded.Error
    }

    val statusColor = when (status) {
        ToolCardStatus.RUNNING -> palette.primary
        ToolCardStatus.SUCCESS -> palette.secondary
        ToolCardStatus.ERROR -> palette.error
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(palette.surfaceHigh)
            .border(1.dp, palette.outlineSoft, RoundedCornerShape(12.dp))
            .animateContentSize(animationSpec = tween(260))
            .clickable { expanded = !expanded }
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = palette.primary,
                modifier = Modifier.size(18.dp),
            )

            if (status == ToolCardStatus.RUNNING) {
                CortexShimmerText(
                    text = "$toolName...",
                    modifier = Modifier.weight(1f),
                )
            } else {
                Text(
                    text = toolName,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium,
                        color = palette.onSurface,
                    ),
                    modifier = Modifier.weight(1f),
                )
            }

            if (durationMs != null && status != ToolCardStatus.RUNNING) {
                Text(
                    text = "${durationMs}ms",
                    style = MaterialTheme.typography.labelSmall.copy(color = palette.onSurfaceVariant),
                )
            }

            if (statusIcon != null) {
                Icon(
                    imageVector = statusIcon,
                    contentDescription = null,
                    tint = statusColor,
                    modifier = Modifier.size(16.dp),
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = palette.onSurfaceVariant,
                modifier = Modifier
                    .size(16.dp)
                    .graphicsLayer { rotationZ = arrowRotation },
            )
        }

        AnimatedVisibility(
            visible = expanded && (!command.isNullOrBlank() || !output.isNullOrBlank()),
            enter = expandVertically(tween(260)) + fadeIn(tween(200)),
            exit = shrinkVertically(tween(200)) + fadeOut(tween(150)),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (!command.isNullOrBlank()) {
                    Text(
                        text = "Input",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = palette.onSurfaceVariant,
                        ),
                    )
                    SelectionContainer {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(palette.surfaceHigher)
                                .padding(8.dp)
                                .horizontalScroll(rememberScrollState()),
                        ) {
                            Text(
                                text = command,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp,
                                    color = palette.onSurface,
                                ),
                            )
                        }
                    }
                }

                if (!output.isNullOrBlank()) {
                    Text(
                        text = "Output",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = palette.onSurfaceVariant,
                        ),
                    )
                    SelectionContainer {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(palette.surfaceHigher)
                                .padding(8.dp)
                                .horizontalScroll(rememberScrollState()),
                        ) {
                            Text(
                                text = output,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp,
                                    color = palette.onSurface,
                                ),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CortexShimmerText(
    text: String,
    modifier: Modifier = Modifier,
) {
    val palette = CortexTheme.palette
    val transition = rememberInfiniteTransition(label = "shimmer_transition")
    val offset by transition.animateFloat(
        initialValue = -300f,
        targetValue = 300f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 2000
                300f at 1600 using LinearEasing
                300f at 2000
            },
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmer_offset",
    )

    val brush = Brush.linearGradient(
        colors = listOf(
            palette.onSurfaceVariant.copy(alpha = 0.4f),
            palette.primary,
            palette.onSurfaceVariant.copy(alpha = 0.4f),
        ),
        start = Offset(offset - 100f, 0f),
        end = Offset(offset + 100f, 0f),
    )

    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium.copy(
            fontWeight = FontWeight.Medium,
            brush = brush,
        ),
        modifier = modifier,
    )
}
