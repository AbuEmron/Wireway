package com.wirewaypro.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.unit.sp
import com.wirewaypro.app.ui.theme.BrandGradients

private val WirewayCyan = Color(0xFF22D3FF)

/**
 * The Wireway brand lockup from the mockup: the W-bolt badge + the "WIREWAY" wordmark
 * with a cyan "PRO", optionally over the "electrical estimating · powered by precision"
 * tagline. Used on the login and dashboard headers.
 */
@Composable
fun WirewayWordmark(
    modifier: Modifier = Modifier,
    badgeSize: Dp = 36.dp,
    showPro: Boolean = true,
    tagline: Boolean = false,
) {
    Column(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            WirewayLogoBadge(size = badgeSize)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "WIREWAY",
                    style = MaterialTheme.typography.headlineSmall.copy(letterSpacing = 1.2.sp),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                if (showPro) {
                    Text(
                        text = "PRO",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = WirewayCyan,
                        modifier = Modifier.padding(start = 5.dp),
                    )
                }
            }
        }
        if (tagline) {
            Text(
                text = "ELECTRICAL ESTIMATING · POWERED BY PRECISION",
                style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 0.8.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = badgeSize + 10.dp, top = 4.dp),
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
