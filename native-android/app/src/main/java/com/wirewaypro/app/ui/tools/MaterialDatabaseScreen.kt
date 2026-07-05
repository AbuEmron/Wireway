package com.wirewaypro.app.ui.tools

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wirewaypro.app.domain.catalog.Catalog
import com.wirewaypro.app.domain.catalog.CatalogService
import com.wirewaypro.app.domain.catalog.EliteCatalog
import com.wirewaypro.app.domain.catalog.EliteMaterial
import com.wirewaypro.app.domain.model.Tier
import com.wirewaypro.app.ui.components.DetailScaffold
import com.wirewaypro.app.ui.components.EmptyState
import com.wirewaypro.app.ui.components.FormField
import com.wirewaypro.app.ui.components.InfoRow
import com.wirewaypro.app.ui.components.SectionHeader
import com.wirewaypro.app.ui.components.UpgradePrompt
import com.wirewaypro.app.ui.theme.Spacing
import com.wirewaypro.app.ui.util.Format

/**
 * Standalone material & labor database. The NEC-2023 residential service catalog
 * (deterministic costs, backs the estimate builder) is available on every tier;
 * the commercial + industrial library (truthful specs, market/quote price basis —
 * never fabricated dollar figures) is the Elite layer.
 */
@Composable
fun MaterialDatabaseScreen(
    onBack: () -> Unit,
    onOpenSubscription: () -> Unit = {},
    viewModel: MaterialDatabaseViewModel = hiltViewModel(),
) {
    var query by remember { mutableStateOf("") }
    val q = query.trim().lowercase()
    val tier by viewModel.tier.collectAsStateWithLifecycle()
    val isElite = tier?.atLeast(Tier.ELITE) == true

    // Flat, category-tagged rows so search can span the whole catalog while a blank
    // query still reads as grouped sections.
    val rows: List<CatalogRow> = remember(q, isElite) { buildRows(q, isElite) }

    // Tapped card → full-detail bottom sheet (same pattern as the pull list).
    var serviceDetail by remember { mutableStateOf<CatalogService?>(null) }
    var eliteDetail by remember { mutableStateOf<EliteMaterial?>(null) }
    serviceDetail?.let { ServiceDetailSheet(it, onDismiss = { serviceDetail = null }) }
    eliteDetail?.let { EliteMaterialDetailSheet(it, onDismiss = { eliteDetail = null }) }

    DetailScaffold(title = "Material database", onBack = onBack, isLoading = false, error = null) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            com.wirewaypro.app.ui.components.SearchField(
                value = query,
                onValueChange = { query = it },
                placeholder = "Search materials, services, NEC article",
                modifier = Modifier.padding(horizontal = Spacing.screen, vertical = Spacing.md),
            )
            if (rows.isEmpty()) {
                EmptyState(
                    title = "No matches",
                    message = "Nothing in the catalog matches “$query”. Try a broader term.",
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        start = Spacing.screen,
                        end = Spacing.screen,
                        bottom = Spacing.xxl,
                    ),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    items(rows) { row ->
                        when (row) {
                            is CatalogRow.Header -> Text(
                                text = row.label.uppercase(),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = Spacing.md, bottom = Spacing.xxs),
                            )
                            is CatalogRow.Item -> MaterialCard(row.service, onClick = { serviceDetail = row.service })
                            is CatalogRow.EliteItem -> EliteMaterialCard(row.material, onClick = { eliteDetail = row.material })
                            CatalogRow.ElitePricingNote -> Text(
                                "Commercial & industrial gear is market- and quote-priced (copper moves " +
                                    "daily; switchgear is engineered to order), so no list prices are shown — " +
                                    "they'd be wrong. Each entry states how the item is sold; price it with " +
                                    "your distributor at bid time.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = Spacing.xxs),
                            )
                            CatalogRow.EliteUpsell -> UpgradePrompt(
                                hook = "Bidding commercial or industrial?",
                                detail = "Elite adds the full commercial + industrial library — " +
                                    "277/480 V distribution, switchgear, motor control and VFDs, busway, " +
                                    "cable tray, fire alarm, PLC/controls and hazardous-location gear — " +
                                    "with truthful specs, NEC references and how each item is actually sold.",
                                tier = Tier.ELITE,
                                onUpgrade = onOpenSubscription,
                                modifier = Modifier.padding(top = Spacing.md),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MaterialCard(service: CatalogService, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(Modifier.padding(Spacing.lg)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    service.label,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                NecTagPill(service.nec)
            }
            Spacer(Modifier.height(Spacing.sm))
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.lg)) {
                Metric("Material", Format.money(service.materialCost))
                Metric("Labor", Format.money(service.laborCost))
                Metric("Hours", hoursText(service.laborHours))
                Metric("Unit", service.unit)
            }
            if (service.variants.size > 1) {
                Spacer(Modifier.height(Spacing.sm))
                Text(
                    "Variants: " + service.variants.joinToString(", ") { it.label },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun EliteMaterialCard(material: EliteMaterial, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(Modifier.padding(Spacing.lg)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    material.label,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                NecTagPill(material.nec)
            }
            Spacer(Modifier.height(Spacing.xs))
            Text(
                material.spec,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(Spacing.sm))
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.lg)) {
                Metric("Unit", material.unit)
                Metric("Pricing", "Live / quoted")
            }
        }
    }
}

@Composable
private fun Metric(label: String, value: String) {
    Column {
        Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private sealed interface CatalogRow {
    data class Header(val label: String) : CatalogRow
    data class Item(val service: CatalogService) : CatalogRow
    data class EliteItem(val material: EliteMaterial) : CatalogRow
    data object ElitePricingNote : CatalogRow
    data object EliteUpsell : CatalogRow
}

/** Grouped rows when unfiltered; a flat matching list when searching. */
private fun buildRows(q: String, isElite: Boolean): List<CatalogRow> {
    if (q.isEmpty()) {
        val residential = Catalog.categories.flatMap { cat ->
            buildList {
                add(CatalogRow.Header(cat.label))
                cat.services.forEach { add(CatalogRow.Item(it)) }
            }
        }
        if (!isElite) return residential + CatalogRow.EliteUpsell
        return residential + EliteCatalog.categories.flatMapIndexed { idx, cat ->
            buildList {
                add(CatalogRow.Header("${cat.sector.label} · ${cat.label}"))
                if (idx == 0) add(CatalogRow.ElitePricingNote)
                cat.materials.forEach { add(CatalogRow.EliteItem(it)) }
            }
        }
    }
    val residential = Catalog.allServices
        .filter { it.label.lowercase().contains(q) || it.nec.lowercase().contains(q) }
        .map<CatalogService, CatalogRow>(CatalogRow::Item)
    if (!isElite) return residential
    return residential + EliteCatalog.allMaterials
        .filter {
            it.label.lowercase().contains(q) || it.nec.lowercase().contains(q) ||
                it.spec.lowercase().contains(q) || it.typicalUse.lowercase().contains(q)
        }
        .map(CatalogRow::EliteItem)
}

private fun hoursText(h: Double): String =
    (if (h % 1.0 == 0.0) h.toLong().toString() else String.format("%.2f", h)) + " hr"

/**
 * Full detail for a residential catalog service: the deterministic numbers the
 * estimate builder uses, per-variant costs computed with the calculator's own
 * math (base × variant multiplier), the NEC reference, and where to buy.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ServiceDetailSheet(service: CatalogService, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                service.label,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            NecTagPill(service.nec)

            Spacer(Modifier.height(10.dp))
            SectionHeader("Base cost (per ${service.unit})")
            InfoRow("Material", Format.money(service.materialCost))
            InfoRow("Labor", Format.money(service.laborCost))
            InfoRow("Labor hours", hoursText(service.laborHours))

            if (service.variants.size > 1) {
                Spacer(Modifier.height(10.dp))
                SectionHeader("Variants")
                // Same math as the estimate: material, labor and hours all scale
                // by the variant multiplier.
                service.variants.forEach { v ->
                    InfoRow(
                        v.label,
                        Format.money(service.materialCost * v.m) +
                            " + " + Format.money(service.laborCost * v.m) +
                            " · " + hoursText(service.laborHours * v.m),
                    )
                }
                Text(
                    "material + labor · hours",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.height(4.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Spacer(Modifier.height(4.dp))
            SectionHeader("Price basis")
            Text(
                "Catalog defaults — the same deterministic numbers the estimate builder " +
                    "uses (labor is priced at the \$85/hr base; your hourly rate rescales " +
                    "labor in estimates). Supply pricing varies by region — confirm " +
                    "big-ticket material with your supplier.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(10.dp))
            SectionHeader("Where to buy")
            Text(
                "Big-box (Home Depot, Lowe's) for common devices and wire; electrical " +
                    "distributors (Graybar, CED, City Electric Supply) for panels, " +
                    "breakers and contractor pricing.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Full detail for a commercial/industrial (Elite) entry: truthful spec, the work
 * it's for, NEC reference, unit and honest price basis, and supply channels —
 * never a fabricated dollar figure.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EliteMaterialDetailSheet(material: EliteMaterial, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                material.label,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            NecTagPill(material.nec)

            Spacer(Modifier.height(10.dp))
            SectionHeader("Spec")
            Text(
                material.spec,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Spacer(Modifier.height(10.dp))
            SectionHeader("What it's for")
            Text(
                material.typicalUse,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Spacer(Modifier.height(10.dp))
            SectionHeader("Unit & price basis")
            InfoRow("Estimating unit", material.unit)
            Text(
                material.priceBasis,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(10.dp))
            SectionHeader("Where to buy")
            material.vendors.forEach { vendor ->
                Text(
                    "• $vendor",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "No list price is shown because this gear is market- and quote-priced — " +
                    "a hardcoded number would be wrong. Price it with your distributor at bid time.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
