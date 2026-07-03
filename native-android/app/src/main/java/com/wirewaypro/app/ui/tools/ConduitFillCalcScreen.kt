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
import com.wirewaypro.app.domain.calc.ConduitFill
import com.wirewaypro.app.ui.components.DetailScaffold
import com.wirewaypro.app.ui.components.FormField
import com.wirewaypro.app.ui.components.SectionCard
import com.wirewaypro.app.ui.components.SectionEyebrow
import com.wirewaypro.app.ui.components.SegmentedTabs
import com.wirewaypro.app.ui.theme.Spacing

/** Conduit fill — NEC Chapter 9 Tables 1 (fill %), 4 (conduit areas), 5 (THHN areas). */
@Composable
fun ConduitFillCalcScreen(onBack: () -> Unit) {
    var gauge by remember { mutableStateOf(Awg.AWG12) }
    var count by remember { mutableStateOf("3") }
    var typeIdx by remember { mutableStateOf(0) }

    val type = ConduitFill.ConduitType.entries[typeIdx]
    val n = parseNum(count)?.toInt()?.coerceAtLeast(0)
    val conductors = if (n != null && n > 0) listOf(ConduitFill.ConductorSpec(gauge, n)) else emptyList()

    val minConduit = if (conductors.isNotEmpty()) ConduitFill.minimumConduit(conductors, type) else null
    val eval = if (minConduit != null) ConduitFill.evaluate(conductors, type, minConduit) else null

    DetailScaffold(title = "Conduit fill", onBack = onBack, isLoading = false, error = null) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.screen, vertical = Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            SectionCard(title = "Conductors (THHN/THWN-2)") {
                CalcDropdown("Conductor size", Awg.entries, gauge, { it.label }, { gauge = it })
                Spacer(Modifier.height(Spacing.sm))
                FormField(count, { count = it }, "How many conductors", keyboardType = KeyboardType.Number)
                Spacer(Modifier.height(Spacing.md))
                SectionEyebrow("Conduit type")
                SegmentedTabs(ConduitFill.ConduitType.entries.map { it.label }, typeIdx, { typeIdx = it })
            }

            when {
                conductors.isEmpty() -> ResultBanner("—", "Enter a conductor count", ResultStatus.NEUTRAL)
                minConduit == null -> ResultBanner(
                    "Too many",
                    "Won't fit the sizes carried here (up to 4\")",
                    ResultStatus.FAIL,
                )
                eval != null -> {
                    ResultBanner(
                        value = "${minConduit.label} ${type.label}",
                        caption = "Smallest that fits — ${fmt1(eval.fillPercent)}% fill (max ${pct(eval.maxFillFraction)})",
                        status = ResultStatus.PASS,
                    )
                    SectionCard {
                        BreakdownRow("Conductors", "${eval.conductorCount} × ${gauge.label}")
                        BreakdownRow("Conductor area", "${fmt3(eval.conductorAreaSqIn)} in²")
                        BreakdownRow("Max fill (${eval.conductorCount} cond.)", pct(eval.maxFillFraction))
                        BreakdownRow("Allowed area", "${fmt3(eval.allowedAreaSqIn)} in²")
                        BreakdownRow("Fill", "${fmt1(eval.fillPercent)} %", emphasize = true)
                    }
                }
            }

            AssumptionsCard(
                necTag = "NEC Ch. 9 · T1/T4/T5",
                assumptions = listOf(
                    "Max fill (Table 1): 1 conductor 53%, 2 conductors 31%, 3 or more 40%.",
                    "Conduit internal area from Table 4 (${type.label}).",
                    "Conductor area from Table 5, THHN/THWN-2 insulation.",
                    "All conductors the same size here; mixed sizes sum their areas.",
                ),
                why = "Conductors need air around them to shed heat and to be pulled without damaging insulation. " +
                    "The fill percentage caps how much of the pipe's cross-section the wires may occupy — exceed it " +
                    "and you risk overheating and a rough, jacket-scraping pull.",
            )
        }
    }
}

private fun fmt1(v: Double): String = String.format("%.1f", v)
private fun fmt3(v: Double): String = String.format("%.3f", v)
private fun pct(frac: Double): String = "${(frac * 100).toInt()}%"
