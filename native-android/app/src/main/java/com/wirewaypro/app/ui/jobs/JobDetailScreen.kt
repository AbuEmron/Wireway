package com.wirewaypro.app.ui.jobs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wirewaypro.app.domain.model.Job
import com.wirewaypro.app.domain.model.JobDraw
import com.wirewaypro.app.ui.components.DetailScaffold
import com.wirewaypro.app.ui.components.InfoRow
import com.wirewaypro.app.ui.components.SectionCard
import com.wirewaypro.app.ui.components.StatusChip
import com.wirewaypro.app.ui.util.Format

@Composable
fun JobDetailScreen(
    onBack: () -> Unit,
    viewModel: JobDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    DetailScaffold(
        title = state.job?.title ?: "Job",
        onBack = onBack,
        isLoading = state.isLoading,
        error = state.error,
        onRetry = viewModel::load,
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

            if (state.draws.isNotEmpty()) {
                SectionCard(title = "Progress billing") {
                    state.draws.forEachIndexed { index, draw ->
                        DrawRow(draw)
                        if (index < state.draws.lastIndex) Spacer(Modifier.padding(top = 4.dp))
                    }
                }
            }

            job.notes?.takeIf { it.isNotBlank() }?.let { notes ->
                SectionCard(title = "Notes") {
                    Text(
                        text = notes,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}

@Composable
private fun HeaderBlock(title: String, status: String?, total: Double?) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.padding(top = 6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            StatusChip(status = status)
            Spacer(Modifier.padding(start = 12.dp))
            Text(
                text = Format.money(total),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun DrawRow(draw: JobDraw) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = draw.label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            val sub = buildString {
                draw.dueDate?.let { append("Due ${Format.date(it)}") }
                if (draw.retainagePct > 0.0) {
                    if (isNotEmpty()) append("  ·  ")
                    append("${trimNum(draw.retainagePct)}% retainage")
                }
            }
            if (sub.isNotBlank()) {
                Text(
                    text = sub,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = Format.money(draw.amount),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            StatusChip(status = draw.status)
        }
    }
}

/** Drops a trailing ".0" so 2.0 -> "2" but 2.5 stays "2.5". */
private fun trimNum(value: Double): String =
    if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()
