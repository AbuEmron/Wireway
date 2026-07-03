package com.wirewaypro.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.wirewaypro.app.R

/**
 * The W-cable mark — the silver + blue "W" whose last stroke becomes a cut cable
 * with exposed conductors — on a transparent background. The first stroke is
 * near-white, so this variant is for DARK or brand-gradient surfaces only;
 * on possibly-light surfaces use [WirewayLogoBadge].
 */
@Composable
fun WirewayLogomark(
    modifier: Modifier = Modifier,
    size: Dp = 28.dp,
) {
    Image(
        painter = painterResource(R.drawable.wireway_mark),
        contentDescription = "Wireway",
        modifier = modifier.size(size),
    )
}

/**
 * The full brand tile — the W-cable mark and "WIREWAY PRO" wordmark on the dark
 * rounded tile, matching the launcher icon. Self-contained, so it works on any
 * background (the lockup of choice for light surfaces).
 */
@Composable
fun WirewayLogoBadge(
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
) {
    Image(
        painter = painterResource(R.drawable.wireway_tile),
        contentDescription = "Wireway",
        modifier = modifier.size(size),
    )
}
