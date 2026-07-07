package com.wirewaypro.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.wirewaypro.app.ui.theme.GradientBlue
import com.wirewaypro.app.ui.theme.GradientPurple
import com.wirewaypro.app.ui.theme.MotionTokens

/**
 * An animated circular progress ring — the mockup's signature gauge. The arc
 * springs to its new value (interruptible, no jump-cuts when data refreshes),
 * sweeps a blue→purple brand gradient with rounded caps, and leaves the center
 * free for content ("72%", a fill verdict, an icon).
 *
 * Deliberately Canvas-only: one arc + one track per frame, no recomposition
 * during the spring beyond the animated fraction — cheap enough for a list.
 *
 * @param progress fraction in 0..1 (values outside are clamped).
 * @param tint overrides the gradient with a solid color (e.g. success green when
 *   a conduit-fill result passes, error red when it fails).
 */
@Composable
fun ProgressRing(
    progress: Float,
    modifier: Modifier = Modifier,
    size: Dp = 72.dp,
    strokeWidth: Dp = 8.dp,
    tint: Color? = null,
    trackColor: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f),
    content: (@Composable () -> Unit)? = null,
) {
    val fraction by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = MotionTokens.springGentle(),
        label = "ring-progress",
    )

    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size)) {
            val stroke = strokeWidth.toPx()
            val inset = stroke / 2f
            val arcSize = Size(this.size.width - stroke, this.size.height - stroke)
            val topLeft = Offset(inset, inset)

            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )

            if (fraction > 0f) {
                val sweep = 360f * fraction
                // Rotate so the sweep gradient's seam sits at the arc start (12
                // o'clock) instead of slicing through the middle of the arc.
                rotate(degrees = -90f) {
                    val brush = if (tint != null) {
                        Brush.sweepGradient(listOf(tint, tint))
                    } else {
                        Brush.sweepGradient(
                            0f to GradientBlue,
                            (fraction * 0.75f) to GradientPurple,
                            1f to GradientBlue,
                        )
                    }
                    drawArc(
                        brush = brush,
                        startAngle = 0f,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = stroke, cap = StrokeCap.Round),
                    )
                }
            }
        }
        if (content != null) content()
    }
}
