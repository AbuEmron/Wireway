package com.wirewaypro.app.ui.quotes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wirewaypro.app.ui.components.FormField
import com.wirewaypro.app.ui.components.SaveTopBar
import com.wirewaypro.app.ui.components.SectionCard
import com.wirewaypro.app.ui.util.Format

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuoteBuilderScreen(
    onClose: () -> Unit,
    viewModel: QuoteBuilderViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    // Leave the screen once the save succeeds.
    androidx.compose.runtime.LaunchedEffect(state.saved) {
        if (state.saved) onClose()
    }

    val kind = if (state.isInvoice) "Invoice" else "Estimate"
    val titleVerb = if (state.isEdit) "Edit" else "New"

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            SaveTopBar(
                title = "$titleVerb $kind",
                onBack = onClose,
                onSave = viewModel::save,
                saveEnabled = !state.isSaving,
                saving = state.isSaving,
            )
        },
    ) { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        val totals = viewModel.previewTotals

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            state.quoteNumber?.let {
                Text(
                    text = "#$it",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            SectionCard(title = "Client") {
                FormField(state.clientName, viewModel::setClientName, "Client name")
                Spacer(Modifier.padding(top = 10.dp))
                FormField(state.jobName, viewModel::setJobName, "Job name")
                Spacer(Modifier.padding(top = 10.dp))
                FormField(state.clientEmail, viewModel::setClientEmail, "Email", keyboardType = KeyboardType.Email)
                Spacer(Modifier.padding(top = 10.dp))
                FormField(state.clientPhone, viewModel::setClientPhone, "Phone", keyboardType = KeyboardType.Phone)
            }

            SectionCard(title = "Line items") {
                state.items.forEachIndexed { index, item ->
                    LineItemEditor(
                        item = item,
                        canRemove = state.items.size > 1,
                        onChange = { viewModel.updateItem(index, it) },
                        onRemove = { viewModel.removeItem(index) },
                    )
                    Spacer(Modifier.padding(top = 8.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(Modifier.padding(top = 8.dp))
                }
                OutlinedButton(onClick = viewModel::addItem, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                    Text("Add line item")
                }
            }

            SectionCard(title = "Pricing") {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FormField(state.markupPct, viewModel::setMarkupPct, "Markup %", Modifier.weight(1f), KeyboardType.Number)
                    FormField(state.hourlyRate, viewModel::setHourlyRate, "Hourly $", Modifier.weight(1f), KeyboardType.Number)
                }
                Spacer(Modifier.padding(top = 12.dp))
                SwitchRow("Charge sales tax (on materials)", state.taxEnabled, viewModel::setTaxEnabled)
                if (state.taxEnabled) {
                    Spacer(Modifier.padding(top = 10.dp))
                    FormField(state.taxRatePct, viewModel::setTaxRatePct, "Tax rate %", keyboardType = KeyboardType.Number)
                }
            }

            SectionCard(title = "Document") {
                SwitchRow("Save as invoice", state.isInvoice, viewModel::setInvoiceMode)
                if (state.isInvoice) {
                    Spacer(Modifier.padding(top = 10.dp))
                    FormField(state.invoiceDueDate, viewModel::setInvoiceDueDate, "Due date (YYYY-MM-DD)")
                    Spacer(Modifier.padding(top = 10.dp))
                    SwitchRow("Paid", state.invoicePaid, viewModel::setInvoicePaid)
                }
                Spacer(Modifier.padding(top = 10.dp))
                FormField(state.notes, viewModel::setNotes, "Notes", singleLine = false)
            }

            SectionCard(title = "Totals") {
                TotalRow("Materials", Format.money(totals.totalMaterial))
                TotalRow("Labor", Format.money(totals.totalLabor))
                TotalRow("Markup", Format.money(totals.markupAmount))
                if (state.taxEnabled) TotalRow("Tax", Format.money(totals.taxAmount))
                Spacer(Modifier.padding(top = 6.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(Modifier.padding(top = 6.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Total", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        Format.money(totals.total),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            if (state.error != null) {
                Text(
                    text = state.error!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun LineItemEditor(
    item: CustomItemUi,
    canRemove: Boolean,
    onChange: ((CustomItemUi) -> CustomItemUi) -> Unit,
    onRemove: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        FormField(item.label, { v -> onChange { it.copy(label = v) } }, "Description", Modifier.weight(1f))
        if (canRemove) {
            IconButton(onClick = onRemove) {
                Icon(
                    Icons.Outlined.Delete,
                    contentDescription = "Remove line",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
    Spacer(Modifier.padding(top = 8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FormField(item.qty, { v -> onChange { it.copy(qty = v) } }, "Qty", Modifier.weight(1f), KeyboardType.Number)
        FormField(item.materialCost, { v -> onChange { it.copy(materialCost = v) } }, "Mat $", Modifier.weight(1f), KeyboardType.Number)
    }
    Spacer(Modifier.padding(top = 8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FormField(item.laborCost, { v -> onChange { it.copy(laborCost = v) } }, "Labor $", Modifier.weight(1f), KeyboardType.Number)
        FormField(item.laborHours, { v -> onChange { it.copy(laborHours = v) } }, "Hours", Modifier.weight(1f), KeyboardType.Number)
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun TotalRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}
