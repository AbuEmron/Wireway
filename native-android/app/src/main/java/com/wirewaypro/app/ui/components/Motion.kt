package com.wirewaypro.app.ui.components

import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

/**
 * Animates a number counting up to [target] whenever it changes, e.g. the dashboard
 * hero amount. Returns the current interpolated value to render.
 */
@Composable
fun animatedCount(target: Double, durationMillis: Int = 900): Double {
    val value by animateFloatAsState(
        targetValue = target.toFloat(),
        animationSpec = tween(durationMillis = durationMillis, easing = LinearOutSlowInEasing),
        label = "count-up",
    )
    return value.toDouble()
}

/** A small wrapper so screens can fire a tasteful tap without importing the API. */
fun HapticFeedback.tap() = performHapticFeedback(HapticFeedbackType.LongPress)

/** Composable accessor for the current haptic feedback handler. */
@Composable
fun rememberHaptics(): HapticFeedback = LocalHapticFeedback.current
