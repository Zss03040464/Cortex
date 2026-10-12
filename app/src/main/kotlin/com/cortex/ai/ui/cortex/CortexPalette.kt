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
    background = Color(0xFFFFFFFF),
    backgroundGradientTop = Color(0xFFFFFFFF),
    settingsBackground = Color(0xFFF2F2F7),
    sidebarBackground = Color(0xFFF9F9F9),
    sidebarControl = Color(0xFFF3F3F2),
    settingsIcon = Color(0xFF000000),
    surface = Color(0xFFFFFFFF),
    surfaceHigh = Color(0xFFF3F3F2),
    surfaceHigher = Color(0xFFECECEC),
    surfaceVariant = Color(0xFFE5E5E5),
    outline = Color(0xFFD9D9D9),
    outlineSoft = Color(0xFFE7E7E7),
    onSurface = Color(0xFF202123),
    onSurfaceVariant = Color(0xFF6B6B6B),
    primary = Color(0xFFAD7BF9),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFF1E5FF),
    onPrimaryContainer = Color(0xFF4D2F8E),
    secondary = Color(0xFF4A7B6B),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFDDF1E8),
    onSecondaryContainer = Color(0xFF1E4A3B),
    tertiary = Color(0xFF9A7DF8),
    error = Color(0xFFB43E3E),
    messageBubble = Color(0xFFF0E3FF),
    scrim = Color(0x22000000),
)

val DarkCortexPalette = CortexPalette(
    background = Color(0xFF151619),
    backgroundGradientTop = Color(0xFF1B1D22),
    settingsBackground = Color(0xFF151619),
    sidebarBackground = Color(0xFF1C1F23),
    sidebarControl = Color(0xFF24282D),
    settingsIcon = Color(0xFFF3F1EC),
    surface = Color(0xFF1C1F23),
    surfaceHigh = Color(0xFF24282D),
    surfaceHigher = Color(0xFF2C3036),
    surfaceVariant = Color(0xFF343941),
    outline = Color(0xFF4A5059),
    outlineSoft = Color(0xFF3D424A),
    onSurface = Color(0xFFF3F1EC),
    onSurfaceVariant = Color(0xFFB9B4AA),
    primary = Color(0xFFC0AEFF),
    onPrimary = Color(0xFF251448),
    primaryContainer = Color(0xFF3A275F),
    onPrimaryContainer = Color(0xFFF0E9FF),
    secondary = Color(0xFF89C8AF),
    onSecondary = Color(0xFF143126),
    secondaryContainer = Color(0xFF24483A),
    onSecondaryContainer = Color(0xFFDDF6EA),
    tertiary = Color(0xFFD1C2FF),
    error = Color(0xFFFF8E8E),
    messageBubble = Color(0xFF32264A),
    scrim = Color(0x66000000),
)

val LightHighContrastCortexPalette = LightCortexPalette.copy(
    outline = Color(0xFFA39E95),
    outlineSoft = Color(0xFFBBB6AC),
    onSurface = Color(0xFF111214),
    onSurfaceVariant = Color(0xFF514D46),
    primary = Color(0xFF6545D6),
    primaryContainer = Color(0xFFE7D5FF),
    onPrimaryContainer = Color(0xFF35186F),
    error = Color(0xFFB43E3E),
)

val DarkHighContrastCortexPalette = DarkCortexPalette.copy(
    outline = Color(0xFF777F89),
    outlineSoft = Color(0xFF656C75),
    onSurface = Color(0xFFFFFFFF),
    onSurfaceVariant = Color(0xFFD2CCC1),
    primary = Color(0xFFD5C8FF),
    primaryContainer = Color(0xFF4C3378),
    onPrimaryContainer = Color(0xFFFFFFFF),
    error = Color(0xFFFFB0B0),
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
