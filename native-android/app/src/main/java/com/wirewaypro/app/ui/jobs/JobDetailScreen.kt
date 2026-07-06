package com.wirewaypro.app.ui.jobs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import com.wirewaypro.app.domain.model.CrewMember
import com.wirewaypro.app.domain.model.Job
import com.wirewaypro.app.domain.model.JobDraw
import com.wirewaypro.app.domain.model.TimeEntry
import com.wirewaypro.app.domain.model.Tier
import com.wirewaypro.app.ui.components.AnimatedBarRow
import com.wirewaypro.app.ui.components.AnimatedMoneyText
import com.wirewaypro.app.ui.components.ConfirmDialog
import com.wirewaypro.app.ui.components.DateField
import com.wirewaypro.app.ui.components.DetailScaffold
import com.wirewaypro.app.ui.components.FormField
import com.wirewaypro.app.ui.components.InfoRow
import com.wirewaypro.app.ui.components.ProgressRing
import com.wirewaypro.app.ui.components.SectionCard
import com.wirewaypro.app.ui.components.StatusChip
import com.wirewaypro.app.ui.components.StatusSelector
import com.wirewaypro.app.ui.components.UpgradePrompt
import com.wirewaypro.app.ui.theme.BrandAmber
import com.wirewaypro.app.ui.theme.BrandGreen
import com.wirewaypro.app.ui.util.Format

@Composable
fun JobDetailScreen(
    onBack: () -> Unit,
    onEdit: (String) -> Unit,
    onOpenSubscription: () -> Unit = {},
    viewModel: JobDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current
    var confirmDelete by remember { mutableStateOf(false) }

    com.wirewaypro.app.ui.components.RefreshOnReturn(viewModel::load)
    androidx.compose.runtime.LaunchedEffect(state.deleted) { if (state.deleted) onBack() }
    androidx.compose.runtime.LaunchedEffect(state.pdfToShare) {
        state.pdfToShare?.let { shareJobCostPdf(context, it); viewModel.pdfConsumed() }
    }

    DetailScaffold(
        title = state.job?.title ?: "Job",
        onBack = onBack,
        isLoading = state.isLoading,
        error = state.error?.takeIf { state.job == null }, // load errors only; action errors show inline
        onRetry = viewModel::load,
        actions = {
            if (state.job != null) {
                IconButton(onClick = { state.job?.let { onEdit(it.id) } }) {
                    Icon(Icons.Outlined.Edit, contentDescription = "Edit job")
                }
                IconButton(onClick = { confirmDelete = true }) {
                    Icon(Icons.Outlined.Delete, contentDescription = "Delete job")
                }
            }
        },
    ) { padding ->
        val job = state.job ?: return@DetailScaffold
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            state.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }

            HeaderBlock(title = job.title, status = job.status, total = job.total)

            SectionCard(title = "Schedule") {
                InfoRow("Date", Format.date(job.scheduledDate))
                Format.time(job.scheduledTime)?.let { InfoRow("Time", it) }
                job.durationHours?.let { InfoRow("Duration", "${trimNum(it)} hrs") }
                InfoRow("Status", Format.status(job.status))
            }

            if (job.clientName != null || job.clientPhone != null ||
                job.clientEmail != null || job.jobAddress != null
            ) {
                SectionCard(title = "Client") {
                    job.clientName?.let { InfoRow("Name", it) }
                    job.clientPhone?.let { InfoRow("Phone", it) }
                    job.clientEmail?.let { InfoRow("Email", it) }
                    job.jobAddress?.let { InfoRow("Address", it) }
                }
            }

            SectionCard(title = "Progress billing") {
                if (state.draws.isEmpty()) {
                    Text(
                        "No draws yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    state.draws.forEach { draw ->
                        DrawRow(draw = draw, onClick = { viewModel.editDraw(draw) })
                    }
                }
                // Any unpaid draw → let the contractor share the client pay page.
                if (state.draws.any { it.status != "paid" }) {
                    Spacer(Modifier.padding(top = 10.dp))
                    androidx.compose.material3.Button(
                        onClick = { shareDrawPayLink(context, job.id) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Outlined.Payments, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                        Text("Request payment (share pay page)")
                    }
                    Text(
                        "Client pays by card or bank (ACH) — money goes straight to your connected account.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.padding(top = 10.dp))
                OutlinedButton(onClick = viewModel::addDraw, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                    Text("Add draw")
                }
            }

            if (job.status == "complete") {
                SectionCard(title = "After the job") {
                    Text(
                        "Happy client? Ask for a review while the good work is fresh.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.padding(top = 10.dp))
                    OutlinedButton(
                        onClick = { shareReviewRequest(context, job.clientName, state.reviewLink) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Ask for a review")
                    }
                    if (state.reviewLink.isBlank()) {
                        Text(
                            "Tip: add your Google/Yelp review link in Profile & business so the text includes it.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            // Elite: log crew hours against this job — deterministic labor cost
            // (hours × the crew member's cost rate) that feeds the card below.
            if (state.tier.atLeast(Tier.ELITE)) {
                CrewLaborSection(
                    crew = state.crew,
                    entries = state.timeEntries,
                    onLogHours = viewModel::openCrewLog,
                    onClockIn = viewModel::clockInCrew,
                    onClockOut = viewModel::clockOutEntry,
                    onDeleteEntry = viewModel::deleteTimeEntry,
                )
            }

            state.profitability?.let { p ->
                SectionCard(title = "Did I make money?") {
                    if (p.isEmpty) {
                        Text(
                            "Mark draws paid, and tag receipts + time entries to this job \u2014 real profit shows up here.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        // Profit analysis graph: money in vs the two cost lines,
                        // each bar filling on a spring with the exact figure printed.
                        val pMax = maxOf(p.collected, p.materials, p.laborCost)
                        AnimatedBarRow(
                            label = "Collected (paid draws)",
                            value = p.collected,
                            maxValue = pMax,
                            valueText = Format.money(p.collected),
                            color = BrandGreen,
                        )
                        AnimatedBarRow(
                            label = "Materials & receipts",
                            value = p.materials,
                            maxValue = pMax,
                            valueText = "\u2212" + Format.money(p.materials),
                            color = MaterialTheme.colorScheme.secondary,
                        )
                        AnimatedBarRow(
                            label = "Labor (" + trimNum(p.laborHours) + " hrs)",
                            value = p.laborCost,
                            maxValue = pMax,
                            valueText = "\u2212" + Format.money(p.laborCost),
                            color = BrandAmber,
                        )
                        Spacer(Modifier.padding(top = 8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text("Profit", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                                AnimatedMoneyText(
                                    value = p.profit,
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = if (p.profit >= 0) BrandGreen else MaterialTheme.colorScheme.error,
                                )
                                Text(
                                    "Counts only what you've recorded on this job.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            // Margin ring \u2014 profit as a share of collected.
                            p.margin?.let { m ->
                                ProgressRing(
                                    progress = m.toFloat().coerceIn(0f, 1f),
                                    size = 64.dp,
                                    strokeWidth = 7.dp,
                                    tint = if (p.profit >= 0) BrandGreen else MaterialTheme.colorScheme.error,
                                ) {
                                    Text(
                                        "${(m * 100).toInt()}%",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                }
                            }
                        }
                        // Elite: line the bid up against real costs on this job.
                        state.estimateTotal?.let { bid ->
                            val trueCost = p.materials + p.laborCost
                            val bMax = maxOf(bid, trueCost)
                            Spacer(Modifier.padding(top = 10.dp))
                            AnimatedBarRow(
                                label = "You bid",
                                value = bid,
                                maxValue = bMax,
                                valueText = Format.money(bid),
                                color = MaterialTheme.colorScheme.primary,
                            )
                            AnimatedBarRow(
                                label = "True cost so far",
                                value = trueCost,
                                maxValue = bMax,
                                valueText = Format.money(trueCost),
                                color = if (trueCost <= bid) MaterialTheme.colorScheme.secondary
                                else MaterialTheme.colorScheme.error,
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text("Left in the bid", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                                AnimatedMoneyText(
                                    value = bid - trueCost,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (bid - trueCost >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                )
                            }
                        }
                        // Elite deeper layer: estimate vs ACTUAL, split into labor
                        // (hours) and materials (cost), with variance + true profit.
                        state.costing?.let { c ->
                            Spacer(Modifier.padding(top = 12.dp))
                            EstimateVsActualBlock(c)
                            if (!c.isEmpty) {
                                Spacer(Modifier.padding(top = 12.dp))
                                OutlinedButton(
                                    onClick = viewModel::exportJobCostPdf,
                                    enabled = !state.exportingPdf,
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Icon(Icons.Outlined.Description, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                                    Text(if (state.exportingPdf) "Building report…" else "Export job cost report (PDF)")
                                }
                            }
                        }
                    }
                }
            }

            // The Pro→Elite moment (WIREWAY_PRICING_TIERS.md): hours are being
            // logged on this job — Elite lines the bid up against real costs.
            state.profitability?.let { p ->
                if (!state.tier.atLeast(Tier.ELITE) && p.laborHours > 0.0) {
                    UpgradePrompt(
                        hook = "See true job cost vs estimate",
                        detail = "Hours are adding up on this job. Elite compares what you bid " +
                            "against real materials and labor as they land, so you know where " +
                            "the money went before the next bid.",
                        tier = Tier.ELITE,
                        onUpgrade = onOpenSubscription,
                    )
                }
            }

            job.notes?.takeIf { it.isNotBlank() }?.let { notes ->
                SectionCard(title = "Notes") {
                    Text(notes, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }

    if (confirmDelete) {
        ConfirmDialog(
            title = "Delete job?",
            message = "This permanently deletes the job and its draws.",
            onConfirm = { confirmDelete = false; viewModel.deleteJob() },
            onDismiss = { confirmDelete = false },
        )
    }

    state.editingDraw?.let { draft ->
        DrawEditorDialog(
            draft = draft,
            onChange = viewModel::updateDrawDraft,
            onSave = viewModel::saveDraw,
            onDelete = draft.id?.let { id -> { viewModel.deleteDraw(id); viewModel.closeDrawEditor() } },
            onDismiss = viewModel::closeDrawEditor,
        )
    }

    state.crewLog?.let { draft ->
        CrewLogDialog(
            draft = draft,
            crew = state.crew,
            onChange = viewModel::updateCrewLog,
            onSave = viewModel::saveCrewLog,
            onDismiss = viewModel::closeCrewLog,
        )
    }
}

/**
 * Elite: log crew hours against this job. Running timers can be stopped; hours can
 * be entered manually. Each entry's labor cost = hours × the crew member's cost
 * rate, flowing straight into the profit + job-costing cards below.
 */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun CrewLaborSection(
    crew: List<CrewMember>,
    entries: List<TimeEntry>,
    onLogHours: () -> Unit,
    onClockIn: (String) -> Unit,
    onClockOut: (TimeEntry) -> Unit,
    onDeleteEntry: (String) -> Unit,
) {
    SectionCard(title = "Crew & labor") {
        if (crew.isEmpty()) {
            Text(
                "Add crew first (Time tracking → Manage crew), then log their hours here to " +
                    "turn labor into real job cost.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return@SectionCard
        }

        val running = entries.filter { it.isRunning }
        val completed = entries.filter { !it.isRunning }

        // On the clock now (multiple crew can be clocked in at once).
        running.forEach { entry ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(entry.workerName ?: "Crew", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                    Text(
                        "On the clock · ${Format.money(entry.rate)}/hr",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                OutlinedButton(onClick = { onClockOut(entry) }) { Text("Stop") }
            }
        }

        // Clock a crew member in — one tap per person.
        Text(
            "Clock in",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = if (running.isEmpty()) 0.dp else 8.dp, bottom = 4.dp),
        )
        androidx.compose.foundation.layout.FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            val runningIds = running.mapNotNull { it.crewMemberId }.toSet()
            crew.filter { it.id !in runningIds }.forEach { member ->
                androidx.compose.material3.AssistChip(
                    onClick = { onClockIn(member.id) },
                    label = { Text(member.name) },
                    leadingIcon = {
                        Icon(Icons.Outlined.Schedule, contentDescription = null, modifier = Modifier.padding(0.dp))
                    },
                )
            }
        }

        Spacer(Modifier.padding(top = 10.dp))
        OutlinedButton(onClick = onLogHours, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
            Text("Log hours manually")
        }

        // Logged so far on this job.
        if (completed.isNotEmpty()) {
            Spacer(Modifier.padding(top = 8.dp))
            completed.forEach { entry ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(entry.workerName ?: "Crew", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        val sub = buildString {
                            append(trimNum(entry.hours ?: 0.0)).append(" hrs")
                            append(" · ").append(Format.money(entry.rate)).append("/hr")
                            entry.notes?.takeIf { it.isNotBlank() }?.let { append(" · ").append(it) }
                        }
                        Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(
                        Format.money(entry.laborCost),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    IconButton(onClick = { onDeleteEntry(entry.id) }) {
                        Icon(Icons.Outlined.Delete, contentDescription = "Delete entry", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun CrewLogDialog(
    draft: CrewLogDraft,
    crew: List<CrewMember>,
    onChange: ((CrewLogDraft) -> CrewLogDraft) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    val selected = crew.firstOrNull { it.id == draft.crewMemberId }
    val hoursValue = draft.hours.trim().toDoubleOrNull()
    val cost = (hoursValue ?: 0.0) * (selected?.hourlyCostRate ?: 0.0)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log crew hours") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Who worked?", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                crew.forEach { member ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onChange { it.copy(crewMemberId = member.id) } }
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        androidx.compose.material3.RadioButton(
                            selected = member.id == draft.crewMemberId,
                            onClick = { onChange { it.copy(crewMemberId = member.id) } },
                        )
                        Text(
                            "${member.name} · ${Format.money(member.hourlyCostRate)}/hr",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
                FormField(draft.hours, { v -> onChange { it.copy(hours = v) } }, "Hours", keyboardType = KeyboardType.Decimal)
                FormField(draft.notes, { v -> onChange { it.copy(notes = v) } }, "Notes (optional)")
                if (hoursValue != null && hoursValue > 0 && selected != null) {
                    Text(
                        "Labor cost: ${Format.money(cost)}  (${trimNum(hoursValue)} hrs × ${Format.money(selected.hourlyCostRate)})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onSave,
                enabled = selected != null && hoursValue != null && hoursValue > 0,
            ) { Text("Log hours") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

/**
 * Elite true job costing: estimate vs ACTUAL, split into labor (compared in hours,
 * because the estimate's labor dollars are a bill rate, not a cost) and materials
 * (compared in cost dollars), each with an honest variance. True profit already
 * shows above as the Pro card's profit (collected − actual costs).
 */
@Composable
private fun EstimateVsActualBlock(c: com.wirewaypro.app.domain.model.JobCosting) {
    Text(
        "Estimate vs actual",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
    )
    if (!c.hasEstimate) {
        Spacer(Modifier.padding(top = 4.dp))
        Text(
            "Link this job to its estimate (from the quote) to compare estimated labor " +
                "and materials against what actually landed.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }

    // Materials — cost vs cost.
    Spacer(Modifier.padding(top = 8.dp))
    val mMax = maxOf(c.estimatedMaterialCost, c.actualMaterialCost, 1.0)
    Text("Materials", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    AnimatedBarRow(
        label = "Estimated",
        value = c.estimatedMaterialCost,
        maxValue = mMax,
        valueText = Format.money(c.estimatedMaterialCost),
        color = MaterialTheme.colorScheme.secondary,
    )
    AnimatedBarRow(
        label = "Actual",
        value = c.actualMaterialCost,
        maxValue = mMax,
        valueText = Format.money(c.actualMaterialCost),
        color = if (c.materialVariance <= 0.0) BrandGreen else MaterialTheme.colorScheme.error,
    )
    VarianceRow(
        label = "Material variance",
        text = signedMoney(c.materialVariance),
        over = c.materialVariance > 0.0,
    )

    // Labor — hours vs hours (with actual cost shown alongside).
    Spacer(Modifier.padding(top = 10.dp))
    val hMax = maxOf(c.estimatedLaborHours, c.actualLaborHours, 1.0)
    Text("Labor (hours)", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    AnimatedBarRow(
        label = "Estimated",
        value = c.estimatedLaborHours,
        maxValue = hMax,
        valueText = "${trimNum(c.estimatedLaborHours)} hrs",
        color = MaterialTheme.colorScheme.secondary,
    )
    AnimatedBarRow(
        label = "Actual",
        value = c.actualLaborHours,
        maxValue = hMax,
        valueText = "${trimNum(c.actualLaborHours)} hrs",
        color = if (c.laborHoursVariance <= 0.0) BrandGreen else MaterialTheme.colorScheme.error,
    )
    VarianceRow(
        label = "Hours variance",
        text = signedHours(c.laborHoursVariance),
        over = c.laborHoursVariance > 0.0,
    )
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text("Actual labor cost", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            Format.money(c.actualLaborCost),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun VarianceRow(label: String, text: String, over: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            text + if (over) "  over" else "  under",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = if (over) MaterialTheme.colorScheme.error else BrandGreen,
        )
    }
}

/** "+$120" / "−$80" (variance = actual − estimate). */
private fun signedMoney(v: Double): String =
    (if (v >= 0) "+" else "−") + Format.money(kotlin.math.abs(v))

/** "+6 hrs" / "−2 hrs". */
private fun signedHours(v: Double): String =
    (if (v >= 0) "+" else "−") + trimNum(kotlin.math.abs(v)) + " hrs"

@Composable
private fun HeaderBlock(title: String, status: String?, total: Double?) {
    Column {
        Text(title, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onBackground)
        Spacer(Modifier.padding(top = 6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            StatusChip(status = status)
            Spacer(Modifier.padding(start = 12.dp))
            AnimatedMoneyText(
                value = total ?: 0.0,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun DrawRow(draw: JobDraw, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(draw.label, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
            val sub = buildString {
                draw.dueDate?.let { append("Due ${Format.date(it)}") }
                if (draw.retainagePct > 0.0) {
                    if (isNotEmpty()) append("  ·  ")
                    append("${trimNum(draw.retainagePct)}% retainage · net ${Format.money(draw.net)}")
                }
            }
            if (sub.isNotBlank()) {
                Text(sub, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                Format.money(draw.amount),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            StatusChip(status = draw.status)
        }
    }
}

@Composable
private fun DrawEditorDialog(
    draft: DrawDraft,
    onChange: ((DrawDraft) -> DrawDraft) -> Unit,
    onSave: () -> Unit,
    onDelete: (() -> Unit)?,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (draft.id == null) "New draw" else "Edit draw") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                FormField(draft.label, { v -> onChange { it.copy(label = v) } }, "Label")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FormField(draft.amount, { v -> onChange { it.copy(amount = v) } }, "Amount $", Modifier.weight(1f), KeyboardType.Number)
                    FormField(draft.retainagePct, { v -> onChange { it.copy(retainagePct = v) } }, "Retainage %", Modifier.weight(1f), KeyboardType.Number)
                }
                DateField("Due date", draft.dueDate, { v -> onChange { it.copy(dueDate = v) } })
                StatusSelector(DrawDraft.STATUSES, draft.status, { v -> onChange { it.copy(status = v) } })
            }
        },
        confirmButton = { TextButton(onClick = onSave) { Text("Save") } },
        dismissButton = {
            Row {
                if (onDelete != null) {
                    TextButton(onClick = onDelete) {
                        Text("Delete", color = MaterialTheme.colorScheme.error)
                    }
                }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        },
    )
}

private fun trimNum(value: Double): String =
    if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()

/**
 * Opens the share sheet with the client-facing draw pay page — the web's public
 * /pay/{jobId}, where the client can pay this job's outstanding progress draws by
 * card or bank. Payment requires the contractor's Stripe Connect (Settings → Get paid).
 */
/** Opens the share sheet with a prefilled review-request text (SMS-friendly). */
private fun shareReviewRequest(context: android.content.Context, clientName: String?, reviewLink: String) {
    val first = clientName?.trim()?.split(" ")?.firstOrNull()?.takeIf { it.isNotBlank() }
    val message = buildString {
        append(if (first != null) "Hi $first, " else "Hi, ")
        append("thanks for having us out! If you were happy with the electrical work, ")
        append("a quick review would mean a lot to our small business.")
        if (reviewLink.isNotBlank()) append(" $reviewLink")
    }
    val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(android.content.Intent.EXTRA_TEXT, message)
    }
    context.startActivity(android.content.Intent.createChooser(intent, "Ask for a review"))
}

/** Shares the built job-cost report PDF via the system chooser (FileProvider). */
private fun shareJobCostPdf(context: android.content.Context, file: java.io.File) {
    runCatching {
        val uri = androidx.core.content.FileProvider.getUriForFile(
            context, "${context.packageName}.fileprovider", file,
        )
        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(android.content.Intent.EXTRA_STREAM, uri)
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(
            android.content.Intent.createChooser(intent, "Share job cost report")
                .apply { addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK) },
        )
    }
}

private fun shareDrawPayLink(context: android.content.Context, jobId: String) {
    runCatching {
        val url = "https://www.wireway.cc/pay/$jobId"
        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(android.content.Intent.EXTRA_TEXT, "Pay your progress draws securely here:\n$url")
        }
        context.startActivity(
            android.content.Intent.createChooser(intent, "Share pay page")
                .apply { addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK) },
        )
    }
}
