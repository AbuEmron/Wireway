package com.wirewaypro.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.wirewaypro.app.R

/**
 * The Wireway mark — the blue→cyan ring, blue→purple "W", and cyan lightning bolt —
 * rendered from the [R.drawable.ic_wireway_logo] VectorDrawable so it matches the brand
 * mockup and the launcher icon exactly. Designed on a dark surface; prefer
 * [WirewayLogoBadge] on light backgrounds.
 */
@Composable
fun WirewayLogomark(
    modifier: Modifier = Modifier,
    size: Dp = 28.dp,
) {
    Image(
        painter = painterResource(R.drawable.wireway_icon),
        contentDescription = "Wireway",
        modifier = modifier.size(size),
    )
}

/**
 * The mark inside the dark-navy rounded badge — the app/brand lockup that mirrors the
 * launcher icon. Works on any background.
 */
@Composable
fun WirewayLogoBadge(
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
) {
    // The brand icon PNG already carries its own rounded blue background, so it
    // renders directly here and matches the launcher icon exactly.
    Image(
        painter = painterResource(R.drawable.wireway_icon),
        contentDescription = "Wireway",
        modifier = modifier.size(size),
    )
}
