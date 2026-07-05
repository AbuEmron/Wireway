package com.wirewaypro.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.dp
import com.wirewaypro.app.ui.theme.Elevation
import com.wirewaypro.app.ui.theme.MotionTokens
import com.wirewaypro.app.ui.theme.Spacing

/**
 * A card that morphs open — the mockup's expandable estimate-item row. The whole
 * header is the tap target (gloved thumbs), the chevron rolls over on a spring,
 * the body expands with a gentle physics settle, and elevation lifts slightly
 * while open so the expanded card reads as the active surface.
 *
 * State is the caller's ([expanded]/[onToggle]) so lists can keep exactly one row
 * open, persist expansion, or drive it from a ViewModel.
 */
@Composable
fun MorphingCard(
    expanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    header: @Composable RowScope.() -> Unit,
    body: @Composable ColumnScope.() -> Unit,
) {
    val haptics = rememberWirewayHaptics()
    val chevron by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = MotionTokens.springBouncy(),
        label = "chevron",
    )
    val interaction = remember { MutableInteractionSource() }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .pressScale(interaction, pressedScale = 0.985f)
            .animateContentSize(animationSpec = MotionTokens.springGentle()),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (expanded) Elevation.raised else Elevation.card,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(interactionSource = interaction, indication = null) {
                    haptics.tap()
                    onToggle()
                }
                .padding(horizontal = Spacing.lg, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            header()
            Icon(
                Icons.Outlined.KeyboardArrowDown,
                contentDescription = if (expanded) "Collapse" else "Expand",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.rotate(chevron),
            )
        }
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(animationSpec = MotionTokens.springGentle()) + fadeIn(),
            exit = shrinkVertically(animationSpec = MotionTokens.springGentle()) + fadeOut(),
        ) {
            Column(
                Modifier.padding(start = Spacing.lg, end = Spacing.lg, bottom = Spacing.lg),
            ) {
                body()
            }
        }
    }
}
