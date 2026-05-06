package com.zeus.lineagent.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LineGreen = Color(0xFF06C755)
private val LineGreenDark = Color(0xFF03A845)
private val Navy = Color(0xFF0F1B2D)

private val LightColors = lightColorScheme(
    primary = LineGreen,
    onPrimary = Color.White,
    secondary = Navy,
    onSecondary = Color.White,
)

private val DarkColors = darkColorScheme(
    primary = LineGreenDark,
    onPrimary = Color.White,
    secondary = Color(0xFFB0BEC5),
    onSecondary = Navy,
)

@Composable
fun ZeusTheme(content: @Composable () -> Unit) {
    val colors = if (isSystemInDarkTheme()) DarkColors else LightColors
    MaterialTheme(colorScheme = colors, content = content)
}
