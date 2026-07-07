package com.wirewaypro.app.ui.components

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import com.wirewaypro.app.ui.theme.MotionTokens

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

/**
 * The richer haptic vocabulary, backed by the View layer (Compose's
 * [HapticFeedbackType] only carries two patterns on this Compose version).
 * Every effect degrades gracefully on older APIs, and all of them respect the
 * user's system haptics setting.
 *
 * Vocabulary: [tap] for any button/row press; [tick] for discrete steps
 * (segmented tabs, sliders, steppers); [confirm] when something real succeeded
 * (saved, sent, calculated PASS); [reject] for a blocked or failing action.
 */
class WirewayHaptics(private val view: View) {
    /** Standard press acknowledgement. */
    fun tap() {
        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
    }

    /** A light tick for scrubbing across discrete values. */
    fun tick() {
        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
    }

    /** Positive completion — save, send, PASS. */
    fun confirm() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
        } else {
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        }
    }

    /** Negative outcome — validation failure, FAIL result, blocked action. */
    fun reject() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            view.performHapticFeedback(HapticFeedbackConstants.REJECT)
        } else {
            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        }
    }

    /** A heavier thud for big moments (estimate completed, reward overlays). */
    fun heavy() {
        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
    }
}

/** Accessor for the richer haptic vocabulary: `val haptics = rememberWirewayHaptics()`. */
@Composable
fun rememberWirewayHaptics(): WirewayHaptics {
    val view = LocalView.current
    return remember(view) { WirewayHaptics(view) }
}

/**
 * Entrance stagger: fades + rises a block once on first composition, delayed by
 * [index] so successive sections choreograph. Pure graphicsLayer — hidden
 * blocks keep their layout, so nothing reflows as they arrive.
 */
@Composable
fun Modifier.riseIn(index: Int = 0): Modifier {
    var on by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { on = true }
    val t by animateFloatAsState(
        targetValue = if (on) 1f else 0f,
        animationSpec = tween(
            durationMillis = 340,
            delayMillis = index * 55,
            easing = MotionTokens.emphasized,
        ),
        label = "rise-in",
    )
    return graphicsLayer {
        alpha = t
        translationY = (1f - t) * 28f
    }
}
