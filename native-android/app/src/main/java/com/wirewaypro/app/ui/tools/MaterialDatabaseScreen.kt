package com.wirewaypro.app.ui.tools

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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
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
import com.wirewaypro.app.domain.catalog.Catalog
import com.wirewaypro.app.domain.catalog.CatalogService
import com.wirewaypro.app.ui.components.DetailScaffold
import com.wirewaypro.app.ui.components.EmptyState
import com.wirewaypro.app.ui.components.FormField
import com.wirewaypro.app.ui.components.SectionEyebrow
import com.wirewaypro.app.ui.theme.Spacing
import com.wirewaypro.app.ui.util.Format

/**
 * Standalone material & labor database — the NEC-2023 residential service catalog that
 * already backs the estimate builder, now browsable on its own. Every line carries a
 * material cost, a labor cost, labor hours, and the NEC article — deterministic
 * reference an electrician can trust at the supply-house counter, offline.
 */
@Composable
fun MaterialDatabaseScreen(onBack: () -> Unit) {
    var query by remember { mutableStateOf("") }
    val q = query.trim().lowercase()

    // Flat, category-tagged rows so search can span the whole catalog while a blank
    // query still reads as grouped sections.
    val rows: List<CatalogRow> = remember(q) { buildRows(q) }

    DetailScaffold(title = "Material database", onBack = onBack, isLoading = false, error = null) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            FormField(
                value = query,
                onValueChange = { query = it },
                label = "Search materials, services, NEC article",
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
                            is CatalogRow.Item -> MaterialCard(row.service)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MaterialCard(service: CatalogService) {
    Card(
        modifier = Modifier.fillMaxWidth(),
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
private fun Metric(label: String, value: String) {
    Column {
        Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private sealed interface CatalogRow {
    data class Header(val label: String) : CatalogRow
    data class Item(val service: CatalogService) : CatalogRow
}

/** Grouped rows when unfiltered; a flat matching list when searching. */
private fun buildRows(q: String): List<CatalogRow> {
    if (q.isEmpty()) {
        return Catalog.categories.flatMap { cat ->
            buildList {
                add(CatalogRow.Header(cat.label))
                cat.services.forEach { add(CatalogRow.Item(it)) }
            }
        }
    }
    return Catalog.allServices
        .filter { it.label.lowercase().contains(q) || it.nec.lowercase().contains(q) }
        .map { CatalogRow.Item(it) }
}

private fun hoursText(h: Double): String =
    (if (h % 1.0 == 0.0) h.toLong().toString() else String.format("%.2f", h)) + " hr"
