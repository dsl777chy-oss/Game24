package com.spx.game24.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

@Immutable
data class GameColors(
    val accent: Color,
    val teal: Color,
    val blue: Color,
    val warm: Color,
    val bgTop: Color,
    val bgBottom: Color,
    val successGreen: Color
)

val LightGameColors = GameColors(
    accent = Color(0xFF7C4DFF),
    teal = Color(0xFF26C6DA),
    blue = Color(0xFF42A5F5),
    warm = Color(0xFFFF7043),
    bgTop = Color(0xFFF7F4FF),
    bgBottom = Color(0xFFEEF9FF),
    successGreen = Color(0xFF2E7D32)
)

val DarkGameColors = GameColors(
    accent = Color(0xFFB388FF),
    teal = Color(0xFF4DD0E1),
    blue = Color(0xFF64B5F6),
    warm = Color(0xFFFF8A65),
    bgTop = Color(0xFF1A1A2E),
    bgBottom = Color(0xFF16213E),
    successGreen = Color(0xFF66BB6A)
)

val LocalGameColors = staticCompositionLocalOf { LightGameColors }
