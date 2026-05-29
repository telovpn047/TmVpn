package com.telo.wg.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val TeloGreen = Color(0xFF4CAF50)
val TeloGreenDark = Color(0xFF388E3C)
val TeloGreenLight = Color(0xFFC8E6C9)

private val DarkColors = darkColorScheme(
    primary = TeloGreen,
    onPrimary = Color.White,
    secondary = TeloGreenDark,
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E),
)

private val LightColors = lightColorScheme(
    primary = TeloGreen,
    onPrimary = Color.White,
    secondary = TeloGreenDark,
    background = Color(0xFFF5F5F5),
    surface = Color.White,
)

@Composable
fun TeloWGTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content
    )
}
