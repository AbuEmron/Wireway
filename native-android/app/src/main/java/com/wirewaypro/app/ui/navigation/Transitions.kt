package com.wirewaypro.app.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.navigation.NavBackStackEntry
import com.wirewaypro.app.ui.theme.MotionTokens

/**
 * The app's screen-transition language, applied at the NavHost so every screen
 * moves the same way without per-screen wiring:
 *
 *  - **Push/pop** ([enter]/[exit]/[popEnter]/[popExit]): Material shared-axis X —
 *    the new screen slides in over a quarter width while the old one recedes an
 *    eighth, so navigation reads as depth, not a page flip. Because pop mirrors
 *    push, Navigation-Compose drives these same specs from the predictive-back
 *    gesture: the outgoing screen tracks the finger and settles either way.
 *
 *  - **Tab switches** ([tabEnter]/[tabExit]): fade-through with a whisper of
 *    scale — sibling surfaces swap in place rather than implying hierarchy.
 */
object WirewayTransitions {
    private const val PUSH_MS = 320
    private const val FADE_MS = 180

    val enter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        slideInHorizontally(
            animationSpec = tween(PUSH_MS, easing = MotionTokens.emphasized),
        ) { it / 4 } + fadeIn(tween(PUSH_MS))
    }

    val exit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        slideOutHorizontally(
            animationSpec = tween(PUSH_MS, easing = MotionTokens.emphasized),
        ) { -it / 8 } + fadeOut(tween(FADE_MS))
    }

    val popEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        slideInHorizontally(
            animationSpec = tween(PUSH_MS, easing = MotionTokens.emphasized),
        ) { -it / 8 } + fadeIn(tween(PUSH_MS))
    }

    val popExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        slideOutHorizontally(
            animationSpec = tween(PUSH_MS, easing = MotionTokens.emphasized),
        ) { it / 4 } + fadeOut(tween(FADE_MS))
    }

    val tabEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        fadeIn(tween(210)) + scaleIn(
            initialScale = 0.985f,
            animationSpec = tween(210, easing = MotionTokens.emphasized),
        )
    }

    val tabExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        fadeOut(tween(90))
    }
}
