package com.wirewaypro.app.ui.tools

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import com.wirewaypro.app.domain.pricing.RegionalLaborRates
import com.wirewaypro.app.ui.components.DetailScaffold
import com.wirewaypro.app.ui.components.FormField
import com.wirewaypro.app.ui.components.SecondaryButton
import com.wirewaypro.app.ui.components.SectionCard
import com.wirewaypro.app.ui.components.SectionEyebrow
import com.wirewaypro.app.ui.theme.Spacing
import com.wirewaypro.app.ui.util.Format

/**
 * Standalone labor calculator — turns hours × billed rate into a labor cost, with an
 * offline regional-rate helper so a contractor without a saved rate has a defensible
 * starting point. The contractor always sets the final number; this only accelerates.
 */
@Composable
fun LaborCalcScreen(onBack: () -> Unit) {
    var hoursPerUnit by remember { mutableStateOf("") }
    var qty by remember { mutableStateOf("1") }
    var rate by remember { mutableStateOf("") }
    val states = remember { RegionalLaborRates.allStates() }
    var stateBand by remember { mutableStateOf(states.firstOrNull()) }

    val h = parseNum(hoursPerUnit)
    val n = parseNum(qty) ?: 1.0
    val r = parseNum(rate)
    val totalHours = if (h != null) h * n else null
    val laborCost = if (totalHours != null && r != null) totalHours * r else null

    DetailScaffold(title = "Labor calculator", onBack = onBack, isLoading = false, error = null) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.screen, vertical = Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            SectionCard(title = "Labor") {
                FormField(hoursPerUnit, { hoursPerUnit = it }, "Labor hours per unit", keyboardType = KeyboardType.Number)
                Spacer(Modifier.height(Spacing.sm))
                FormField(qty, { qty = it }, "Quantity of units", keyboardType = KeyboardType.Number)
                Spacer(Modifier.height(Spacing.sm))
                FormField(rate, { rate = it }, "Billed hourly rate ($)", keyboardType = KeyboardType.Number)
            }

            // Offline regional rate helper.
            SectionCard(title = "Suggested rate (optional)") {
                CalcDropdown(
                    label = "State",
                    options = states,
                    selected = stateBand ?: states.first(),
                    optionLabel = { it.stateName },
                    onSelect = { stateBand = it },
                )
                stateBand?.let { band ->
                    Spacer(Modifier.height(Spacing.sm))
                    Text(
                        "${band.stateName}: ${Format.money(band.low.toDouble())}–${Format.money(band.high.toDouble())}/hr " +
                            "(typical ${Format.money(band.typical.toDouble())})",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(Spacing.sm))
                    SecondaryButton(
                        text = "Use ${Format.money(band.typical.toDouble())}/hr",
                        onClick = { rate = band.typical.toString() },
                    )
                }
            }

            if (laborCost == null) {
                ResultBanner("—", "Enter hours and a rate", ResultStatus.NEUTRAL)
            } else {
                ResultBanner(
                    value = Format.money(laborCost),
                    caption = "${hoursText(totalHours!!)} at ${Format.money(r!!)}/hr",
                    status = ResultStatus.NEUTRAL,
                )
                SectionCard {
                    BreakdownRow("Hours per unit", hoursText(h!!))
                    BreakdownRow("Quantity", trimNum(n))
                    BreakdownRow("Total hours", hoursText(totalHours), emphasize = true)
                    BreakdownRow("Rate", "${Format.money(r)}/hr")
                    BreakdownRow("Labor cost", Format.money(laborCost), emphasize = true)
                }
            }

            SectionEyebrow("How this is figured")
            Text(
                "Labor cost = labor hours × quantity × billed hourly rate. The rate band is an offline " +
                    "approximation of a licensed contractor's client-facing billed rate (which carries overhead, " +
                    "insurance, vehicle, and profit — well above an electrician's wage), scaled from public BLS " +
                    "wage indices by state. It's a starting point you edit, never a floor or ceiling.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun hoursText(h: Double): String =
    (if (h % 1.0 == 0.0) h.toLong().toString() else String.format("%.2f", h)) + " hr"

private fun trimNum(v: Double): String =
    if (v % 1.0 == 0.0) v.toLong().toString() else String.format("%.1f", v)
