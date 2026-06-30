package com.wirewaypro.app.ui.theme

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

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

/**
 * @param dynamicColor opt into Material You wallpaper-based color on Android 12+.
 *   Defaults to false: Wireway ships a deliberate dark brand palette (mirrors the
 *   web app), so dynamic color is available but off by default. When enabled we
 *   stay dark — [dynamicDarkColorScheme] — so the app never flips to a light UI.
 */
@Composable
fun WirewayTheme(
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            dynamicDarkColorScheme(context)
        else -> WirewayColorScheme
    }
    val families = remember { BrandFonts.resolve(context) }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = wirewayTypography(families.display, families.body),
        shapes = WirewayShapes,
        content = content,
    )
}
