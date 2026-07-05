package com.wirewaypro.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import com.wirewaypro.app.ui.theme.BrandBlue
import com.wirewaypro.app.ui.theme.BrandGreen
import com.wirewaypro.app.ui.theme.BrandPurple
import com.wirewaypro.app.ui.theme.GradientBlue
import com.wirewaypro.app.ui.theme.GradientPurple
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * The reward moment: an electric spark burst in brand colors — dots and short
 * "current" streaks that fire outward, decelerate, drift down, and fade. Plays
 * once per increment of [trigger] (keep a counter in the caller and bump it when
 * the estimate saves/sends); 0 draws nothing, so idle cost is zero.
 *
 * One Animatable drives every particle, so the whole burst is a single
 * animation + one Canvas draw per frame — no per-particle recomposition.
 * Overlay it with `Modifier.matchParentSize()` in a Box above the content;
 * it never intercepts touches.
 */
@Composable
fun SparkBurst(
    trigger: Int,
    modifier: Modifier = Modifier,
    onFinished: () -> Unit = {},
) {
    if (trigger <= 0) return

    val progress = remember(trigger) { Animatable(0f) }
    val particles = remember(trigger) {
        val rng = Random(trigger * 7919)
        List(42) { i ->
            val angle = rng.nextDouble(0.0, 2 * Math.PI)
            Spark(
                dx = cos(angle).toFloat(),
                dy = sin(angle).toFloat() * 0.85f, // slightly flattened: fountain, not sphere
                speed = 0.32f + rng.nextFloat() * 0.55f,
                radius = 2.5f + rng.nextFloat() * 3.5f,
                color = SPARK_COLORS[i % SPARK_COLORS.size],
                streak = i % 4 == 0, // every 4th particle is a current streak
                spin = rng.nextFloat() * 0.6f - 0.3f,
            )
        }
    }

    LaunchedEffect(trigger) {
        progress.snapTo(0f)
        progress.animateTo(1f, tween(durationMillis = 1100, easing = LinearEasing))
        onFinished()
    }

    Canvas(modifier.fillMaxSize()) {
        val p = progress.value
        if (p <= 0f || p >= 1f) return@Canvas
        val center = Offset(size.width / 2f, size.height * 0.42f)
        val reach = size.minDimension * 0.9f
        // Ease-out travel + a soft gravity pull once the burst slows.
        val travel = 1f - (1f - p) * (1f - p)
        val gravity = p * p * size.height * 0.18f
        val alpha = (1f - p).coerceIn(0f, 1f)

        particles.forEach { s ->
            val x = center.x + s.dx * s.speed * reach * travel + s.spin * reach * p * p
            val y = center.y + s.dy * s.speed * reach * travel + gravity
            if (s.streak) {
                // A short streak along the travel direction — the "current" look.
                val tail = 14f * (1f - p) + 4f
                drawLine(
                    color = s.color.copy(alpha = alpha),
                    start = Offset(x - s.dx * tail, y - s.dy * tail),
                    end = Offset(x, y),
                    strokeWidth = 3f,
                    cap = StrokeCap.Round,
                )
            } else {
                drawCircle(
                    color = s.color.copy(alpha = alpha),
                    radius = s.radius * (1f - p * 0.4f),
                    center = Offset(x, y),
                )
            }
        }
    }
}

private class Spark(
    val dx: Float,
    val dy: Float,
    val speed: Float,
    val radius: Float,
    val color: Color,
    val streak: Boolean,
    val spin: Float,
)

private val SPARK_COLORS = listOf(
    GradientBlue,
    BrandPurple,
    Color.White,
    BrandBlue,
    GradientPurple,
    BrandGreen,
)
