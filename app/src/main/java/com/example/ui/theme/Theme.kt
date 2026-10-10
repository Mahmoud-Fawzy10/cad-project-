package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = CadCyan,
    onPrimary = CadBackgroundDark,
    primaryContainer = CadSurfaceVariant,
    onPrimaryContainer = CadCyan,
    secondary = CadOrange,
    onSecondary = CadBackgroundDark,
    secondaryContainer = CadSurfaceVariant,
    onSecondaryContainer = CadOrange,
    background = CadBackgroundDark,
    onBackground = CadTextPrimary,
    surface = CadSurfaceDark,
    onSurface = CadTextPrimary,
    surfaceVariant = CadSurfaceVariant,
    onSurfaceVariant = CadTextSecondary,
    outline = CadBorder,
    error = CadRed
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
