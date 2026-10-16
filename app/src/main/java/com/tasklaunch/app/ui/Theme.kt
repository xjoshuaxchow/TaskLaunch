package com.tasklaunch.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Teal = Color(0xFF123C3A)
private val TealLight = Color(0xFF8ED0CC)
private val Amber = Color(0xFFC9842A)
private val Paper = Color(0xFFF6F3EC)
private val Ink = Color(0xFF1C1B19)

private val LightColors = lightColorScheme(
    primary = Teal,
    onPrimary = Color.White,
    secondary = Amber,
    onSecondary = Color(0xFF2A1C08),
    background = Paper,
    onBackground = Ink,
    surface = Color.White,
    onSurface = Ink,
    surfaceVariant = Color(0xFFE7E1D6),
    onSurfaceVariant = Color(0xFF4E4942),
)

private val DarkColors = darkColorScheme(
    primary = TealLight,
    onPrimary = Color(0xFF083230),
    secondary = Color(0xFFF4C95D),
    onSecondary = Color(0xFF2A1C08),
    background = Color(0xFF121411),
    onBackground = Color(0xFFE8E4DC),
    surface = Color(0xFF1C211F),
    onSurface = Color(0xFFE8E4DC),
    surfaceVariant = Color(0xFF2A312E),
    onSurfaceVariant = Color(0xFFC9C2B6),
)

@Composable
fun TaskLaunchTheme(content: @Composable () -> Unit) {
    val dark = androidx.compose.foundation.isSystemInDarkTheme()
    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        content = content,
    )
}
