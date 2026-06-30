package com.wirewaypro.app.ui.quotes

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wirewaypro.app.domain.catalog.Catalog
import com.wirewaypro.app.domain.model.QuoteCalculator
import com.wirewaypro.app.domain.model.QuoteCatalogEntry
import com.wirewaypro.app.domain.model.RateMode
import com.wirewaypro.app.ui.components.DateField
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
    var showCatalog by remember { mutableStateOf(false) }

    LaunchedEffect(state.saved) { if (state.saved) onClose() }

    if (showCatalog) {
        CatalogPicker(
            selectedIds = state.catalogItems.map { it.serviceId }.toSet(),
            onAdd = viewModel::addCatalogEntry,
            onClose = { showCatalog = false },
        )
        return
    }

    val kind = if (state.isInvoice) "Invoice" else "Estimate"
    val titleVerb = if (state.isEdit) "Edit" else "New"
    val hourlyRate = state.hourlyRate.trim().toDoubleOrNull()?.takeIf { it > 0 } ?: 85.0
    val taxRate = state.taxRatePct.trim().toDoubleOrNull()?.div(100.0) ?: 0.0

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
                Text("#$it", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
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

            SectionCard(title = "Catalog items") {
                if (state.catalogItems.isEmpty()) {
                    Text(
                        "No catalog items. Browse the NEC service catalog to add priced work.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    state.catalogItems.forEachIndexed { index, item ->
                        CatalogItemEditor(
                            item = item,
                            hourlyRate = hourlyRate,
                            onChange = { viewModel.updateCatalogEntry(index, it) },
                            onRemove = { viewModel.removeCatalogEntry(index) },
                        )
                        Spacer(Modifier.padding(top = 8.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Spacer(Modifier.padding(top = 8.dp))
                    }
                }
                OutlinedButton(onClick = { showCatalog = true }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                    Text("Add catalog item")
                }
            }

            SectionCard(title = "Custom items") {
                state.items.forEachIndexed { index, item ->
                    LineItemEditor(
                        item = item,
                        onChange = { viewModel.updateItem(index, it) },
                        onRemove = { viewModel.removeItem(index) },
                    )
                    Spacer(Modifier.padding(top = 8.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(Modifier.padding(top = 8.dp))
                }
                OutlinedButton(onClick = viewModel::addItem, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                    Text("Add custom item")
                }
            }

            SectionCard(title = "Pricing") {
                Text(
                    "How are you pricing this job?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.padding(top = 8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = state.rateMode == RateMode.FLAT,
                        onClick = { viewModel.setRateMode(RateMode.FLAT) },
                        label = { Text("Flat rate") },
                    )
                    FilterChip(
                        selected = state.rateMode == RateMode.HOURLY,
                        onClick = { viewModel.setRateMode(RateMode.HOURLY) },
                        label = { Text("Hourly") },
                    )
                }
                if (state.rateMode == RateMode.HOURLY) {
                    Spacer(Modifier.padding(top = 10.dp))
                    Text(
                        "Who supplies the materials?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.padding(top = 6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = !state.clientBuysAll,
                            onClick = { viewModel.setClientBuysAll(false) },
                            label = { Text("Labor + materials") },
                        )
                        FilterChip(
                            selected = state.clientBuysAll,
                            onClick = { viewModel.setClientBuysAll(true) },
                            label = { Text("Just labor") },
                        )
                    }
                }
                Spacer(Modifier.padding(top = 12.dp))
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
                    DateField("Due date", state.invoiceDueDate, viewModel::setInvoiceDueDate)
                    Spacer(Modifier.padding(top = 10.dp))
                    SwitchRow("Paid", state.invoicePaid, viewModel::setInvoicePaid)
                }
                Spacer(Modifier.padding(top = 10.dp))
                FormField(state.notes, viewModel::setNotes, "Notes", singleLine = false)
                Spacer(Modifier.padding(top = 8.dp))
                OutlinedButton(
                    onClick = viewModel::draftNotes,
                    enabled = !state.draftingNotes,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (state.draftingNotes) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text("Drafting…")
                    } else {
                        Icon(Icons.Outlined.AutoAwesome, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                        Text("Draft notes with AI")
                    }
                }
            }

            SectionCard(title = "Totals") {
                val headline = totals.headlineTotal(state.rateMode, hourlyRate, state.taxEnabled, taxRate)
                if (state.rateMode == RateMode.HOURLY) {
                    TotalRow("Estimated time", "${hoursText(totals.totalHours)} hrs")
                    TotalRow("Hourly rate", Format.money(hourlyRate))
                    TotalRow("Labor", Format.money(totals.totalHours * hourlyRate))
                    if (!state.clientBuysAll) {
                        TotalRow("Materials", Format.money(totals.totalMaterial))
                        if (state.taxEnabled) TotalRow("Tax", Format.money(totals.totalMaterial * taxRate))
                    }
                } else {
                    TotalRow("Materials", Format.money(totals.totalMaterial))
                    TotalRow("Labor", Format.money(totals.totalLabor))
                    TotalRow("Markup", Format.money(totals.markupAmount))
                    if (state.taxEnabled) TotalRow("Tax", Format.money(totals.taxAmount))
                }
                Spacer(Modifier.padding(top = 6.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(Modifier.padding(top = 6.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Total", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        Format.money(headline),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                if (state.rateMode == RateMode.HOURLY) {
                    Spacer(Modifier.padding(top = 6.dp))
                    Text(
                        if (state.clientBuysAll) "Just labor — the client supplies the materials."
                        else "Labor + materials you supply.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (state.error != null) {
                Text(state.error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun CatalogItemEditor(
    item: CatalogEntryUi,
    hourlyRate: Double,
    onChange: ((CatalogEntryUi) -> CatalogEntryUi) -> Unit,
    onRemove: () -> Unit,
) {
    val service = Catalog.service(item.serviceId)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            service?.label ?: item.serviceId,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onRemove) {
            Icon(Icons.Outlined.Delete, contentDescription = "Remove", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    if (service != null && service.variants.size > 1) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            service.variants.forEachIndexed { idx, variant ->
                FilterChip(
                    selected = idx == item.variantIdx,
                    onClick = { onChange { it.copy(variantIdx = idx) } },
                    label = { Text(variant.label) },
                )
            }
        }
    }
    Spacer(Modifier.padding(top = 6.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        FormField(item.qty, { v -> onChange { it.copy(qty = v) } }, "Qty", Modifier.weight(1f), KeyboardType.Number)
        Spacer(Modifier.width(12.dp))
        val amount = QuoteCalculator.catalogLineAmount(
            QuoteCatalogEntry(item.serviceId, item.qty.trim().toDoubleOrNull() ?: 0.0, item.variantIdx, item.clientBuys),
            hourlyRate,
        )
        Text(
            Format.money(amount),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun LineItemEditor(
    item: CustomItemUi,
    onChange: ((CustomItemUi) -> CustomItemUi) -> Unit,
    onRemove: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        FormField(item.label, { v -> onChange { it.copy(label = v) } }, "Description", Modifier.weight(1f))
        IconButton(onClick = onRemove) {
            Icon(Icons.Outlined.Delete, contentDescription = "Remove line", tint = MaterialTheme.colorScheme.onSurfaceVariant)
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

/** "12.0" -> "12", "12.5" -> "12.5" for the estimated-hours display. */
private fun hoursText(h: Double): String =
    if (h % 1.0 == 0.0) h.toLong().toString() else String.format("%.1f", h)
