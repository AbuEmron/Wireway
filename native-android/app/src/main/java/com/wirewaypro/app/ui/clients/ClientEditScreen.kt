package com.wirewaypro.app.ui.clients

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wirewaypro.app.domain.model.FreeLimits
import com.wirewaypro.app.domain.model.Tier
import com.wirewaypro.app.ui.components.ConfirmDialog
import com.wirewaypro.app.ui.components.FormField
import com.wirewaypro.app.ui.components.SaveTopBar
import com.wirewaypro.app.ui.components.SectionCard
import com.wirewaypro.app.ui.components.UpgradePrompt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientEditScreen(
    onClose: () -> Unit,
    onOpenSubscription: () -> Unit = {},
    viewModel: ClientEditViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var confirmDelete by remember { mutableStateOf(false) }

    LaunchedEffect(state.saved) { if (state.saved) onClose() }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            SaveTopBar(
                title = if (state.isEdit) "Edit client" else "New client",
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
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SectionCard(title = "Client") {
                FormField(state.name, viewModel::setName, "Name", isError = state.error != null && state.name.isBlank())
                Spacer(Modifier.padding(top = 10.dp))
                FormField(state.email, viewModel::setEmail, "Email", keyboardType = KeyboardType.Email)
                Spacer(Modifier.padding(top = 10.dp))
                FormField(state.phone, viewModel::setPhone, "Phone", keyboardType = KeyboardType.Phone)
            }

            if (state.isEdit) {
                OutlinedButton(
                    onClick = { confirmDelete = true },
                    enabled = !state.isSaving,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Delete client", color = MaterialTheme.colorScheme.error)
                }
            }

            if (state.error != null) {
                Text(state.error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }

            // Free hit the saved-client ceiling on Save — the Pro moment.
            if (state.clientCapReached) {
                UpgradePrompt(
                    hook = "Your client list is growing",
                    detail = "Free includes ${FreeLimits.MAX_CLIENTS} saved clients and you've used them all. " +
                        "Pro is unlimited — clients, quotes, invoices.",
                    tier = Tier.PRO,
                    onUpgrade = onOpenSubscription,
                )
            }
        }
    }

    if (confirmDelete) {
        ConfirmDialog(
            title = "Delete client?",
            message = "This permanently deletes the client.",
            onConfirm = { confirmDelete = false; viewModel.delete() },
            onDismiss = { confirmDelete = false },
        )
    }
}
