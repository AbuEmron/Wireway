package com.wirewaypro.app.ui.quotes

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wirewaypro.app.domain.catalog.Catalog
import com.wirewaypro.app.domain.model.FreeLimits
import com.wirewaypro.app.domain.model.PricingRecommendation
import com.wirewaypro.app.domain.model.QuoteCalculator
import com.wirewaypro.app.domain.model.QuoteCatalogEntry
import com.wirewaypro.app.domain.model.RateMode
import com.wirewaypro.app.domain.model.Tier
import com.wirewaypro.app.ui.components.DateField
import com.wirewaypro.app.ui.components.FormField
import com.wirewaypro.app.ui.components.GlassCard
import com.wirewaypro.app.ui.components.SaveTopBar
import com.wirewaypro.app.ui.components.SectionCard
import com.wirewaypro.app.ui.components.SectionHeader
import com.wirewaypro.app.ui.components.UpgradePrompt
import com.wirewaypro.app.ui.util.Format

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuoteBuilderScreen(
    onClose: () -> Unit,
    onOpenSubscription: () -> Unit = {},
    viewModel: QuoteBuilderViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showCatalog by remember { mutableStateOf(false) }
    var showAdvisor by remember { mutableStateOf(false) }

    val locationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> if (granted) viewModel.useMyLocation() }

    fun requestGps() {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        if (granted) viewModel.useMyLocation() else locationPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

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
            // Free hit the saved-quote ceiling on Save — the natural Pro moment
            // ("I'm bidding regularly"). Nothing typed here is lost: the builder
            // autosaves a local draft, so the quote is waiting after the upgrade.
            if (state.quoteCapReached) {
                UpgradePrompt(
                    hook = "You're bidding for real now",
                    detail = "Free includes ${FreeLimits.MAX_QUOTES} saved quotes and you've used them all. " +
                        "Pro is unlimited — quotes, invoices, clients. This draft is saved on your phone " +
                        "and will be right here.",
                    tier = Tier.PRO,
                    onUpgrade = onOpenSubscription,
                )
            }

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
                state.rateHint?.let { hint ->
                    Spacer(Modifier.padding(top = 6.dp))
                    Text(
                        hint,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.padding(top = 12.dp))
                SwitchRow("Charge sales tax (on materials)", state.taxEnabled, viewModel::setTaxEnabled)
                if (state.taxEnabled) {
                    Spacer(Modifier.padding(top = 10.dp))
                    FormField(state.taxRatePct, viewModel::setTaxRatePct, "Tax rate %", keyboardType = KeyboardType.Number)
                }
                if (!state.isInvoice) {
                    Spacer(Modifier.padding(top = 12.dp))
                    FormField(state.depositPct, viewModel::setDepositPct, "Deposit % to accept (optional)", keyboardType = KeyboardType.Number)
                    val dep = state.depositPct.trim().toDoubleOrNull()
                    if (dep != null && dep > 0) {
                        Text(
                            "Client pays " + Format.money(previewDeposit(dep, totals.headlineTotal(state.rateMode, hourlyRate, state.taxEnabled, taxRate))) + " up front to accept.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Spacer(Modifier.padding(top = 12.dp))
                OutlinedButton(onClick = { showAdvisor = true }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Outlined.AutoAwesome, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                    Text("Suggest a price for this area")
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

            // The number the electrician actually hands over — lifted onto the
            // premium glass surface so it's unmistakably the headline of the screen.
            GlassCard {
                SectionHeader("Totals")
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

            // Deterministic sanity checks (doctrine: rules off the templates,
            // never an AI hunch). Advisory only — they never block a save.
            val sanityFlags = viewModel.sanityFlags
            if (sanityFlags.isNotEmpty()) {
                GlassCard {
                    SectionHeader("Heads-up")
                    sanityFlags.forEach { flag ->
                        Text(
                            "•  ${flag.message}",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(bottom = 4.dp),
                        )
                    }
                    Text(
                        "Rule-based checks that show their work — advisory only, never a blocker.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (state.error != null) {
                Text(state.error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }

    if (showAdvisor) {
        PricingAdvisorSheet(
            state = state,
            onLocationChange = viewModel::setLocationInput,
            onUseGps = { requestGps() },
            onGetSuggestion = viewModel::requestPricing,
            onApply = { viewModel.applyAdvice(); showAdvisor = false },
            onDismiss = { viewModel.dismissAdvice(); showAdvisor = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PricingAdvisorSheet(
    state: QuoteBuilderUiState,
    onLocationChange: (String) -> Unit,
    onUseGps: () -> Unit,
    onGetSuggestion: () -> Unit,
    onApply: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(10.dp))
                Text("AI pricing suggestion", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            Text(
                "Tell me where the job is and I'll suggest a price that fits the local market. " +
                    "It's just a starting point — you set what your work is worth.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            OutlinedTextField(
                value = state.locationInput,
                onValueChange = onLocationChange,
                label = { Text("Job location (city, state or address)") },
                placeholder = { Text("e.g. Binghamton, NY") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedButton(onClick = onUseGps, enabled = !state.locatingArea, modifier = Modifier.fillMaxWidth()) {
                if (state.locatingArea) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("Finding your location…")
                } else {
                    Icon(Icons.Outlined.MyLocation, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                    Text("Use my GPS location")
                }
            }

            Button(
                onClick = onGetSuggestion,
                enabled = !state.advising,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (state.advising) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                    Spacer(Modifier.width(8.dp))
                    Text("Searching live rates…")
                } else {
                    Text("Get a suggestion")
                }
            }

            if (state.advising) {
                Text(
                    "Searching live local rates for your area — this takes about 30–60 seconds.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            state.adviceError?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }

            state.advice?.let { rec -> AdviceResult(rec, state.locationInput, onApply, onDismiss) }
        }
    }
}

@Composable
private fun AdviceResult(
    rec: PricingRecommendation,
    area: String,
    onApply: () -> Unit,
    onDismiss: () -> Unit,
) {
    val areaLabel = area.trim().ifBlank { "your area" }
    SectionCard {
        rec.recommendedTotal?.let { total ->
            Text(
                "Jobs like this around $areaLabel tend to run about ${Format.money(total)}.",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Spacer(Modifier.padding(top = 6.dp))
        val modeLine = if (rec.mode == RateMode.HOURLY) {
            rec.recommendedRate?.let { "Suggested: hourly at ${Format.money(it)}/hr" } ?: "Suggested: hourly"
        } else {
            "Suggested: flat rate"
        }
        Text(modeLine, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
        if (rec.lowTotal != null && rec.highTotal != null) {
            Text(
                "Typical range: ${Format.money(rec.lowTotal)} – ${Format.money(rec.highTotal)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.padding(top = 4.dp))
        val confidence = rec.confidence?.takeIf { it.isNotBlank() }
            ?.replaceFirstChar { it.uppercase() }
        Text(
            "Live-searched estimate" + (confidence?.let { " · $it confidence" } ?: "") + " — not a guaranteed quote.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
        )
        if (rec.areaContext.isNotBlank()) {
            Spacer(Modifier.padding(top = 6.dp))
            Text(rec.areaContext, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (rec.reasoning.isNotBlank()) {
            Spacer(Modifier.padding(top = 4.dp))
            Text(rec.reasoning, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.padding(top = 10.dp))
        Text(
            "Here's our suggestion — adjust it to what your work is worth.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.padding(top = 12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = onApply, modifier = Modifier.weight(1f)) { Text("Use this") }
            OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Keep mine") }
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

/** Whole-% deposit preview of a headline total, rounded to cents. */
private fun previewDeposit(pct: Double, total: Double): Double =
    kotlin.math.round(total * pct) / 100.0
