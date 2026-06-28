package com.wirewaypro.app.ui.jobs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wirewaypro.app.ui.components.FormField
import com.wirewaypro.app.ui.components.SaveTopBar
import com.wirewaypro.app.ui.components.SectionCard
import com.wirewaypro.app.ui.components.StatusSelector

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JobEditScreen(
    onClose: () -> Unit,
    viewModel: JobEditViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.saved) { if (state.saved) onClose() }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            SaveTopBar(
                title = if (state.isEdit) "Edit job" else "New job",
                onBack = onClose,
                onSave = viewModel::save,
                saveEnabled = !state.isSaving,
                saving = state.isSaving,
            )
        },
    ) { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SectionCard(title = "Job") {
                FormField(state.title, viewModel::setTitle, "Title", isError = state.error != null && state.title.isBlank())
                Spacer(Modifier.padding(top = 10.dp))
                FormField(state.total, viewModel::setTotal, "Bid total $", keyboardType = KeyboardType.Number)
                Spacer(Modifier.padding(top = 12.dp))
                Text("Status", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.padding(top = 6.dp))
                StatusSelector(JobEditUiState.STATUSES, state.status, viewModel::setStatus)
            }

            SectionCard(title = "Schedule") {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FormField(state.scheduledDate, viewModel::setScheduledDate, "Date (YYYY-MM-DD)", Modifier.weight(1f))
                    FormField(state.scheduledTime, viewModel::setScheduledTime, "Time (HH:MM)", Modifier.weight(1f))
                }
                Spacer(Modifier.padding(top = 10.dp))
                FormField(state.durationHours, viewModel::setDurationHours, "Duration (hours)", keyboardType = KeyboardType.Number)
            }

            SectionCard(title = "Client") {
                FormField(state.clientName, viewModel::setClientName, "Client name")
                Spacer(Modifier.padding(top = 10.dp))
                FormField(state.clientPhone, viewModel::setClientPhone, "Phone", keyboardType = KeyboardType.Phone)
                Spacer(Modifier.padding(top = 10.dp))
                FormField(state.clientEmail, viewModel::setClientEmail, "Email", keyboardType = KeyboardType.Email)
                Spacer(Modifier.padding(top = 10.dp))
                FormField(state.jobAddress, viewModel::setJobAddress, "Address")
            }

            SectionCard(title = "Notes") {
                FormField(state.notes, viewModel::setNotes, "Notes", singleLine = false)
            }

            if (state.error != null) {
                Text(state.error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
