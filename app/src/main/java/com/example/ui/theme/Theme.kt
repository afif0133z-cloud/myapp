package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val JinwoLightColorScheme = lightColorScheme(
    primary = JinwoBlue,
    onPrimary = Color.White,
    primaryContainer = JinwoBlueSoft,
    onPrimaryContainer = JinwoBlueDark,
    secondary = JinwoOk,
    onSecondary = Color.White,
    secondaryContainer = JinwoOkSoft,
    onSecondaryContainer = JinwoOk,
    background = JinwoSurface,
    onBackground = JinwoInk,
    surface = JinwoSurface,
    onSurface = JinwoInk,
    surfaceVariant = JinwoBlueSoft,
    onSurfaceVariant = JinwoMuted,
    outline = JinwoBlueBorder,
    error = JinwoError,
    onError = Color.White
)

@Composable
fun JinwoEmiTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = JinwoLightColorScheme,
        typography = JinwoTypography,
        content = content
    )
}
