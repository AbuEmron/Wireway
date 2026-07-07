package com.wirewaypro.app.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A moving-highlight placeholder for content that is still loading — the difference
 * between "the app is thinking" and "the app froze". On a job site with a weak
 * signal, the offline-first data often paints a frame late; a shimmer tells the
 * electrician the screen is alive rather than hung, which is the whole trust game.
 *
 * Apply [shimmer] to any box you'd otherwise leave blank, or drop in a [ShimmerBox]
 * of a fixed size. Colors track the theme so it stays subtle in dark panels.
 */
@Composable
fun Modifier.shimmer(
    shape: Shape = RoundedCornerShape(8.dp),
    baseColor: androidx.compose.ui.graphics.Color =
        androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant,
    highlightColor: androidx.compose.ui.graphics.Color =
        androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.14f),
): Modifier {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmer-progress",
    )
    return this
        .clip(shape)
        .drawWithCache {
            val widthPx = size.width.coerceAtLeast(1f)
            val travel = widthPx * 2f
            val start = -widthPx + progress * travel
            val brush = Brush.linearGradient(
                colors = listOf(baseColor, highlightColor, baseColor),
                start = Offset(start, 0f),
                end = Offset(start + widthPx, size.height),
            )
            onDrawBehind { drawRect(brush = brush) }
        }
}

/** A ready-made shimmering block; use for placeholder lines, tiles, and avatars. */
@Composable
fun ShimmerBox(
    width: Dp,
    height: Dp,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(8.dp),
) {
    Box(
        modifier
            .width(width)
            .height(height)
            .background(
                androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant,
                shape,
            )
            .shimmer(shape),
    )
}

/**
 * A stack of shimmering card placeholders shaped like [ListCard] rows — the premium
 * "first load" state for a list. Reads as the list arriving rather than a lone
 * spinner spinning in the middle of an empty screen.
 */
@Composable
fun ListCardSkeleton(
    rows: Int = 5,
    modifier: Modifier = Modifier,
) {
    androidx.compose.foundation.layout.Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement =
            androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp),
    ) {
        repeat(rows) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(androidx.compose.material3.MaterialTheme.colorScheme.surface)
                    .padding(16.dp),
            ) {
                androidx.compose.foundation.layout.Column(
                    verticalArrangement =
                        androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp),
                ) {
                    ShimmerBox(width = 180.dp, height = 16.dp)
                    ShimmerBox(width = 120.dp, height = 12.dp)
                    ShimmerBox(width = 90.dp, height = 12.dp)
                }
            }
        }
    }
}
