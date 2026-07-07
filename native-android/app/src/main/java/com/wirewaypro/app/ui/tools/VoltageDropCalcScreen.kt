package com.wirewaypro.app.ui.tools

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
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
import com.wirewaypro.app.domain.calc.Awg
import com.wirewaypro.app.domain.calc.ConductorMaterial
import com.wirewaypro.app.domain.calc.VoltageDrop
import com.wirewaypro.app.ui.components.DetailScaffold
import com.wirewaypro.app.ui.components.FormField
import com.wirewaypro.app.ui.components.SectionCard
import com.wirewaypro.app.ui.components.SectionEyebrow
import com.wirewaypro.app.ui.components.SegmentedTabs
import com.wirewaypro.app.ui.theme.Spacing

/** Voltage drop over a run — Vd = (2 | √3)·K·I·L ÷ CM, with a 3% recommendation. */
@Composable
fun VoltageDropCalcScreen(onBack: () -> Unit) {
    var gauge by remember { mutableStateOf(Awg.AWG12) }
    var materialIdx by remember { mutableStateOf(0) }
    var phaseIdx by remember { mutableStateOf(0) }
    var volts by remember { mutableStateOf("120") }
    var amps by remember { mutableStateOf("") }
    var lengthFt by remember { mutableStateOf("") }

    val material = if (materialIdx == 0) ConductorMaterial.COPPER else ConductorMaterial.ALUMINUM
    val phase = VoltageDrop.Phase.entries[phaseIdx]
    val v = parseNum(volts)
    val i = parseNum(amps)
    val l = parseNum(lengthFt)

    val result = if (v != null && i != null && l != null && v > 0)
        VoltageDrop.calculate(gauge, material, v, i, l, phase) else null
    val suggested = if (v != null && i != null && l != null && v > 0)
        VoltageDrop.minimumSizeForDrop(material, v, i, l, phase = phase) else null

    DetailScaffold(title = "Voltage drop", onBack = onBack, isLoading = false, error = null) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.screen, vertical = Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            SectionCard(title = "Circuit") {
                CalcDropdown("Conductor size", Awg.entries, gauge, { it.label }, { gauge = it })
                Spacer(Modifier.height(Spacing.md))
                SectionEyebrow("Material")
                SegmentedTabs(listOf("Copper", "Aluminum"), materialIdx, { materialIdx = it })
                Spacer(Modifier.height(Spacing.md))
                SectionEyebrow("Phase")
                SegmentedTabs(listOf("1-phase", "3-phase"), phaseIdx, { phaseIdx = it })
                Spacer(Modifier.height(Spacing.md))
                FormField(volts, { volts = it }, "System voltage", keyboardType = KeyboardType.Number)
                Spacer(Modifier.height(Spacing.sm))
                FormField(amps, { amps = it }, "Load (amps)", keyboardType = KeyboardType.Number)
                Spacer(Modifier.height(Spacing.sm))
                FormField(lengthFt, { lengthFt = it }, "One-way length (ft)", keyboardType = KeyboardType.Number)
            }

            if (result == null) {
                ResultBanner("—", "Enter voltage, load, and length", ResultStatus.NEUTRAL)
            } else {
                ResultBanner(
                    value = "${fmt1(result.percent)}%  (${fmt1(result.voltageDrop)} V)",
                    caption = if (result.withinRecommended) "Within the 3% recommendation"
                        else "Exceeds the 3% branch-circuit recommendation",
                    status = if (result.withinRecommended) ResultStatus.PASS else ResultStatus.FAIL,
                )
                SectionCard {
                    BreakdownRow("Voltage drop", "${fmt1(result.voltageDrop)} V")
                    BreakdownRow("Percent drop", "${fmt1(result.percent)} %", emphasize = true)
                    BreakdownRow("Voltage at load", "${fmt1(result.endVoltage)} V")
                    if (!result.withinRecommended && suggested != null && suggested != gauge) {
                        BreakdownRow("Upsize to ≤3%", suggested.label, emphasize = true)
                    }
                }
            }

            AssumptionsCard(
                necTag = "NEC 210.19(A) Note",
                assumptions = listOf(
                    "Formula: Vd = ${if (phase == VoltageDrop.Phase.SINGLE) "2" else "√3"} × K × I × L ÷ circular mils.",
                    "K = ${if (material == ConductorMaterial.COPPER) "12.9 (copper)" else "21.2 (aluminum)"} ohm-cmil/ft.",
                    "L is one-way length; the phase constant covers the return path.",
                    "3% branch / 5% total is a recommendation, not a hard NEC requirement.",
                ),
                why = "Long runs lose voltage in the conductor itself. Keeping the drop under ~3% means motors " +
                    "start, lights don't dim, and the equipment sees close to nameplate voltage — so on a long " +
                    "pull you upsize the wire even when ampacity alone would allow a smaller one.",
            )
        }
    }
}

private fun fmt1(v: Double): String = String.format("%.1f", v)
