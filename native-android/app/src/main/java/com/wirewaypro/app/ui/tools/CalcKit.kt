package com.wirewaypro.app.ui.tools

import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.wirewaypro.app.ui.components.GlassCard
import com.wirewaypro.app.ui.components.SectionCard
import com.wirewaypro.app.ui.components.SectionEyebrow
import com.wirewaypro.app.ui.theme.Spacing
import com.wirewaypro.app.ui.theme.extended

/**
 * Shared building blocks for the deterministic calculators. Every calculator obeys the
 * Architecture Doctrine's "transparent" rule: it states its assumptions and cites the
 * NEC article it implements, so the number is defensible in front of an inspector.
 */

/** A read-only dropdown for choosing an enum-ish option (material, gauge, conduit…). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> CalcDropdown(
    label: String,
    options: List<T>,
    selected: T,
    optionLabel: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = optionLabel(selected),
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(optionLabel(option)) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    },
                )
            }
        }
    }
}

/** Pass / fail / info status for a result banner. */
enum class ResultStatus { PASS, FAIL, NEUTRAL }

/**
 * The headline answer, lifted onto glass and tinted by outcome. The big value is the
 * first thing an electrician reads; the status color (green pass / red fail) is legible
 * at a glance in the field.
 */
@Composable
fun ResultBanner(
    value: String,
    caption: String,
    status: ResultStatus = ResultStatus.NEUTRAL,
    modifier: Modifier = Modifier,
) {
    val ext = MaterialTheme.extended
    val (tint, icon) = when (status) {
        ResultStatus.PASS -> ext.success to Icons.Outlined.CheckCircle
        ResultStatus.FAIL -> MaterialTheme.colorScheme.error to Icons.Outlined.Warning
        ResultStatus.NEUTRAL -> MaterialTheme.colorScheme.primary to Icons.Outlined.Info
    }
    GlassCard(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .padding(end = Spacing.md)
                    .height(44.dp)
                    .drawBehind {
                        drawRoundRect(
                            color = tint.copy(alpha = 0.14f),
                            cornerRadius = CornerRadius(14.dp.toPx(), 14.dp.toPx()),
                        )
                    }
                    .padding(10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = tint)
            }
            Column(Modifier.fillMaxWidth()) {
                // The answer slides up as inputs change — alive, never jarring.
                androidx.compose.animation.AnimatedContent(
                    targetState = value,
                    transitionSpec = {
                        (
                            androidx.compose.animation.fadeIn(
                                androidx.compose.animation.core.tween(180),
                            ) + androidx.compose.animation.slideInVertically { it / 3 }
                            ).togetherWith(
                            androidx.compose.animation.fadeOut(
                                androidx.compose.animation.core.tween(120),
                            ) + androidx.compose.animation.slideOutVertically { -it / 3 },
                        )
                    },
                    label = "result-value",
                ) { shown ->
                    Text(
                        text = shown,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Text(
                    text = caption,
                    style = MaterialTheme.typography.bodyMedium,
                    color = tint,
                )
            }
        }
    }
}

/**
 * The "show your work" card the doctrine requires: the NEC article this calc
 * implements, the assumptions baked in, and a plain-English "why". Never hidden —
 * transparency is what earns trust with a tool used on real jobs.
 */
@Composable
fun AssumptionsCard(
    necTag: String,
    assumptions: List<String>,
    why: String,
    modifier: Modifier = Modifier,
) {
    SectionCard(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionEyebrow("Assumptions & why", modifier = Modifier.weight(1f))
            NecTagPill(necTag)
        }
        Spacer(Modifier.height(Spacing.md))
        assumptions.forEach { line ->
            Row(Modifier.padding(vertical = 2.dp)) {
                Text("•  ", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                Text(
                    line,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(Spacing.md))
        Text(
            why,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(Spacing.sm))
        Text(
            "Educational engineering tool — confirm against the adopted code and local amendments.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** A small pill tagging the NEC article/table a calc implements. */
@Composable
fun NecTagPill(text: String, modifier: Modifier = Modifier) {
    val tint = MaterialTheme.colorScheme.primary
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = tint,
        modifier = modifier
            .drawBehind {
                drawRoundRect(
                    color = tint.copy(alpha = 0.12f),
                    cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx()),
                )
            }
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}

/** A labelled row for one line of a computed breakdown. */
@Composable
fun BreakdownRow(label: String, value: String, emphasize: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (emphasize) FontWeight.Bold else FontWeight.Medium,
            color = if (emphasize) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        )
    }
}

/** Parse a user-entered decimal, tolerant of blank/garbage → null. */
fun parseNum(s: String): Double? = s.trim().toDoubleOrNull()?.takeIf { it.isFinite() }
