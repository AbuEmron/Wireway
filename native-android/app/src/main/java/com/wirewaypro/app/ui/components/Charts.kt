package com.wirewaypro.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.wirewaypro.app.ui.theme.MotionTokens

/**
 * Chart primitives for the analytics surfaces. Both are Canvas-only (one draw
 * pass per frame, no per-element recomposition) and both animate on real data:
 * the reveal replays when the values change, so a synced refresh reads as the
 * chart updating live rather than repainting.
 *
 * Doctrine note: charts render exactly the numbers the caller passes — no
 * smoothing, no projection. The printed figures stay next to every graphic.
 */
data class DonutSlice(
    val label: String,
    val value: Double,
    val color: Color,
)

/**
 * An animated donut: slices sweep in clockwise from 12 o'clock with rounded
 * caps and a small gap between slices. The center is a free slot (typically
 * the animated total). Zero/negative slices are ignored.
 */
@Composable
fun AnimatedDonutChart(
    slices: List<DonutSlice>,
    modifier: Modifier = Modifier,
    size: Dp = 148.dp,
    strokeWidth: Dp = 18.dp,
    center: (@Composable () -> Unit)? = null,
) {
    val shown = slices.filter { it.value > 0.0 }
    val total = shown.sumOf { it.value }

    // Reveal replays whenever the data set changes.
    val reveal = remember(shown) { Animatable(0f) }
    LaunchedEffect(shown) {
        reveal.snapTo(0f)
        reveal.animateTo(1f, tween(900, easing = MotionTokens.emphasized))
    }

    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size)) {
            val stroke = strokeWidth.toPx()
            val inset = stroke / 2f
            val arcSize = Size(this.size.width - stroke, this.size.height - stroke)
            val topLeft = Offset(inset, inset)
            if (total <= 0.0 || shown.isEmpty()) return@Canvas

            val gap = if (shown.size > 1) 3f else 0f
            val available = 360f - gap * shown.size
            var start = -90f
            shown.forEach { slice ->
                val sweep = (slice.value / total).toFloat() * available * reveal.value
                if (sweep > 0f) {
                    drawArc(
                        color = slice.color,
                        startAngle = start,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = stroke, cap = StrokeCap.Round),
                    )
                }
                start += (slice.value / total).toFloat() * available + gap
            }
        }
        if (center != null) center()
    }
}

/** A legend line for a donut slice: color dot, label, and the exact figure. */
@Composable
fun DonutLegendRow(
    slice: DonutSlice,
    valueText: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            Modifier
                .size(10.dp)
                .clip(CircleShape)
                .drawBehind { drawCircle(slice.color) },
        )
        Text(
            slice.label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(
            valueText,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

/**
 * A labelled horizontal bar that fills to `value / maxValue` on first show and
 * glides when the data changes — the building block of the money-in and
 * receivables graphs. The exact figure prints at the row's end.
 */
@Composable
fun AnimatedBarRow(
    label: String,
    value: Double,
    maxValue: Double,
    valueText: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    var appeared by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { appeared = true }
    val target = if (!appeared || maxValue <= 0.0) 0f
    else (value / maxValue).toFloat().coerceIn(0f, 1f)
    val fraction by animateFloatAsState(
        targetValue = target,
        animationSpec = tween(700, easing = MotionTokens.emphasized),
        label = "bar-fill",
    )
    val track = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)

    Column(modifier = modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                valueText,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        Spacer(Modifier.height(5.dp))
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(8.dp),
        ) {
            val radius = this.size.height / 2f
            drawRoundRect(
                color = track,
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius, radius),
            )
            val w = this.size.width * fraction
            if (w > 0f) {
                drawRoundRect(
                    color = color,
                    size = Size(w.coerceAtLeast(this.size.height), this.size.height),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius, radius),
                )
            }
        }
    }
}
