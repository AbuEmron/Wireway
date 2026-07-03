package com.wirewaypro.app.ui.quotes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wirewaypro.app.domain.catalog.Assembly
import com.wirewaypro.app.domain.catalog.AssemblySector
import com.wirewaypro.app.domain.catalog.Catalog
import com.wirewaypro.app.domain.model.Tier
import com.wirewaypro.app.ui.components.BackTopBar
import com.wirewaypro.app.ui.components.FormField
import com.wirewaypro.app.ui.components.GradientButton
import com.wirewaypro.app.ui.components.ListCard
import com.wirewaypro.app.ui.components.SectionHeader
import com.wirewaypro.app.ui.components.UpgradePrompt

/**
 * Job-template picker — the fast path to an estimate without AI. The whole
 * library is browsable and searchable on every tier; tapping a template opens
 * a detail sheet listing the complete "job in a box", and STARTING from it is
 * the gated moment (residential = Pro, commercial/industrial = Elite).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssembliesScreen(
    onBack: () -> Unit,
    onPicked: () -> Unit,
    onOpenSubscription: () -> Unit = {},
    viewModel: AssembliesViewModel = hiltViewModel(),
) {
    var query by remember { mutableStateOf("") }
    val q = query.trim().lowercase()
    val tier by viewModel.tier.collectAsStateWithLifecycle()
    val walk by viewModel.walk.collectAsStateWithLifecycle()

    var detail by remember { mutableStateOf<Assembly?>(null) }
    detail?.let { assembly ->
        TemplateDetailSheet(
            assembly = assembly,
            tier = tier,
            requiredTier = viewModel.requiredTier(assembly),
            inWalk = assembly.id in walk,
            onStart = {
                viewModel.seed(assembly)
                detail = null
                onPicked()
            },
            onToggleWalk = {
                viewModel.toggleWalk(assembly)
                detail = null
            },
            onUpgrade = {
                detail = null
                onOpenSubscription()
            },
            onDismiss = { detail = null },
        )
    }

    val rows: List<TemplateRow> = remember(q) { buildTemplateRows(viewModel.assemblies, q) }

    Scaffold(
        topBar = { BackTopBar(title = "Job Templates", onBack = onBack) },
        bottomBar = {
            // The job walk in progress: areas picked while walking the job,
            // built into ONE estimate. Deterministic merge — no AI in this path.
            if (walk.isNotEmpty()) {
                androidx.compose.material3.Surface(tonalElevation = 3.dp) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                    ) {
                        GradientButton(
                            text = "Build one estimate (${walk.size} ${if (walk.size == 1) "area" else "areas"})",
                            onClick = {
                                if (tier?.atLeast(viewModel.requiredWalkTier()) == true) {
                                    viewModel.seedWalk()
                                    onPicked()
                                } else {
                                    onOpenSubscription()
                                }
                            },
                            modifier = Modifier.weight(1f),
                        )
                        androidx.compose.material3.TextButton(onClick = viewModel::clearWalk) { Text("Clear") }
                    }
                }
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            FormField(
                value = query,
                onValueChange = { query = it },
                label = "Search templates (EV, panel, motor, fire alarm…)",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            )
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(rows, key = { it.key }) { row ->
                    when (row) {
                        is TemplateRow.Header -> Text(
                            row.label.uppercase(),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                        is TemplateRow.Item -> {
                            val a = row.assembly
                            ListCard(
                                title = a.label,
                                onClick = { detail = a },
                                subtitle = a.description,
                                footerStart = buildString {
                                    append(if (a.itemCount == 1) "1 line item" else "${a.itemCount} line items")
                                    if (a.sector == AssemblySector.COMMERCIAL_INDUSTRIAL) append(" · Elite")
                                },
                            )
                        }
                    }
                }
                if (rows.isEmpty()) {
                    item(key = "empty") {
                        Text(
                            "No template matches “$query”. Try a broader term — or build it once in the estimate builder and it takes a minute next time too.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 12.dp),
                        )
                    }
                }
            }
        }
    }
}

/**
 * The "job in a box" detail: every line the template seeds, then the start CTA
 * — or, below the value it just showed, the contextual upgrade moment.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TemplateDetailSheet(
    assembly: Assembly,
    tier: Tier?,
    requiredTier: Tier,
    inWalk: Boolean,
    onStart: () -> Unit,
    onToggleWalk: () -> Unit,
    onUpgrade: () -> Unit,
    onDismiss: () -> Unit,
) {
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
                assembly.label,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                assembly.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(10.dp))
            SectionHeader("What it seeds")
            assembly.items.forEach { item ->
                val svc = Catalog.service(item.serviceId)
                if (svc != null) {
                    val variant = svc.variants.getOrNull(item.variantIdx)
                        ?.label?.takeIf { svc.variants.size > 1 }
                    LineRow(
                        qty = item.qty,
                        label = svc.label + (variant?.let { " — $it" } ?: ""),
                    )
                }
            }
            assembly.customItems.forEach { item ->
                LineRow(qty = item.qty, label = item.label, hoursEach = item.laborHours)
            }

            Spacer(Modifier.height(4.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Text(
                if (assembly.sector == AssemblySector.COMMERCIAL_INDUSTRIAL) {
                    "Labor prices at YOUR hourly rate when the estimate opens; material " +
                        "seeds at \$0 for your supplier's quote. Every quantity and hour is editable."
                } else {
                    "Every quantity, variant and price is editable once the estimate opens — " +
                        "this is a ~90% starting point, not a bid."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(10.dp))
            // Walking the job? Collect this area and keep browsing — the pinned
            // bar below builds every picked area into one estimate.
            androidx.compose.material3.OutlinedButton(onClick = onToggleWalk, modifier = Modifier.fillMaxWidth()) {
                Text(if (inWalk) "Remove from job walk" else "Add to job walk — quote several areas as one")
            }
            Spacer(Modifier.height(2.dp))
            val allowed = tier?.atLeast(requiredTier)
            when (allowed) {
                true -> GradientButton(text = "Start this estimate", onClick = onStart)
                false -> UpgradePrompt(
                    hook = if (requiredTier == Tier.ELITE) "Bid commercial work from a template" else "Quote this job in under a minute",
                    detail = if (requiredTier == Tier.ELITE) {
                        "Commercial & industrial templates are part of Elite — along with the " +
                            "commercial material library and the expanded NEC reference."
                    } else {
                        "Job templates seed a near-complete estimate you just tweak and send. " +
                            "They're part of Pro — unlimited quotes, branded PDFs, get-paid links."
                    },
                    tier = requiredTier,
                    onUpgrade = onUpgrade,
                )
                null -> GradientButton(text = "Start this estimate", onClick = {}, loading = true)
            }
        }
    }
}

@Composable
private fun LineRow(qty: Double, label: String, hoursEach: Double? = null) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(
            trimQty(qty) + " ×",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.padding(start = 8.dp))
        Text(
            label + (hoursEach?.let { "  (${trimQty(it)} hr ea)" } ?: ""),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

private sealed interface TemplateRow {
    val key: String

    data class Header(val label: String) : TemplateRow {
        override val key: String get() = "h/$label"
    }

    data class Item(val assembly: Assembly) : TemplateRow {
        override val key: String get() = assembly.id
    }
}

/** Sector → category grouped rows when unfiltered; a flat match list when searching. */
private fun buildTemplateRows(all: List<Assembly>, q: String): List<TemplateRow> {
    if (q.isNotEmpty()) {
        return all.filter { it.search.contains(q) }.map(TemplateRow::Item)
    }
    return AssemblySector.entries.flatMap { sector ->
        val inSector = all.filter { it.sector == sector }
        if (inSector.isEmpty()) return@flatMap emptyList()
        inSector.groupBy { it.category }.entries.flatMap { (category, assemblies) ->
            buildList {
                add(TemplateRow.Header("${sector.label} · $category"))
                assemblies.forEach { add(TemplateRow.Item(it)) }
            }
        }
    }
}

private fun trimQty(v: Double): String =
    if (v % 1.0 == 0.0) v.toLong().toString() else v.toString()
