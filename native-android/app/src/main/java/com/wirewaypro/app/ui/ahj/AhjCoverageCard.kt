package com.wirewaypro.app.ui.ahj

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.LocationOff
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wirewaypro.app.domain.ahj.AhjCoverage
import com.wirewaypro.app.domain.ahj.CoverageConfidence
import com.wirewaypro.app.ui.components.SectionCard
import com.wirewaypro.app.ui.theme.BrandAmber
import com.wirewaypro.app.ui.theme.BrandGreen

/**
 * Drop-in "AHJ coverage" card for an estimate/job. Shows the selected jurisdiction
 * and, honestly, what we know about it: the adopted NEC edition + its source when
 * we have one, and — ALWAYS — that local amendments aren't yet mapped. It never
 * presents an area as compliant; absence of data is shown as absence.
 *
 * Broadly available (not tier-gated): the jurisdiction + adopted-edition baseline
 * is the trust magnet.
 */
@Composable
fun AhjCoverageCard(
    onEdit: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AhjCoverageViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    if (state.loading) return

    SectionCard(title = "AHJ coverage", modifier = modifier) {
        if (!state.hasJurisdiction) {
            NoJurisdictionBody(onEdit)
        } else {
            CoverageBody(state.coverage, onEdit)
        }
    }
}

@Composable
private fun NoJurisdictionBody(onEdit: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            Icons.Outlined.LocationOff,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.padding(end = 10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                "No jurisdiction set",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
            )
            Text(
                "Pick your state (and county/city) to check this against the code your inspector enforces.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    Spacer(Modifier.height(6.dp))
    TextButton(onClick = onEdit, modifier = Modifier.fillMaxWidth()) {
        Text("Set jurisdiction")
        Spacer(Modifier.padding(end = 6.dp))
        Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = null)
    }
}

@Composable
private fun CoverageBody(coverage: AhjCoverage, onEdit: () -> Unit) {
    AhjCoverageContent(coverage)
    Spacer(Modifier.height(4.dp))
    TextButton(onClick = onEdit) { Text("Change jurisdiction") }
}

/**
 * The honest coverage lines with NO edit affordance — reused by the detail card
 * and by the picker's live preview so both show the exact same truth: headline,
 * adopted edition, source, any nuance note, and the always-present amendment gap.
 */
@Composable
fun AhjCoverageContent(coverage: AhjCoverage, modifier: Modifier = Modifier) {
    Column(modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                coverage.headline,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            ConfidencePill(coverage.confidence)
        }

        Spacer(Modifier.height(8.dp))
        Text(
            coverage.editionLine,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )

        coverage.sourceLine?.let {
            Spacer(Modifier.height(4.dp))
            Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        coverage.note?.let {
            Spacer(Modifier.height(8.dp))
            Text(
                it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // The always-present honesty line: absence of amendment data, shown as absence.
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.Top) {
            Text("⚠", style = MaterialTheme.typography.bodyMedium, color = BrandAmber)
            Spacer(Modifier.padding(end = 6.dp))
            Text(
                coverage.amendmentsLine,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * A one-time onboarding nudge for the Home screen: shows a compact "set your
 * jurisdiction" prompt ONLY while the user hasn't picked one, and renders nothing
 * once they have. This is how the AHJ picker is surfaced during onboarding without
 * a dedicated first-run flow.
 */
@Composable
fun AhjJurisdictionNudge(
    onSet: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AhjCoverageViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    if (state.loading || state.hasJurisdiction) return

    SectionCard(title = "Pass inspection", modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Place, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.padding(end = 10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "Set your jurisdiction",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    "Check every estimate against the NEC edition your local inspector enforces.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        TextButton(onClick = onSet, modifier = Modifier.fillMaxWidth()) {
            Text("Set jurisdiction")
            Spacer(Modifier.padding(end = 6.dp))
            Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = null)
        }
    }
}

/** A small pill communicating how strong the shown coverage is (NOT a compliance verdict). */
@Composable
fun ConfidencePill(confidence: CoverageConfidence, modifier: Modifier = Modifier) {
    val (label, tint) = when (confidence) {
        CoverageConfidence.BASELINE -> "Verified baseline" to BrandGreen
        CoverageConfidence.AMBIGUOUS -> "Unverified" to BrandAmber
        CoverageConfidence.NONE -> "Not mapped" to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = tint,
        modifier = modifier
            .background(tint.copy(alpha = 0.14f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}
