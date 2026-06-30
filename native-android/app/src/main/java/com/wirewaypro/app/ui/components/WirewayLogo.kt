package com.wirewaypro.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.wirewaypro.app.ui.theme.BrandGradients
import com.wirewaypro.app.ui.theme.GradientBlue
import com.wirewaypro.app.ui.theme.GradientPurple

/**
 * The Wireway mark: a lightning-bolt "W" rendered as vector strokes. The down-strokes
 * are sheared into bolt-like blades and a spark notch sits in the centre valley, so
 * the glyph reads as both a W and a lightning bolt. Filled with the blue→purple
 * brand gradient.
 *
 * @param onGradient when true the mark is drawn in white (for use on top of a
 *   gradient/colored tile, e.g. [WirewayLogoBadge]); otherwise it uses the gradient.
 */
@Composable
fun WirewayLogomark(
    modifier: Modifier = Modifier,
    size: Dp = 28.dp,
    onGradient: Boolean = false,
) {
    Canvas(modifier = modifier.size(size)) {
        drawLightningW(
            brush = if (onGradient) null else BrandGradients.primary,
            solid = if (onGradient) Color.White else null,
        )
    }
}

/**
 * The mark inside a rounded gradient tile — the app/brand badge lockup. The "W" is
 * knocked out in white over the blue→purple gradient.
 */
@Composable
fun WirewayLogoBadge(
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
) {
    Canvas(modifier = modifier.size(size)) {
        val r = this.size.minDimension * 0.28f
        drawRoundRect(
            brush = Brush.linearGradient(
                colors = listOf(GradientBlue, GradientPurple),
                start = Offset.Zero,
                end = Offset(this.size.width, this.size.height),
            ),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(r, r),
        )
        // Inset the mark within the tile.
        val pad = this.size.minDimension * 0.26f
        drawLightningW(
            brush = null,
            solid = Color.White,
            inset = pad,
        )
    }
}

/**
 * Draws the lightning-"W" stroke into the current [DrawScope]. The path runs through
 * five points (a W zigzag); the centre peak is split by a short spark to suggest a
 * bolt. Either [brush] (gradient) or [solid] colour must be supplied.
 */
private fun DrawScope.drawLightningW(
    brush: Brush?,
    solid: Color?,
    inset: Float = size.minDimension * 0.12f,
) {
    val w = size.width
    val h = size.height
    val left = inset
    val right = w - inset
    val top = inset
    val bottom = h - inset
    val midX = w / 2f
    // Bolt-sheared W: down-strokes lean, the centre peak undershoots to leave a spark gap.
    val p1 = Offset(left, top)
    val v1 = Offset(left + (right - left) * 0.27f, bottom)
    val peak = Offset(midX, top + (bottom - top) * 0.36f)
    val v2 = Offset(right - (right - left) * 0.27f, bottom)
    val p2 = Offset(right, top)

    val path = Path().apply {
        moveTo(p1.x, p1.y)
        lineTo(v1.x, v1.y)
        lineTo(peak.x, peak.y)
        lineTo(v2.x, v2.y)
        lineTo(p2.x, p2.y)
    }
    val strokeWidth = size.minDimension * 0.135f
    val stroke = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
    when {
        brush != null -> drawPath(path, brush = brush, style = stroke)
        solid != null -> drawPath(path, color = solid, style = stroke)
    }
    // Spark: a short bolt segment dropping from the centre peak.
    val spark = Path().apply {
        moveTo(peak.x, peak.y + strokeWidth * 0.2f)
        lineTo(midX - strokeWidth * 0.45f, top + (bottom - top) * 0.62f)
        lineTo(midX + strokeWidth * 0.15f, top + (bottom - top) * 0.58f)
        lineTo(midX - strokeWidth * 0.25f, bottom - strokeWidth * 0.2f)
    }
    val sparkStroke = Stroke(width = strokeWidth * 0.5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    when {
        brush != null -> drawPath(spark, brush = brush, style = sparkStroke)
        solid != null -> drawPath(spark, color = solid, style = sparkStroke)
    }
}
