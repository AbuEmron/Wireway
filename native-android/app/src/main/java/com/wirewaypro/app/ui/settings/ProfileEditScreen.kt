package com.wirewaypro.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileEditScreen(
    onClose: () -> Unit,
    viewModel: ProfileEditViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.saved) { if (state.saved) onClose() }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            SaveTopBar(
                title = "Profile & business",
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
            SectionCard(title = "You") {
                FormField(state.fullName, viewModel::setFullName, "Your name")
            }

            SectionCard(title = "Business (shown on proposals & PDFs)") {
                FormField(state.companyName, viewModel::setCompanyName, "Business name")
                Spacer(Modifier.padding(top = 10.dp))
                FormField(state.companyPhone, viewModel::setCompanyPhone, "Phone", keyboardType = KeyboardType.Phone)
                Spacer(Modifier.padding(top = 10.dp))
                FormField(state.companyEmail, viewModel::setCompanyEmail, "Email", keyboardType = KeyboardType.Email)
                Spacer(Modifier.padding(top = 10.dp))
                FormField(state.companyLicense, viewModel::setCompanyLicense, "License #")
                Spacer(Modifier.padding(top = 10.dp))
                FormField(state.companyAddress, viewModel::setCompanyAddress, "Address")
                Spacer(Modifier.padding(top = 10.dp))
                FormField(state.companyWebsite, viewModel::setCompanyWebsite, "Website")
            }

            SectionCard(title = "Baseline rates (your starting point)") {
                FormField(state.hourlyRate, viewModel::setHourlyRate, "Hourly rate $", keyboardType = KeyboardType.Number)
                Spacer(Modifier.padding(top = 10.dp))
                FormField(state.flatRate, viewModel::setFlatRate, "Flat-rate baseline $ (optional)", keyboardType = KeyboardType.Number)
                Spacer(Modifier.padding(top = 8.dp))
                Text(
                    "These prefill new quotes and give the AI a starting point. You can always override per quote.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            SectionCard(title = "Notifications") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Job & payment alerts", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    Spacer(Modifier.width(12.dp))
                    Switch(checked = state.notificationsEnabled, onCheckedChange = viewModel::setNotifications)
                }
            }

            if (state.error != null) {
                Text(state.error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
