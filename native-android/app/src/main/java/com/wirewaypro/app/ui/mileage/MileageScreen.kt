package com.wirewaypro.app.ui.mileage

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
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import com.wirewaypro.app.domain.model.Mileage
import com.wirewaypro.app.domain.model.Trip
import com.wirewaypro.app.domain.model.TripInput
import com.wirewaypro.app.ui.components.BackTopBar
import com.wirewaypro.app.ui.components.GradientButton
import com.wirewaypro.app.ui.components.SectionCard
import com.wirewaypro.app.ui.util.Format
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MileageScreen(
    onBack: () -> Unit,
    viewModel: MileageViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showForm by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { BackTopBar(title = "Mileage", onBack = onBack) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SectionCard(title = "${viewModel.year} mileage") {
                Text(
                    text = "${Format.miles(state.totalMiles)} mi",
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "${Format.money(state.deduction)} tax deduction · IRS \$${Mileage.IRS_RATE_PER_MILE}/mi",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (showForm) {
                TripForm(
                    saving = state.saving,
                    onSave = { input -> viewModel.addTrip(input) { showForm = false } },
                    onCancel = { showForm = false },
                )
            } else {
                GradientButton(
                    text = "Log a trip",
                    onClick = { showForm = true },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            state.error?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
            }

            if (state.isEmpty && !state.isLoading) {
                Text(
                    "No trips logged yet. Tap “Log a trip” to track miles for your taxes.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            state.trips.forEach { trip ->
                TripRow(trip = trip, onDelete = { viewModel.deleteTrip(trip.id) })
            }
        }
    }
}

@Composable
private fun TripForm(
    saving: Boolean,
    onSave: (TripInput) -> Unit,
    onCancel: () -> Unit,
) {
    var date by remember { mutableStateOf(LocalDate.now().toString()) }
    var miles by remember { mutableStateOf("") }
    var purpose by remember { mutableStateOf("") }
    var from by remember { mutableStateOf("") }
    var to by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    val milesValue = miles.toDoubleOrNull()
    val canSave = milesValue != null && milesValue > 0 && purpose.isNotBlank() && !saving

    SectionCard(title = "Log a trip") {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
                value = date, onValueChange = { date = it },
                label = { Text("Date (YYYY-MM-DD)") }, singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = miles, onValueChange = { miles = it },
                label = { Text("Miles driven") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = purpose, onValueChange = { purpose = it },
                label = { Text("Purpose (e.g. job site visit)") }, singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = from, onValueChange = { from = it },
                label = { Text("From (optional)") }, singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = to, onValueChange = { to = it },
                label = { Text("To (optional)") }, singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = notes, onValueChange = { notes = it },
                label = { Text("Notes (optional)") },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(2.dp))
            GradientButton(
                text = "Save trip",
                onClick = {
                    onSave(
                        TripInput(
                            tripDate = date.ifBlank { LocalDate.now().toString() },
                            miles = milesValue ?: 0.0,
                            purpose = purpose.trim(),
                            startLoc = from.trim().ifBlank { null },
                            endLoc = to.trim().ifBlank { null },
                            notes = notes.trim().ifBlank { null },
                            jobId = null,
                        )
                    )
                },
                enabled = canSave,
                loading = saving,
                modifier = Modifier.fillMaxWidth(),
            )
            androidx.compose.material3.TextButton(
                onClick = onCancel,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Cancel") }
        }
    }
}

@Composable
private fun TripRow(trip: Trip, onDelete: () -> Unit) {
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
            Icon(Icons.Outlined.DirectionsCar, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f)) {
                Text(
                    "${Format.miles(trip.miles)} mi · ${trip.purpose ?: "Trip"}",
                    style = MaterialTheme.typography.titleMedium,
                )
                val sub = listOfNotNull(
                    Format.date(trip.tripDate),
                    listOfNotNull(trip.startLoc, trip.endLoc).takeIf { it.isNotEmpty() }?.joinToString(" → "),
                    if (trip.isBilled) "Billed" else null,
                ).joinToString(" · ")
                Text(
                    sub,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Outlined.Delete, contentDescription = "Delete trip", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
