package com.wirewaypro.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Wireway is a dark-only brand (the web app has no light mode), so we ship a
// single dark scheme rather than reacting to the system setting.
private val WirewayColorScheme = darkColorScheme(
    primary = BrandAccent,
    onPrimary = OnAccent,
    primaryContainer = BrandAccentDeep,
    onPrimaryContainer = TextPrimary,
    secondary = BrandGreen,
    onSecondary = OnAccent,
    background = BrandBackground,
    onBackground = TextPrimary,
    surface = BrandSurface,
    onSurface = TextPrimary,
    surfaceVariant = BrandSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = Color(0xFF2A3A52),
    outlineVariant = Hairline,
    error = ErrorRed,
    onError = Color.White,
)

@Composable
fun WirewayTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = WirewayColorScheme,
        typography = WirewayTypography,
        shapes = WirewayShapes,
        content = content,
    )
}
