package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val HighDensityColorScheme = lightColorScheme(
    primary = HighDensityIndigo,
    onPrimary = Color.White,
    primaryContainer = HighDensityIndigoLight,
    onPrimaryContainer = HighDensityIndigo,
    secondary = HighDensityAmber,
    onSecondary = Color.White,
    secondaryContainer = HighDensityAmberLight,
    onSecondaryContainer = HighDensityAmber,
    tertiary = HighDensityGreen,
    onTertiary = Color.White,
    tertiaryContainer = HighDensityGreenLight,
    onTertiaryContainer = HighDensityGreen,
    background = HighDensityBg,
    onBackground = TextPrimary,
    surface = HighDensitySurface,
    onSurface = TextPrimary,
    surfaceVariant = HighDensitySurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = BorderColor,
    error = HighDensityRose,
    onError = Color.White,
    errorContainer = HighDensityRoseLight,
    onErrorContainer = HighDensityRose
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = HighDensityColorScheme,
        typography = Typography,
        content = content
    )
}
