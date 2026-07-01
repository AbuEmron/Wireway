package com.wirewaypro.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.wirewaypro.app.ui.theme.BrandGradients

/**
 * The Wireway brand lockup: the lightning-"W" gradient badge + the "Wireway" word
 * with an accent "PRO" chip. Used on the login screen and the dashboard header.
 */
@Composable
fun WirewayWordmark(
    modifier: Modifier = Modifier,
    badgeSize: Dp = 36.dp,
    showPro: Boolean = true,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier,
    ) {
        WirewayLogoBadge(size = badgeSize)
        Text(
            text = "Wireway",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        if (showPro) {
            Text(
                text = "PRO",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier
                    .drawWithCache {
                        val brush = BrandGradients.primary
                        onDrawBehind {
                            drawRoundRect(
                                brush = brush,
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(7.dp.toPx(), 7.dp.toPx()),
                            )
                        }
                    }
                    .padding(horizontal = 8.dp, vertical = 3.dp),
            )
        }
    }
}

/**
 * Gradient-filled text — paints any text with the brand blue→purple gradient via a
 * SrcIn blend over the glyphs. Used for hero numbers and accent headings.
 */
@Composable
fun GradientText(
    text: String,
    modifier: Modifier = Modifier,
    style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.displayLarge,
    fontWeight: FontWeight? = FontWeight.Bold,
) {
    Text(
        text = text,
        style = style,
        fontWeight = fontWeight,
        color = Color.White,
        modifier = modifier.drawWithCache {
            val brush = BrandGradients.primary
            onDrawWithContent {
                drawContent()
                drawRect(brush = brush, blendMode = BlendMode.SrcAtop)
            }
        },
    )
}
