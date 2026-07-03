package com.wirewaypro.app.ui.tools

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.CompareArrows
import androidx.compose.material.icons.outlined.Cable
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Thermostat
import androidx.compose.material.icons.outlined.Straighten
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.wirewaypro.app.ui.components.DetailScaffold
import com.wirewaypro.app.ui.components.NavRow
import com.wirewaypro.app.ui.components.SectionEyebrow
import com.wirewaypro.app.ui.theme.Spacing

/**
 * The Tools hub — one place for the deterministic field calculators and the code
 * references. Every calculator here is pure-Kotlin, NEC-table unit-tested, and shows
 * its assumptions; nothing depends on a signal or a subscription to compute.
 */
@Composable
fun ToolsScreen(
    onBack: () -> Unit,
    onWireSize: () -> Unit,
    onVoltageDrop: () -> Unit,
    onConduitFill: () -> Unit,
    onBoxFill: () -> Unit,
    onDerating: () -> Unit,
    onNec: () -> Unit,
    onLoadAdvisor: () -> Unit,
) {
    DetailScaffold(title = "Tools", onBack = onBack, isLoading = false, error = null) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.screen, vertical = Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            SectionEyebrow("Calculators")
            NavRow("Wire size", Icons.Outlined.Cable, onWireSize, subtitle = "Min conductor for a load — 310.16")
            NavRow("Voltage drop", Icons.Outlined.CompareArrows, onVoltageDrop, subtitle = "% drop over a run; upsize for length")
            NavRow("Conduit fill", Icons.Outlined.Straighten, onConduitFill, subtitle = "Ch. 9 Tables 1/4/5 — fill % + min conduit")
            NavRow("Box fill", Icons.Outlined.Inbox, onBoxFill, subtitle = "314.16 — is the box big enough?")
            NavRow("Derating", Icons.Outlined.Thermostat, onDerating, subtitle = "Ambient + bundling ampacity correction")

            SectionEyebrow("Reference", modifier = Modifier.padding(top = Spacing.sm))
            NavRow("NEC code reference", Icons.Outlined.MenuBook, onNec, subtitle = "Residential articles, rules, common violations")
            NavRow("Load advisor", Icons.Outlined.Bolt, onLoadAdvisor, subtitle = "Can the service handle a new load? — 220.83/.87")
        }
    }
}
