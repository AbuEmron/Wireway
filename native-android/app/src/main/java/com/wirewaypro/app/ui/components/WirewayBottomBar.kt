package com.wirewaypro.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wirewaypro.app.ui.theme.BrandGradients
import com.wirewaypro.app.ui.theme.GradientBlue
import com.wirewaypro.app.ui.theme.MotionTokens

/**
 * The flagship bottom bar from the mockup: four tabs split around a raised
 * blue→purple gradient FAB. Selection animates on springs — the icon pops, the
 * label brightens, and a gradient dot slides in under the active tab. The FAB
 * carries a soft brand glow and the house press-dip, and every touch ticks.
 *
 * Purely presentational — routes and navigation stay with the caller.
 */
data class BottomBarTab(
    val route: String,
    val label: String,
    val icon: ImageVector,
)

@Composable
fun WirewayBottomBar(
    tabs: List<BottomBarTab>,
    isSelected: (BottomBarTab) -> Boolean,
    onTab: (BottomBarTab) -> Unit,
    onFab: () -> Unit,
    modifier: Modifier = Modifier,
    fabContentDescription: String = "New estimate",
) {
    require(tabs.size == 4) { "WirewayBottomBar lays out exactly 4 tabs around the center FAB" }
    val haptics = rememberWirewayHaptics()

    Box(modifier = modifier.fillMaxWidth()) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .height(64.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TabItem(tabs[0], isSelected(tabs[0]), Modifier.weight(1f)) { haptics.tap(); onTab(tabs[0]) }
                TabItem(tabs[1], isSelected(tabs[1]), Modifier.weight(1f)) { haptics.tap(); onTab(tabs[1]) }
                // The FAB's column: reserved space so the bar splits 2-2 around it.
                Spacer(Modifier.width(72.dp))
                TabItem(tabs[2], isSelected(tabs[2]), Modifier.weight(1f)) { haptics.tap(); onTab(tabs[2]) }
                TabItem(tabs[3], isSelected(tabs[3]), Modifier.weight(1f)) { haptics.tap(); onTab(tabs[3]) }
            }
        }

        CenterFab(
            onClick = { haptics.confirm(); onFab() },
            contentDescription = fabContentDescription,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .offset(y = (-28).dp),
        )
    }
}

@Composable
private fun TabItem(
    tab: BottomBarTab,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val color by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = MotionTokens.standardSpec(),
        label = "tab-color",
    )
    val pop by animateFloatAsState(
        targetValue = if (selected) 1.12f else 1f,
        animationSpec = MotionTokens.springBouncy(),
        label = "tab-pop",
    )
    val dotAlpha by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = MotionTokens.standardSpec(),
        label = "tab-dot",
    )
    val interaction = remember { MutableInteractionSource() }

    Column(
        modifier = modifier
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            tab.icon,
            contentDescription = tab.label,
            tint = color,
            modifier = Modifier
                .size(24.dp)
                .graphicsLayer {
                    scaleX = pop
                    scaleY = pop
                },
        )
        Spacer(Modifier.height(3.dp))
        Text(
            text = tab.label,
            style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 0.2.sp),
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            color = color,
            maxLines = 1,
        )
        Spacer(Modifier.height(3.dp))
        Box(
            Modifier
                .size(width = 14.dp, height = 3.dp)
                .graphicsLayer { alpha = dotAlpha }
                .clip(CircleShape)
                .drawBehind { drawRect(brush = BrandGradients.primary) },
        )
    }
}

@Composable
private fun CenterFab(
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .pressScale(interaction, pressedScale = 0.92f)
            .size(58.dp)
            .shadow(
                elevation = 12.dp,
                shape = CircleShape,
                ambientColor = GradientBlue,
                spotColor = GradientBlue,
            )
            .clip(CircleShape)
            .drawBehind {
                drawRect(brush = BrandGradients.primary)
                // Top-lit sheen so the button reads as a physical key.
                drawRect(
                    brush = Brush.verticalGradient(
                        0f to Color.White.copy(alpha = 0.22f),
                        0.55f to Color.Transparent,
                    ),
                )
            }
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Filled.Add,
            contentDescription = contentDescription,
            tint = Color.White,
            modifier = Modifier.size(28.dp),
        )
    }
}
