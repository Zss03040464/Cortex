package com.cortex.ai.ui.cortex

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

data class CortexPalette(
    val background: Color,
    val backgroundGradientTop: Color,
    val settingsBackground: Color,
    val sidebarBackground: Color,
    val sidebarControl: Color,
    val settingsIcon: Color,
    val surface: Color,
    val surfaceHigh: Color,
    val surfaceHigher: Color,
    val surfaceVariant: Color,
    val outline: Color,
    val outlineSoft: Color,
    val onSurface: Color,
    val onSurfaceVariant: Color,
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val secondary: Color,
    val onSecondary: Color,
    val secondaryContainer: Color,
    val onSecondaryContainer: Color,
    val tertiary: Color,
    val error: Color,
    val messageBubble: Color,
    val scrim: Color,
)

val LightCortexPalette = CortexPalette(
    background = Color(0xFFF8FAFC),
    backgroundGradientTop = Color(0xFFFFFFFF),
    settingsBackground = Color(0xFFF1F5F9),
    sidebarBackground = Color(0xFFF8FAFC),
    sidebarControl = Color(0xFFE2E8F0),
    settingsIcon = Color(0xFF0F172A),
    surface = Color(0xFFFFFFFF),
    surfaceHigh = Color(0xFFF1F5F9),
    surfaceHigher = Color(0xFFE2E8F0),
    surfaceVariant = Color(0xFFCBD5E1),
    outline = Color(0xFF94A3B8),
    outlineSoft = Color(0xFFE2E8F0),
    onSurface = Color(0xFF0F172A),
    onSurfaceVariant = Color(0xFF475569),
    primary = Color(0xFF0284C7),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE0F2FE),
    onPrimaryContainer = Color(0xFF0369A1),
    secondary = Color(0xFF0891B2),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFCFFAFE),
    onSecondaryContainer = Color(0xFF155E75),
    tertiary = Color(0xFF6366F1),
    error = Color(0xFFEF4444),
    messageBubble = Color(0xFFE0F2FE),
    scrim = Color(0x22000000),
)

val DarkCortexPalette = CortexPalette(
    background = Color(0xFF090D16),
    backgroundGradientTop = Color(0xFF0F172A),
    settingsBackground = Color(0xFF090D16),
    sidebarBackground = Color(0xFF0D131F),
    sidebarControl = Color(0xFF131D31),
    settingsIcon = Color(0xFFE2E8F0),
    surface = Color(0xFF0F172A),
    surfaceHigh = Color(0xFF162238),
    surfaceHigher = Color(0xFF1E2D4A),
    surfaceVariant = Color(0xFF243656),
    outline = Color(0xFF1E3A5F),
    outlineSoft = Color(0xFF17283C),
    onSurface = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFF94A3B8),
    primary = Color(0xFF38BDF8),
    onPrimary = Color(0xFF082F49),
    primaryContainer = Color(0xFF0C4A6E),
    onPrimaryContainer = Color(0xFFE0F2FE),
    secondary = Color(0xFF06B6D4),
    onSecondary = Color(0xFF083344),
    secondaryContainer = Color(0xFF164E63),
    onSecondaryContainer = Color(0xFFCFFAFE),
    tertiary = Color(0xFF818CF8),
    error = Color(0xFFF87171),
    messageBubble = Color(0xFF0E2A47),
    scrim = Color(0x66000000),
)

val LightHighContrastCortexPalette = LightCortexPalette.copy(
    outline = Color(0xFF64748B),
    outlineSoft = Color(0xFF94A3B8),
    onSurface = Color(0xFF020617),
    onSurfaceVariant = Color(0xFF334155),
    primary = Color(0xFF0369A1),
    primaryContainer = Color(0xFFBAE6FD),
    onPrimaryContainer = Color(0xFF0C4A6E),
    error = Color(0xFFDC2626),
)

val DarkHighContrastCortexPalette = DarkCortexPalette.copy(
    outline = Color(0xFF38BDF8),
    outlineSoft = Color(0xFF0284C7),
    onSurface = Color(0xFFFFFFFF),
    onSurfaceVariant = Color(0xFFCBD5E1),
    primary = Color(0xFF7DD3FC),
    primaryContainer = Color(0xFF075985),
    onPrimaryContainer = Color(0xFFFFFFFF),
    error = Color(0xFFFCA5A5),
)

val LocalCortexPalette = compositionLocalOf { LightCortexPalette }

object CortexTheme {
    val palette: CortexPalette
        @Composable
        @ReadOnlyComposable
        get() = LocalCortexPalette.current
}

val CortexPalette.cardBorder: Color get() = outlineSoft
val CortexPalette.surfaceSecondary: Color get() = surfaceHigh
val CortexPalette.textPrimary: Color get() = onSurface
val CortexPalette.textSecondary: Color get() = onSurfaceVariant
val CortexPalette.textTertiary: Color get() = onSurfaceVariant.copy(alpha = 0.7f)
