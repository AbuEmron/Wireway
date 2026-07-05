package com.wirewaypro.app.ui.timetracking

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.wirewaypro.app.domain.model.TimeEntry
import com.wirewaypro.app.ui.components.BackTopBar
import com.wirewaypro.app.ui.components.GradientButton
import com.wirewaypro.app.ui.components.SectionCard
import com.wirewaypro.app.ui.util.Format
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeTrackingScreen(
    onBack: () -> Unit,
    onManageCrew: () -> Unit = {},
    viewModel: TimeTrackingViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            BackTopBar(
                title = "Time tracking",
                onBack = onBack,
                actions = {
                    IconButton(onClick = onManageCrew) {
                        Icon(Icons.Outlined.Groups, contentDescription = "Manage crew")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            TimerCard(
                running = state.running,
                busy = state.busy,
                onStart = { viewModel.startTimer() },
                onStop = { viewModel.stopTimer() },
            )

            ManualEntry(busy = state.busy, onAdd = { hours, notes -> viewModel.addManual(hours, notes) {} })

            state.error?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
            }

            if (state.completed.isNotEmpty()) {
                Text(
                    "${Format.hours(state.totalHours)} logged",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                state.completed.forEach { entry ->
                    EntryRow(entry = entry, onDelete = { viewModel.delete(entry.id) })
                }
            }
        }
    }

    if (state.promptMiles) {
        MilesPromptDialog(
            onConfirm = { viewModel.logMilesForTrip(it) },
            onDismiss = { viewModel.dismissMilesPrompt() },
        )
    }
}

@Composable
private fun TimerCard(
    running: TimeEntry?,
    busy: Boolean,
    onStart: () -> Unit,
    onStop: () -> Unit,
) {
    SectionCard {
        if (running == null) {
            Text("Track a job", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(
                "Tap start when you’re on your way, stop when the job’s done. Hours feed your job costing.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(14.dp))
            GradientButton(text = "Start tracking", onClick = onStart, enabled = !busy, loading = busy, modifier = Modifier.fillMaxWidth())
        } else {
            // Live ticking elapsed since clock-in.
            var now by remember { mutableStateOf(Instant.now()) }
            LaunchedEffect(running.id) {
                while (true) {
                    now = Instant.now()
                    delay(1000)
                }
            }
            val start = parseInstant(running.clockIn) ?: now
            val elapsed = Duration.between(start, now).coerceAtLeast(Duration.ZERO)
            Text("ON THE CLOCK", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
            Text(
                text = elapsedLabel(elapsed),
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(14.dp))
            GradientButton(text = "Stop tracking", onClick = onStop, enabled = !busy, loading = busy, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun ManualEntry(busy: Boolean, onAdd: (Double, String?) -> Unit) {
    var hours by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    val hoursValue = hours.toDoubleOrNull()
    SectionCard(title = "Add hours manually") {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
                value = hours, onValueChange = { hours = it },
                label = { Text("Hours") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = notes, onValueChange = { notes = it },
                label = { Text("Notes (optional)") },
                modifier = Modifier.fillMaxWidth(),
            )
            GradientButton(
                text = "Add entry",
                onClick = {
                    onAdd(hoursValue ?: 0.0, notes.trim().ifBlank { null })
                    hours = ""; notes = ""
                },
                enabled = hoursValue != null && hoursValue > 0 && !busy,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun EntryRow(entry: TimeEntry, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(Icons.Outlined.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f)) {
                Text("${Format.hours(entry.hours ?: 0.0)} hours", style = MaterialTheme.typography.titleMedium)
                val sub = listOfNotNull(Format.date(entry.createdAt), entry.notes?.takeIf { it.isNotBlank() })
                    .joinToString(" · ")
                if (sub.isNotBlank()) {
                    Text(sub, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Outlined.Delete, contentDescription = "Delete entry", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun MilesPromptDialog(onConfirm: (Double) -> Unit, onDismiss: () -> Unit) {
    var miles by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log mileage?") },
        text = {
            Column {
                Text(
                    "Drove to this job? Add the miles for your tax deduction.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = miles, onValueChange = { miles = it },
                    label = { Text("Miles driven") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(miles.toDoubleOrNull() ?: 0.0) },
                enabled = (miles.toDoubleOrNull() ?: 0.0) > 0,
            ) { Text("Log miles") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Skip") } },
    )
}

private fun elapsedLabel(d: Duration): String {
    val s = d.seconds
    return "%02d:%02d:%02d".format(s / 3600, (s % 3600) / 60, s % 60)
}

/** Parse a Postgres timestamptz string robustly (handles 'Z', '+00:00', or bare). */
private fun parseInstant(s: String?): Instant? {
    if (s.isNullOrBlank()) return null
    return runCatching { OffsetDateTime.parse(s).toInstant() }.getOrNull()
        ?: runCatching { Instant.parse(s) }.getOrNull()
        ?: runCatching { LocalDateTime.parse(s).toInstant(ZoneOffset.UTC) }.getOrNull()
}
