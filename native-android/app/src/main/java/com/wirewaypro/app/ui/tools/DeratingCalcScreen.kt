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
import com.wirewaypro.app.domain.calc.AmpacityTable
import com.wirewaypro.app.domain.calc.Awg
import com.wirewaypro.app.domain.calc.ConductorMaterial
import com.wirewaypro.app.domain.calc.Derating
import com.wirewaypro.app.domain.calc.TempRating
import com.wirewaypro.app.ui.components.DetailScaffold
import com.wirewaypro.app.ui.components.FormField
import com.wirewaypro.app.ui.components.SectionCard
import com.wirewaypro.app.ui.components.SectionEyebrow
import com.wirewaypro.app.ui.components.SegmentedTabs
import com.wirewaypro.app.ui.theme.Spacing

/** Ampacity derating — ambient (Table 310.15(B)(1)) + bundling (Table 310.15(C)(1)). */
@Composable
fun DeratingCalcScreen(onBack: () -> Unit) {
    var gauge by remember { mutableStateOf(Awg.AWG12) }
    var materialIdx by remember { mutableStateOf(0) }
    var tempIdx by remember { mutableStateOf(2) } // default 90 °C insulation (typical THHN)
    var ambient by remember { mutableStateOf("30") }
    var ccc by remember { mutableStateOf("3") }

    val material = if (materialIdx == 0) ConductorMaterial.COPPER else ConductorMaterial.ALUMINUM
    val temp = TempRating.entries[tempIdx]
    val base = AmpacityTable.ampacity(material, gauge, temp)?.toDouble()
    val ambientC = parseNum(ambient)?.toInt()
    val count = parseNum(ccc)?.toInt()?.coerceAtLeast(1) ?: 1

    val result = if (base != null && ambientC != null)
        Derating.derate(base, temp, ambientC, count) else null

    DetailScaffold(title = "Derating", onBack = onBack, isLoading = false, error = null) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.screen, vertical = Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            SectionCard(title = "Conductor & conditions") {
                CalcDropdown("Conductor size", AmpacityTable.gauges(material), gauge, { it.label }, { gauge = it })
                Spacer(Modifier.height(Spacing.md))
                SectionEyebrow("Material")
                SegmentedTabs(listOf("Copper", "Aluminum"), materialIdx, { materialIdx = it })
                Spacer(Modifier.height(Spacing.md))
                SectionEyebrow("Insulation rating")
                SegmentedTabs(listOf("60 °C", "75 °C", "90 °C"), tempIdx, { tempIdx = it })
                Spacer(Modifier.height(Spacing.md))
                FormField(ambient, { ambient = it }, "Ambient temperature (°C)", keyboardType = KeyboardType.Number)
                Spacer(Modifier.height(Spacing.sm))
                FormField(ccc, { ccc = it }, "Current-carrying conductors in the raceway", keyboardType = KeyboardType.Number)
            }

            when {
                base == null -> ResultBanner("—", "${gauge.label} isn't listed for ${material.label}", ResultStatus.FAIL)
                ambientC == null -> ResultBanner("—", "Enter the ambient temperature", ResultStatus.NEUTRAL)
                result == null -> ResultBanner(
                    "Off the table",
                    "Ambient too high for ${temp.label} insulation",
                    ResultStatus.FAIL,
                )
                else -> {
                    ResultBanner(
                        value = "${fmt1(result.deratedAmpacity)} A",
                        caption = "Derated from ${fmt0(result.baseAmpacity)} A base",
                        status = ResultStatus.NEUTRAL,
                    )
                    SectionCard {
                        BreakdownRow("Base ampacity (${temp.label})", "${fmt0(result.baseAmpacity)} A")
                        BreakdownRow("Ambient factor", fmt2(result.ambientFactor))
                        BreakdownRow("Bundling factor", fmt2(result.bundlingFactor))
                        BreakdownRow("Derated ampacity", "${fmt1(result.deratedAmpacity)} A", emphasize = true)
                    }
                }
            }

            AssumptionsCard(
                necTag = "NEC 310.15(B)(1) · (C)(1)",
                assumptions = listOf(
                    "Base ampacity from Table 310.16 at the ${temp.label} column.",
                    "Ambient correction from Table 310.15(B)(1) (30 °C base).",
                    "Bundling adjustment from Table 310.15(C)(1) (1–3 conductors = no adjustment).",
                    "Derated ampacity = base × ambient factor × bundling factor.",
                ),
                why = "Heat is the enemy of insulation. A hot attic or a bundle of conductors sharing a raceway " +
                    "both raise the conductor's operating temperature, so the code trims the allowable ampacity — " +
                    "you may need a bigger wire even though the load didn't change.",
            )
        }
    }
}

private fun fmt0(v: Double): String = v.toInt().toString()
private fun fmt1(v: Double): String = String.format("%.1f", v)
private fun fmt2(v: Double): String = String.format("%.2f", v)
