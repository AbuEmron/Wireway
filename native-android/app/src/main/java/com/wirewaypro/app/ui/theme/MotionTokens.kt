package com.wirewaypro.app.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween

/**
 * Motion tokens — durations, easings, and springs so animation feels like one hand
 * moved it.
 *
 * Field rationale: motion is tuned *short*. A distracted electrician on a ladder
 * should never wait on a transition, and long flourishes read as lag on mid-range
 * hardware. [emphasized] is the standard curve — a quick, confident settle with no
 * bounce. Use these instead of ad-hoc `tween(300)` sprinkled across screens.
 *
 * Springs are the physics side of the system: interruptible and velocity-aware, so
 * a gesture that reverses mid-flight settles naturally instead of snapping. Use
 * [springSnappy] for press/selection feedback, [springBouncy] where a touch of
 * life is wanted (FAB, tab icons, reveals), [springGentle] for layout/size morphs.
 */
object MotionTokens {
    // ── Durations (ms) ──────────────────────────────────────────────────────────
    /** 90ms — instant feedback: press states, chip toggles. */
    const val quick: Int = 90
    /** 180ms — the default for most UI transitions. */
    const val standard: Int = 180
    /** 280ms — larger surfaces entering (sheets, hero reveals). */
    const val slow: Int = 280
    /** 900ms — hero count-ups and deliberate emphasis. */
    const val deliberate: Int = 900

    // ── Easings ─────────────────────────────────────────────────────────────────
    /** Confident settle with a touch of overshoot-free deceleration. */
    val emphasized: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    /** Symmetric ease for reversible states (toggles, tab indicators). */
    val standardEasing: Easing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)

    // ── Ready-made tween specs ──────────────────────────────────────────────────
    fun <T> quickSpec(): FiniteAnimationSpec<T> = tween(quick, easing = standardEasing)
    fun <T> standardSpec(): FiniteAnimationSpec<T> = tween(standard, easing = emphasized)
    fun <T> slowSpec(): FiniteAnimationSpec<T> = tween(slow, easing = emphasized)

    // ── Springs (physics; interruptible, velocity-aware) ───────────────────────
    /** Crisp, near-critically-damped — press scales, selection indicators. */
    fun <T> springSnappy(): SpringSpec<T> =
        spring(dampingRatio = 0.9f, stiffness = 700f)

    /** A single confident overshoot — FAB pops, tab icons, playful reveals. */
    fun <T> springBouncy(): SpringSpec<T> =
        spring(dampingRatio = 0.65f, stiffness = 420f)

    /** No overshoot, softer settle — content size morphs, expanding cards. */
    fun <T> springGentle(): SpringSpec<T> =
        spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 320f)
}
