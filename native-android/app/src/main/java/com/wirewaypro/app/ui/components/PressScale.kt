package com.wirewaypro.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import com.wirewaypro.app.ui.theme.MotionTokens

/**
 * The house press effect: the surface dips to [pressedScale] under the finger and
 * springs back on release — physical, interruptible, and cheap (graphicsLayer
 * only, no recomposition of children).
 *
 * Two ways in:
 *  - [pressScale] with a [MutableInteractionSource], when a Material component
 *    (Card, Button) or an existing `clickable` already owns the interactions —
 *    pass it the SAME source so press states are shared.
 *  - the source-less [pressScale] overload, which observes presses itself via a
 *    non-consuming pointer watcher and stacks cleanly on any clickable.
 */
fun Modifier.pressScale(
    interactionSource: MutableInteractionSource,
    pressedScale: Float = 0.965f,
): Modifier = composed {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = MotionTokens.springSnappy(),
        label = "press-scale",
    )
    graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/** Standalone variant: watches presses itself (never consumes the events). */
fun Modifier.pressScale(pressedScale: Float = 0.965f): Modifier = composed {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = MotionTokens.springSnappy(),
        label = "press-scale",
    )
    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .pointerInput(Unit) {
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false)
                pressed = true
                waitForUpOrCancellation()
                pressed = false
            }
        }
}
