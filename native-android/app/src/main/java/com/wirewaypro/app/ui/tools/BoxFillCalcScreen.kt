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
import com.wirewaypro.app.domain.calc.BoxFill
import com.wirewaypro.app.ui.components.DetailScaffold
import com.wirewaypro.app.ui.components.FormField
import com.wirewaypro.app.ui.components.SectionCard
import com.wirewaypro.app.ui.components.SectionEyebrow
import com.wirewaypro.app.ui.components.SegmentedTabs
import com.wirewaypro.app.ui.theme.Spacing

/** Box fill — NEC 314.16(B): is the box big enough for what's in it? */
@Composable
fun BoxFillCalcScreen(onBack: () -> Unit) {
    var size by remember { mutableStateOf(BoxFill.BoxWire.AWG14) }
    var conductors by remember { mutableStateOf("") }
    var devices by remember { mutableStateOf("1") }
    var grounds by remember { mutableStateOf("1") }
    var fittings by remember { mutableStateOf("0") }
    var clampsIdx by remember { mutableStateOf(1) } // default Yes (NM cable clamps)
    var boxVol by remember { mutableStateOf("") }

    val c = parseNum(conductors)?.toInt()?.coerceAtLeast(0)
    val d = parseNum(devices)?.toInt()?.coerceAtLeast(0) ?: 0
    val g = parseNum(grounds)?.toInt()?.coerceAtLeast(0) ?: 0
    val f = parseNum(fittings)?.toInt()?.coerceAtLeast(0) ?: 0
    val box = parseNum(boxVol)

    val result = if (c != null && c >= 0 && box != null && box > 0) {
        BoxFill.evaluate(
            BoxFill.Input(
                conductorSize = size,
                conductors = c,
                devices = d,
                hasClamps = clampsIdx == 1,
                supportFittings = f,
                groundingConductors = g,
            ),
            boxVolumeSqIn = box,
        )
    } else null

    DetailScaffold(title = "Box fill", onBack = onBack, isLoading = false, error = null) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.screen, vertical = Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            SectionCard(title = "What's in the box") {
                CalcDropdown("Largest conductor", BoxFill.BoxWire.entries, size, { it.label }, { size = it })
                Spacer(Modifier.height(Spacing.sm))
                FormField(conductors, { conductors = it }, "Current-carrying conductors", keyboardType = KeyboardType.Number)
                Spacer(Modifier.height(Spacing.sm))
                FormField(devices, { devices = it }, "Devices (switch/receptacle yokes)", keyboardType = KeyboardType.Number)
                Spacer(Modifier.height(Spacing.sm))
                FormField(grounds, { grounds = it }, "Grounding conductors", keyboardType = KeyboardType.Number)
                Spacer(Modifier.height(Spacing.sm))
                FormField(fittings, { fittings = it }, "Support fittings (studs/hickeys)", keyboardType = KeyboardType.Number)
                Spacer(Modifier.height(Spacing.md))
                SectionEyebrow("Internal cable clamps")
                SegmentedTabs(listOf("None", "Yes"), clampsIdx, { clampsIdx = it })
                Spacer(Modifier.height(Spacing.md))
                FormField(boxVol, { boxVol = it }, "Box volume (in³, from the box marking)", keyboardType = KeyboardType.Number)
            }

            when {
                result == null -> ResultBanner("—", "Enter conductor count and box volume", ResultStatus.NEUTRAL)
                else -> {
                    ResultBanner(
                        value = "${fmt2(result.requiredVolumeSqIn)} in³ needed",
                        caption = if (result.withinLimit)
                            "Fits — ${fmt2(result.spareVolumeSqIn)} in³ to spare"
                        else
                            "Overfilled by ${fmt2(-result.spareVolumeSqIn)} in³ — size up the box",
                        status = if (result.withinLimit) ResultStatus.PASS else ResultStatus.FAIL,
                    )
                    SectionCard {
                        BreakdownRow("Conductor allowance", "${fmt2(size.volumeSqIn)} in³ each (${size.label})")
                        BreakdownRow("Counted equivalents", fmt1(result.conductorEquivalents))
                        BreakdownRow("Required volume", "${fmt2(result.requiredVolumeSqIn)} in³", emphasize = true)
                        BreakdownRow("Box volume", "${fmt2(result.boxVolumeSqIn)} in³")
                    }
                }
            }

            AssumptionsCard(
                necTag = "NEC 314.16(B)",
                assumptions = listOf(
                    "Each conductor entering/passing through = 1; a pigtail wholly inside = 0.",
                    "Each device yoke = 2; all clamps together = 1; all grounds together = 1.",
                    "Each support fitting (stud/hickey) = 1.",
                    "Everything priced at the largest conductor's volume allowance (${size.label} = ${fmt2(size.volumeSqIn)} in³).",
                ),
                why = "An overstuffed box traps heat and makes terminations hard to fold in without nicking " +
                    "insulation. 314.16 turns everything inside into an equivalent conductor count so you can " +
                    "check the box's marked cubic inches before you cram it.",
            )
        }
    }
}

private fun fmt1(v: Double): String = String.format("%.1f", v)
private fun fmt2(v: Double): String = String.format("%.2f", v)
