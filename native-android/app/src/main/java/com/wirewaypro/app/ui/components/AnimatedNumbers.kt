package com.wirewaypro.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import com.wirewaypro.app.ui.theme.MotionTokens
import com.wirewaypro.app.ui.util.Format

/**
 * Money that counts to its value — the S+ upgrade over a static figure. On first
 * appearance it counts up from zero (the "your money arriving" moment); on later
 * data changes it glides from the previous value, so a synced refresh reads as an
 * update rather than a reload.
 *
 * The count itself is a tween, not a spring: money must land exactly on the real
 * figure with no overshoot — a total that momentarily reads high is a lie.
 */
@Composable
fun AnimatedMoneyText(
    value: Double,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.headlineSmall,
    color: Color = MaterialTheme.colorScheme.onSurface,
    fontWeight: FontWeight? = FontWeight.Bold,
    durationMillis: Int = MotionTokens.deliberate,
) {
    val shown by animateFloatAsState(
        targetValue = value.toFloat(),
        animationSpec = tween(durationMillis, easing = MotionTokens.emphasized),
        label = "money-count",
    )
    Text(
        text = Format.money(shown.toDouble()),
        style = style,
        color = color,
        fontWeight = fontWeight,
        maxLines = 1,
        modifier = modifier,
    )
}

/**
 * A plain number (hours, counts, percentages) that glides to its value. Renders
 * via [format] so callers control precision — e.g. `{ "%.1f%%".format(it) }`.
 */
@Composable
fun AnimatedNumberText(
    value: Double,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.headlineSmall,
    color: Color = MaterialTheme.colorScheme.onSurface,
    fontWeight: FontWeight? = FontWeight.Bold,
    durationMillis: Int = MotionTokens.deliberate,
    format: (Double) -> String = { v -> v.toLong().toString() },
) {
    val shown by animateFloatAsState(
        targetValue = value.toFloat(),
        animationSpec = tween(durationMillis, easing = MotionTokens.emphasized),
        label = "number-count",
    )
    Text(
        text = format(shown.toDouble()),
        style = style,
        color = color,
        fontWeight = fontWeight,
        maxLines = 1,
        modifier = modifier,
    )
}

/**
 * True once per composition lifetime after the first frame — lets a screen play
 * its entrance choreography exactly once (not again on every recomposition).
 */
@Composable
fun rememberPlayedOnce(): Boolean {
    var played by remember { mutableStateOf(false) }
    androidx.compose.runtime.LaunchedEffect(Unit) { played = true }
    return played
}
