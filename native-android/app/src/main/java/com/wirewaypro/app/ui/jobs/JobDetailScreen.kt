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
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Payments
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
import com.wirewaypro.app.domain.model.Job
import com.wirewaypro.app.domain.model.JobDraw
import com.wirewaypro.app.ui.components.ConfirmDialog
import com.wirewaypro.app.ui.components.DateField
import com.wirewaypro.app.ui.components.DetailScaffold
import com.wirewaypro.app.ui.components.FormField
import com.wirewaypro.app.ui.components.InfoRow
import com.wirewaypro.app.ui.components.SectionCard
import com.wirewaypro.app.ui.components.StatusChip
import com.wirewaypro.app.ui.components.StatusSelector
import com.wirewaypro.app.ui.util.Format

@Composable
fun JobDetailScreen(
    onBack: () -> Unit,
    onEdit: (String) -> Unit,
    viewModel: JobDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current
    var confirmDelete by remember { mutableStateOf(false) }

    com.wirewaypro.app.ui.components.RefreshOnReturn(viewModel::load)
    androidx.compose.runtime.LaunchedEffect(state.deleted) { if (state.deleted) onBack() }

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
                }
                Spacer(Modifier.padding(top = 10.dp))
                OutlinedButton(onClick = viewModel::addDraw, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                    Text("Add draw")
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
}

@Composable
private fun HeaderBlock(title: String, status: String?, total: Double?) {
    Column {
        Text(title, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onBackground)
        Spacer(Modifier.padding(top = 6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            StatusChip(status = status)
            Spacer(Modifier.padding(start = 12.dp))
            Text(
                Format.money(total),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
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
