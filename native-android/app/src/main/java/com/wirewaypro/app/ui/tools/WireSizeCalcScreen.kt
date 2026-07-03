package com.wirewaypro.app.ui.tools

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import com.wirewaypro.app.domain.calc.ConductorMaterial
import com.wirewaypro.app.domain.calc.TempRating
import com.wirewaypro.app.domain.calc.WireSize
import com.wirewaypro.app.ui.components.DetailScaffold
import com.wirewaypro.app.ui.components.FormField
import com.wirewaypro.app.ui.components.SectionCard
import com.wirewaypro.app.ui.components.SectionEyebrow
import com.wirewaypro.app.ui.components.SegmentedTabs
import com.wirewaypro.app.ui.theme.Spacing

/** Minimum conductor size for a load — NEC Table 310.16 + the 125% continuous rule. */
@Composable
fun WireSizeCalcScreen(onBack: () -> Unit) {
    var amps by remember { mutableStateOf("") }
    var materialIdx by remember { mutableStateOf(0) }
    var tempIdx by remember { mutableStateOf(0) }
    var continuousIdx by remember { mutableStateOf(0) }

    val material = if (materialIdx == 0) ConductorMaterial.COPPER else ConductorMaterial.ALUMINUM
    val temp = TempRating.entries[tempIdx]
    val continuous = continuousIdx == 1
    val load = parseNum(amps)

    val result = load?.let { WireSize.minimumSize(it, material, temp, continuous) }

    DetailScaffold(title = "Wire size", onBack = onBack, isLoading = false, error = null) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.screen, vertical = Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            SectionCard(title = "Load") {
                FormField(amps, { amps = it }, "Load (amps)", keyboardType = KeyboardType.Number)
                Spacer()
                SectionEyebrow("Conductor material")
                SegmentedTabs(listOf("Copper", "Aluminum"), materialIdx, { materialIdx = it })
                Spacer()
                SectionEyebrow("Termination rating")
                SegmentedTabs(listOf("60 °C", "75 °C", "90 °C"), tempIdx, { tempIdx = it })
                Spacer()
                SectionEyebrow("Load type")
                SegmentedTabs(listOf("Standard", "Continuous"), continuousIdx, { continuousIdx = it })
            }

            when {
                load == null -> ResultBanner("—", "Enter the load in amps", ResultStatus.NEUTRAL)
                result == null -> ResultBanner(
                    "Off the table",
                    "Load exceeds the sizes carried here (up to 500 kcmil)",
                    ResultStatus.FAIL,
                )
                else -> {
                    ResultBanner(
                        value = result.gauge.label,
                        caption = "${result.gaugeAmpacity} A at ${temp.label} — meets ${fmt(result.requiredAmpacity)} A required",
                        status = ResultStatus.PASS,
                    )
                    SectionCard {
                        BreakdownRow("Load", "${fmt(load)} A")
                        if (continuous) BreakdownRow("× 125% (continuous)", "${fmt(result.requiredAmpacity)} A")
                        BreakdownRow("Required ampacity", "${fmt(result.requiredAmpacity)} A")
                        BreakdownRow("Selected conductor", "${result.gauge.label} ${material.label}", emphasize = true)
                        BreakdownRow("Its ampacity (${temp.label})", "${result.gaugeAmpacity} A")
                    }
                }
            }

            AssumptionsCard(
                necTag = "NEC 310.16 · 210.19",
                assumptions = listOf(
                    "Ampacity read from Table 310.16 at the ${temp.label} termination column.",
                    "Conductor material: ${material.label}.",
                    if (continuous)
                        "Continuous load: conductor sized to 125% of the load (210.19(A))."
                    else
                        "Standard (non-continuous) load — no 125% factor applied.",
                    "Base conditions: 30 °C ambient, ≤3 current-carrying conductors (derate separately).",
                ),
                why = "Breakers and lugs are listed for a temperature (usually 60 °C ≤100 A). The conductor's " +
                    "allowable ampacity in that column must meet the load, so the wire never runs hotter than the " +
                    "terminals it lands on.",
            )
        }
    }
}

@Composable
private fun Spacer() {
    androidx.compose.foundation.layout.Spacer(Modifier.padding(top = Spacing.sm))
}

private fun fmt(v: Double): String =
    if (v % 1.0 == 0.0) v.toLong().toString() else String.format("%.1f", v)
