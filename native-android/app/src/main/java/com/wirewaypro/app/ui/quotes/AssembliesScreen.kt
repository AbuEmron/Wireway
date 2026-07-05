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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Icon
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
import com.wirewaypro.app.domain.catalog.AssemblyItem
import com.wirewaypro.app.domain.catalog.AssemblySector
import com.wirewaypro.app.domain.catalog.Catalog
import com.wirewaypro.app.domain.model.Tier
import com.wirewaypro.app.domain.model.UserTemplate
import com.wirewaypro.app.ui.components.BackTopBar
import com.wirewaypro.app.ui.components.FormField
import com.wirewaypro.app.ui.components.GradientButton
import com.wirewaypro.app.ui.components.ListCard
import com.wirewaypro.app.ui.components.SectionHeader
import com.wirewaypro.app.ui.components.UpgradePrompt

/** Editor target: null = closed; a state = the template being created/edited. */
private data class EditorState(
    val assemblyId: String?,
    val name: String,
    val description: String,
    val items: List<AssemblyItem>,
)

/**
 * Job-template picker + area-based job walk. The whole library — built-ins plus
 * the contractor's own templates — is browsable and searchable on every tier.
 * Tapping a template opens a detail sheet; STARTING (or building a whole walk)
 * is the gated moment (residential/user = Pro, commercial/industrial = Elite).
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
    val library by viewModel.library.collectAsStateWithLifecycle()
    val walk by viewModel.walk.collectAsStateWithLifecycle()

    var detail by remember { mutableStateOf<Assembly?>(null) }
    var showReview by remember { mutableStateOf(false) }
    var editor by remember { mutableStateOf<EditorState?>(null) }

    // Full-screen template editor takes over when open.
    editor?.let { ed ->
        TemplateEditorScreen(
            initialName = ed.name,
            initialDescription = ed.description,
            initialItems = ed.items,
            onSave = { name, desc, items ->
                viewModel.saveTemplate(ed.assemblyId, name, desc, items) {}
                editor = null
            },
            onClose = { editor = null },
        )
        return
    }

    detail?.let { assembly ->
        TemplateDetailSheet(
            assembly = assembly,
            tier = tier,
            requiredTier = viewModel.requiredTier(assembly),
            isMine = viewModel.isMine(assembly),
            inWalk = viewModel.inWalk(assembly.id),
            onStart = { viewModel.seed(assembly); detail = null; onPicked() },
            onAddToWalk = { viewModel.addToWalk(assembly); detail = null },
            onEdit = {
                viewModel.userTemplateFor(assembly.id)?.let { t ->
                    editor = EditorState(assembly.id, t.name, t.description, t.items)
                }
                detail = null
            },
            onDelete = { viewModel.deleteTemplate(assembly.id); detail = null },
            onUpgrade = { detail = null; onOpenSubscription() },
            onDismiss = { detail = null },
        )
    }

    if (showReview) {
        val areas = remember(walk, library) {
            walk.mapNotNull { pick -> library.firstOrNull { it.id == pick.assemblyId }?.let { ReviewArea(it, pick.count) } }
        }
        val merged = remember(walk, library) { viewModel.mergedWalk() }
        WalkReviewSheet(
            areas = areas,
            merged = merged,
            tier = tier,
            requiredTier = viewModel.requiredWalkTier(),
            onSetCount = viewModel::setCount,
            onRemove = viewModel::removeFromWalk,
            onBuild = { viewModel.seedWalk(); showReview = false; onPicked() },
            onUpgrade = { showReview = false; onOpenSubscription() },
            onDismiss = { showReview = false },
        )
    }

    val rows: List<TemplateRow> = remember(q, library) { buildTemplateRows(library, q) }
    val areaCount = walk.sumOf { it.count }

    Scaffold(
        topBar = { BackTopBar(title = "Job Templates", onBack = onBack) },
        bottomBar = {
            // The job walk in progress: areas picked while walking, built into ONE
            // deterministic estimate — no AI in this path.
            if (walk.isNotEmpty()) {
                androidx.compose.material3.Surface(tonalElevation = 3.dp) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                    ) {
                        GradientButton(
                            text = "Review & build ($areaCount ${if (areaCount == 1) "area" else "areas"})",
                            onClick = { showReview = true },
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = viewModel::clearWalk) { Text("Clear") }
                    }
                }
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            FormField(
                value = query,
                onValueChange = { query = it },
                label = "Search templates (bedroom, EV, panel, motor…)",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            )
            OutlinedButton(
                onClick = { editor = EditorState(null, "", "", emptyList()) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                Text("New template — build your own job in a box")
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
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
                            val inWalk = walk.any { it.assemblyId == a.id }
                            ListCard(
                                title = a.label,
                                onClick = { detail = a },
                                subtitle = a.description,
                                footerStart = buildString {
                                    append(if (a.itemCount == 1) "1 line item" else "${a.itemCount} line items")
                                    if (a.sector == AssemblySector.COMMERCIAL_INDUSTRIAL) append(" · Elite")
                                    if (inWalk) append(" · in walk")
                                },
                            )
                        }
                    }
                }
                if (rows.isEmpty()) {
                    item(key = "empty") {
                        Text(
                            "No template matches “$query”. Try a broader term — or tap New template to build it once and it's a one-tap area next time.",
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
 * The "job in a box" detail: every line the template seeds, then the actions —
 * add it to the walk, start a single estimate, or (for your own templates) edit
 * and delete. The gated upgrade moment sits below the value it just showed.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TemplateDetailSheet(
    assembly: Assembly,
    tier: Tier?,
    requiredTier: Tier,
    isMine: Boolean,
    inWalk: Boolean,
    onStart: () -> Unit,
    onAddToWalk: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
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
            Text(assembly.label, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Text(assembly.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Spacer(Modifier.height(10.dp))
            SectionHeader("What it seeds")
            assembly.items.forEach { item ->
                val svc = Catalog.service(item.serviceId)
                if (svc != null) {
                    val variant = svc.variants.getOrNull(item.variantIdx)?.label?.takeIf { svc.variants.size > 1 }
                    LineRow(qty = item.qty, label = svc.label + (variant?.let { " — $it" } ?: ""))
                }
            }
            assembly.customItems.forEach { item -> LineRow(qty = item.qty, label = item.label, hoursEach = item.laborHours) }

            Spacer(Modifier.height(4.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Text(
                if (assembly.sector == AssemblySector.COMMERCIAL_INDUSTRIAL) {
                    "Labor prices at YOUR hourly rate when the estimate opens; material seeds at \$0 for your supplier's quote. Every quantity and hour is editable."
                } else {
                    "Every quantity, variant and price is editable once the estimate opens — a starting point, not a bid."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(10.dp))
            OutlinedButton(onClick = onAddToWalk, modifier = Modifier.fillMaxWidth()) {
                Text(if (inWalk) "Add another to the job walk" else "Add to job walk — quote several areas as one")
            }
            if (isMine) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onEdit, modifier = Modifier.weight(1f)) { Text("Edit") }
                    OutlinedButton(onClick = onDelete, modifier = Modifier.weight(1f)) { Text("Delete") }
                }
            }
            Spacer(Modifier.height(2.dp))
            when (tier?.atLeast(requiredTier)) {
                true -> GradientButton(text = "Start this estimate", onClick = onStart, modifier = Modifier.fillMaxWidth())
                false -> UpgradePrompt(
                    hook = if (requiredTier == Tier.ELITE) "Bid commercial work from a template" else "Quote this job in under a minute",
                    detail = if (requiredTier == Tier.ELITE) {
                        "Commercial & industrial templates are part of Elite — along with the commercial material library and the expanded NEC reference."
                    } else {
                        "Job templates seed a near-complete estimate you just tweak and send. They're part of Pro — unlimited quotes, branded PDFs, get-paid links."
                    },
                    tier = requiredTier,
                    onUpgrade = onUpgrade,
                )
                null -> GradientButton(text = "Start this estimate", onClick = {}, loading = true, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun LineRow(qty: Double, label: String, hoursEach: Double? = null) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(trimQty(qty) + " ×", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
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

/** User templates first, then sector → category grouped built-ins; flat match list when searching. */
private fun buildTemplateRows(all: List<Assembly>, q: String): List<TemplateRow> {
    if (q.isNotEmpty()) {
        return all.filter { it.search.contains(q) }.map(TemplateRow::Item)
    }
    val out = mutableListOf<TemplateRow>()
    val mine = all.filter { UserTemplate.isUserAssemblyId(it.id) }
    if (mine.isNotEmpty()) {
        out += TemplateRow.Header("My templates")
        mine.forEach { out += TemplateRow.Item(it) }
    }
    val builtIns = all.filterNot { UserTemplate.isUserAssemblyId(it.id) }
    AssemblySector.entries.forEach { sector ->
        val inSector = builtIns.filter { it.sector == sector }
        if (inSector.isEmpty()) return@forEach
        inSector.groupBy { it.category }.forEach { (category, assemblies) ->
            out += TemplateRow.Header("${sector.label} · $category")
            assemblies.forEach { out += TemplateRow.Item(it) }
        }
    }
    return out
}

private fun trimQty(v: Double): String = if (v % 1.0 == 0.0) v.toLong().toString() else v.toString()
