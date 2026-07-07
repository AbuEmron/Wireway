package com.wirewaypro.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Extended semantic colors that Material's [androidx.compose.material3.ColorScheme]
 * doesn't carry: success, warning, and info — each with a matching "on" ink and a
 * tinted container for chips/banners. Danger already lives in the scheme as `error`.
 *
 * These are skin-aware (light/dark) and provided via [LocalExtendedColors] by
 * [WirewayTheme], so a status chip reads correctly in a sunlit truck cab and in a
 * dark panel. Read them with `MaterialTheme.extended`.
 */
@Immutable
data class ExtendedColors(
    val success: Color,
    val onSuccess: Color,
    val successContainer: Color,
    val onSuccessContainer: Color,
    val warning: Color,
    val onWarning: Color,
    val warningContainer: Color,
    val onWarningContainer: Color,
    val info: Color,
    val onInfo: Color,
    val infoContainer: Color,
    val onInfoContainer: Color,
)

/** Info hue — a sky blue, distinct from the brand primary so "info" ≠ "action". */
private val InfoBlue = Color(0xFF0EA5E9)

val LightExtendedColors = ExtendedColors(
    success = BrandGreen,
    onSuccess = Color.White,
    successContainer = Color(0xFFDCFCE7),
    onSuccessContainer = Color(0xFF064E2B),
    warning = BrandAmber,
    onWarning = Color(0xFF3A2600),
    warningContainer = Color(0xFFFFF1D6),
    onWarningContainer = Color(0xFF5A3B00),
    info = InfoBlue,
    onInfo = Color.White,
    infoContainer = Color(0xFFDCF1FB),
    onInfoContainer = Color(0xFF06364A),
)

val DarkExtendedColors = ExtendedColors(
    success = Color(0xFF4ADE80),
    onSuccess = Color(0xFF042713),
    successContainer = Color(0xFF14351F),
    onSuccessContainer = Color(0xFFBBF7D0),
    warning = Color(0xFFFBBF4A),
    onWarning = Color(0xFF2E1E00),
    warningContainer = Color(0xFF3A2B08),
    onWarningContainer = Color(0xFFFDE7BC),
    info = Color(0xFF56C7F5),
    onInfo = Color(0xFF03212F),
    infoContainer = Color(0xFF0C2E3D),
    onInfoContainer = Color(0xFFC4EBFB),
)

/** Provided by [WirewayTheme]; defaults to the light skin outside a theme. */
val LocalExtendedColors = staticCompositionLocalOf { LightExtendedColors }

/** Accessor mirroring `MaterialTheme.colorScheme`: `MaterialTheme.extended.success`. */
val MaterialTheme.extended: ExtendedColors
    @Composable
    @ReadOnlyComposable
    get() = LocalExtendedColors.current
