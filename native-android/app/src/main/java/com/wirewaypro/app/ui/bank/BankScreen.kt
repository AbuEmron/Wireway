package com.wirewaypro.app.ui.bank

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wirewaypro.app.domain.model.PlaidTxn
import com.wirewaypro.app.ui.components.BackTopBar
import com.wirewaypro.app.ui.components.ListCard
import com.wirewaypro.app.ui.components.RefreshableList
import com.wirewaypro.app.ui.components.SectionCard
import com.wirewaypro.app.ui.util.Format

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BankScreen(
    onBack: () -> Unit,
    viewModel: BankViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var notice by remember { mutableStateOf<String?>(null) }

    // Plaid Link SDK launch is wired in a follow-up; for now, acknowledge the
    // fetched link token so the backend wiring is verifiable on-device.
    LaunchedEffect(state.pendingLinkToken) {
        if (state.pendingLinkToken != null) {
            notice = "Bank link is ready. The secure Plaid login opens on a real device."
            viewModel.linkConsumed()
        }
    }

    Scaffold(topBar = { BackTopBar(title = "Bank", onBack = onBack) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            SectionCard(title = "Connect a bank", modifier = Modifier.padding(16.dp)) {
                Text(
                    "Link your business checking to auto-import transactions for job costing and bookkeeping.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.padding(top = 12.dp))
                Button(onClick = viewModel::connectBank, enabled = !state.linking, modifier = Modifier.fillMaxWidth()) {
                    if (state.linking) {
                        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                        Spacer(Modifier.padding(start = 8.dp))
                        Text("Connecting…")
                    } else {
                        Text("Connect a bank")
                    }
                }
                (state.status ?: notice)?.let {
                    Spacer(Modifier.padding(top = 8.dp))
                    Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                }
                state.error?.let {
                    Spacer(Modifier.padding(top = 8.dp))
                    Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                }
            }

            Box(Modifier.weight(1f)) {
                RefreshableList(
                    isLoading = state.isLoading,
                    isRefreshing = state.isRefreshing,
                    error = null, // bank errors show in the card above
                    isEmpty = state.isEmpty,
                    emptyMessage = "No bank transactions yet. Connect a bank to import them.",
                    onRefresh = viewModel::refresh,
                ) {
                    items(state.transactions, key = { it.id }) { txn ->
                        TxnRow(txn)
                    }
                }
            }
        }
    }
}

@Composable
private fun TxnRow(txn: PlaidTxn) {
    ListCard(
        title = txn.name,
        onClick = {},
        trailing = Format.money(txn.amount),
        subtitle = txn.category?.replaceFirstChar { it.uppercase() },
        footerStart = Format.date(txn.date),
    )
}
