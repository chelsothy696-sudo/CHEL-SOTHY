package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val ThyCutDarkColorScheme = darkColorScheme(
    primary = StudioCyan,
    onPrimary = StudioBg,
    primaryContainer = StudioCyanDim.copy(alpha = 0.25f),
    onPrimaryContainer = StudioCyan,
    secondary = StudioCoral,
    onSecondary = StudioTextPrimary,
    secondaryContainer = StudioCoral.copy(alpha = 0.22f),
    onSecondaryContainer = StudioCoral,
    tertiary = StudioViolet,
    onTertiary = StudioTextPrimary,
    tertiaryContainer = StudioViolet.copy(alpha = 0.22f),
    onTertiaryContainer = StudioViolet,
    background = StudioBg,
    onBackground = StudioTextPrimary,
    surface = StudioSurface,
    onSurface = StudioTextPrimary,
    surfaceVariant = StudioSurfaceVariant,
    onSurfaceVariant = StudioTextSecondary,
    surfaceContainerHigh = StudioSurfaceElevated,
    outline = StudioBorder,
    outlineVariant = StudioBorder.copy(alpha = 0.6f),
    error = StudioError,
    onError = StudioTextPrimary
)

val ThyCutShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun ThyCutTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = ThyCutDarkColorScheme,
        typography = Typography,
        shapes = ThyCutShapes,
        content = content
    )
}
