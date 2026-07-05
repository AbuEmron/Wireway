package com.wirewaypro.app.ui.quotes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.wirewaypro.app.domain.catalog.Assembly
import com.wirewaypro.app.domain.catalog.AssemblyItem
import com.wirewaypro.app.domain.catalog.Catalog
import com.wirewaypro.app.domain.catalog.JobWalk
import com.wirewaypro.app.domain.model.QuoteCalculator
import com.wirewaypro.app.domain.model.Tier
import com.wirewaypro.app.ui.components.BackTopBar
import com.wirewaypro.app.ui.components.FormField
import com.wirewaypro.app.ui.components.GradientButton
import com.wirewaypro.app.ui.components.SectionHeader
import com.wirewaypro.app.ui.components.UpgradePrompt
import com.wirewaypro.app.ui.util.Format

/** One area in the review UI: its template and how many of it. */
data class ReviewArea(val assembly: Assembly, val count: Int)

private const val PREVIEW_RATE = 85.0 // catalog base rate; the builder uses the user's rate

/**
 * The job-walk review: every area with a quantity stepper, and below it the
 * deterministic expansion — the exact catalog lines, quantities, labor hours and
 * a preview subtotal the walk builds. This is the "show the math" moment: nothing
 * here is guessed, every number traces to [Catalog].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalkReviewSheet(
    areas: List<ReviewArea>,
    merged: JobWalk.Merged,
    tier: Tier?,
    requiredTier: Tier,
    onSetCount: (assemblyId: String, count: Int) -> Unit,
    onRemove: (assemblyId: String) -> Unit,
    onBuild: () -> Unit,
    onUpgrade: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text("Job walk", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Text(
                "Set how many of each area you walked — the materials and labor auto-build below.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(8.dp))
            areas.forEach { area ->
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(area.assembly.label, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                        Text(
                            "${area.assembly.itemCount} line ${if (area.assembly.itemCount == 1) "item" else "items"} each",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Stepper(
                        count = area.count,
                        onMinus = { onSetCount(area.assembly.id, area.count - 1) },
                        onPlus = { onSetCount(area.assembly.id, area.count + 1) },
                    )
                    IconButton(onClick = { onRemove(area.assembly.id) }) {
                        Icon(Icons.Outlined.Delete, contentDescription = "Remove area", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Spacer(Modifier.height(6.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            SectionHeader("What this builds")

            var runningTotal = 0.0
            merged.entries.forEach { entry ->
                val svc = Catalog.service(entry.serviceId) ?: return@forEach
                val variant = svc.variants.getOrNull(entry.variantIdx)?.label?.takeIf { svc.variants.size > 1 }
                val amount = QuoteCalculator.catalogLineAmount(entry, PREVIEW_RATE) ?: 0.0
                runningTotal += amount
                val hrs = JobWalk.laborHoursFor(entry)
                MathRow(
                    qty = entry.qty,
                    label = svc.label + (variant?.let { " — $it" } ?: ""),
                    hours = hrs,
                    amount = amount,
                )
            }
            merged.customItems.forEach { c ->
                val labor = c.laborHours * c.qty * PREVIEW_RATE
                MathRow(qty = c.qty, label = c.label + " (supplier-quoted material)", hours = c.laborHours * c.qty, amount = labor)
            }

            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Total labor", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("${trim(JobWalk.totalLaborHours(merged))} hrs", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Preview subtotal", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text(Format.money(runningTotal), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
            Text(
                "Priced at the \$${PREVIEW_RATE.toInt()}/hr base rate. Your rate, every quantity and every price stay editable in the builder.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(10.dp))
            when (tier?.atLeast(requiredTier)) {
                true -> GradientButton(text = "Build one estimate", onClick = onBuild, modifier = Modifier.fillMaxWidth())
                false -> UpgradePrompt(
                    hook = "Turn the whole job walk into one estimate",
                    detail = if (requiredTier == Tier.ELITE) {
                        "This walk includes commercial/industrial areas — building it is part of Elite."
                    } else {
                        "Building an estimate from your job-walk areas is part of Pro — unlimited quotes, branded PDFs, get-paid links."
                    },
                    tier = requiredTier,
                    onUpgrade = onUpgrade,
                )
                null -> GradientButton(text = "Build one estimate", onClick = {}, loading = true, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun Stepper(count: Int, onMinus: () -> Unit, onPlus: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onMinus) { Icon(Icons.Filled.Remove, contentDescription = "Fewer", tint = MaterialTheme.colorScheme.primary) }
        Text("$count", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(24.dp).padding(top = 2.dp))
        IconButton(onClick = onPlus) { Icon(Icons.Filled.Add, contentDescription = "More", tint = MaterialTheme.colorScheme.primary) }
    }
}

@Composable
private fun MathRow(qty: Double, label: String, hours: Double, amount: Double) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.Top) {
        Text("${trim(qty)} ×", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.size(8.dp))
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            Text("${trim(hours)} labor hrs", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(Format.money(amount), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/**
 * Create/edit a contractor template from REAL catalog line items — the user
 * builds their own "job in a box" once and it's a one-tap area forever after.
 * No prices are entered here (they come from [Catalog] deterministically); the
 * contractor picks services and sets quantities.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplateEditorScreen(
    initialName: String,
    initialDescription: String,
    initialItems: List<AssemblyItem>,
    onSave: (name: String, description: String, items: List<AssemblyItem>) -> Unit,
    onClose: () -> Unit,
) {
    var name by remember { mutableStateOf(initialName) }
    var description by remember { mutableStateOf(initialDescription) }
    val items = remember { androidx.compose.runtime.mutableStateListOf<AssemblyItem>().apply { addAll(initialItems) } }
    var showCatalog by remember { mutableStateOf(false) }

    if (showCatalog) {
        CatalogPicker(
            selectedIds = items.map { it.serviceId }.toSet(),
            onAdd = { id -> if (items.none { it.serviceId == id }) items.add(AssemblyItem(id, 1.0, 0)) },
            onClose = { showCatalog = false },
        )
        return
    }

    androidx.compose.material3.Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { BackTopBar(title = if (initialName.isBlank()) "New template" else "Edit template", onBack = onClose) },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            FormField(name, { name = it }, "Template name (e.g. My standard bedroom)")
            FormField(description, { description = it }, "Description (optional)", singleLine = false)

            SectionHeader("Line items")
            if (items.isEmpty()) {
                Text(
                    "Add catalog services — each becomes a real, priced line when the estimate opens.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            items.forEachIndexed { idx, item ->
                val svc = Catalog.service(item.serviceId)
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(svc?.label ?: item.serviceId, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                        svc?.let { Text(it.nec, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                    Stepper(
                        count = item.qty.toInt().coerceAtLeast(1),
                        onMinus = { items[idx] = item.copy(qty = (item.qty - 1).coerceAtLeast(1.0)) },
                        onPlus = { items[idx] = item.copy(qty = item.qty + 1) },
                    )
                    IconButton(onClick = { items.removeAt(idx) }) {
                        Icon(Icons.Outlined.Delete, contentDescription = "Remove line", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            OutlinedButton(onClick = { showCatalog = true }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                Text("Add catalog line")
            }

            Spacer(Modifier.height(8.dp))
            GradientButton(
                text = "Save template",
                onClick = { onSave(name.trim(), description.trim(), items.toList()) },
                enabled = name.isNotBlank() && items.isNotEmpty(),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

private fun trim(v: Double): String = if (v % 1.0 == 0.0) v.toLong().toString() else String.format("%.2f", v)
