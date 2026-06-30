package com.wirewaypro.app.ui.bank

import android.app.Application
import androidx.activity.compose.rememberLauncherForActivityResult
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.plaid.link.FastOpenPlaidLink
import com.plaid.link.Plaid
import com.plaid.link.configuration.LinkTokenConfiguration
import com.plaid.link.result.LinkExit
import com.plaid.link.result.LinkSuccess
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
    val context = LocalContext.current

    // Plaid Link result handler. publicToken is consumed by the server exchange;
    // institution metadata is omitted here to keep the SDK surface minimal.
    val plaidLauncher = rememberLauncherForActivityResult(FastOpenPlaidLink()) { result ->
        when (result) {
            is LinkSuccess -> viewModel.onLinked(result.publicToken, null, null)
            is LinkExit -> {
                // An error here (vs. a plain user cancel) is the REAL reason Plaid
                // Link wouldn't open — e.g. link_token missing android_package_name,
                // or the package isn't allowlisted in the Plaid dashboard.
                val err = result.error
                if (err != null) viewModel.onLinkError(err.toString()) else viewModel.onLinkCancelled()
            }
        }
    }

    // When a link token is ready, open the secure Plaid Link flow.
    LaunchedEffect(state.pendingLinkToken) {
        val token = state.pendingLinkToken ?: return@LaunchedEffect
        val config = LinkTokenConfiguration.Builder().token(token).build()
        val handler = Plaid.create(context.applicationContext as Application, config)
        plaidLauncher.launch(handler)
        viewModel.linkConsumed()
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
                state.status?.let {
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
