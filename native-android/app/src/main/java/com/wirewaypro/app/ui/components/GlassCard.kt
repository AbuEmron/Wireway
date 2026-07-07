package com.wirewaypro.app.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.wirewaypro.app.ui.theme.BrandBlue
import com.wirewaypro.app.ui.theme.BrandPurple
import com.wirewaypro.app.ui.theme.Spacing

/**
 * "Frosted glass" surface treatment: a translucent, top-lit fill with a brand-tinted
 * hairline edge. Deliberately blur-free — a real backdrop blur (RenderEffect) is
 * API 31+ and costs frames on the mid-range phones electricians actually carry, so
 * this fakes the depth with layered translucency and a blue→purple rim. The result
 * reads as premium in a dark panel while holding 60fps.
 *
 * Use [GlassCard] for a padded container, or [glass] to skin any surface.
 */
fun Modifier.glass(
    shape: Shape = RoundedCornerShape(20.dp),
    fillColor: androidx.compose.ui.graphics.Color,
): Modifier = this
    .clip(shape)
    .drawBehind {
        // Top-lit translucent fill: brighter at the top edge, settling into the card.
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    fillColor.copy(alpha = 0.72f),
                    fillColor.copy(alpha = 0.92f),
                ),
                startY = 0f,
                endY = size.height,
            ),
        )
    }
    .border(
        width = 1.dp,
        brush = Brush.linearGradient(
            colors = listOf(
                BrandBlue.copy(alpha = 0.45f),
                BrandPurple.copy(alpha = 0.25f),
            ),
            start = Offset(0f, 0f),
            end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY),
        ),
        shape = shape,
    )

/** A padded glass container; the premium sibling of [SectionCard]. */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    contentPadding: androidx.compose.foundation.layout.PaddingValues =
        androidx.compose.foundation.layout.PaddingValues(Spacing.xl),
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .glass(shape = shape, fillColor = MaterialTheme.colorScheme.surface)
            .padding(contentPadding),
        content = content,
    )
}
