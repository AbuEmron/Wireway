package com.wirewaypro.app.ui.theme

import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape

/**
 * The brand's signature blue→purple gradients. These carry hero surfaces and the
 * primary CTA so the accent identity stays consistent across both skins.
 */
object BrandGradients {

    /** Diagonal blue→purple, the default for hero panels and primary buttons. */
    val primary: Brush
        get() = Brush.linearGradient(
            colors = listOf(GradientBlue, GradientPurple),
            start = Offset(0f, 0f),
            end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY),
        )

    /** Builds the primary gradient with an animated angle for the hero shimmer. */
    fun animated(progress: Float): Brush {
        // progress in [0,1] sweeps the gradient origin around the panel.
        val span = 1200f
        val shift = progress * span
        return Brush.linearGradient(
            colors = listOf(GradientBlue, BrandPurple, GradientPurple),
            start = Offset(-span + shift, 0f),
            end = Offset(shift, span),
        )
    }

    /** A soft radial glow used behind the logo / accent highlights. */
    val glow: Brush
        get() = Brush.radialGradient(
            colors = listOf(GradientPurple.copy(alpha = 0.55f), GradientBlue.copy(alpha = 0f)),
        )
}

/** Convenience: paint a Modifier with the static brand gradient. */
fun Modifier.brandGradient(shape: Shape): Modifier =
    this.background(brush = BrandGradients.primary, shape = shape)
