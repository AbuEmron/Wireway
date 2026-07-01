package com.wirewaypro.app.ui.quotes

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wirewaypro.app.domain.model.QuoteSummary
import com.wirewaypro.app.domain.model.SyncState
import com.wirewaypro.app.ui.components.ListCard
import com.wirewaypro.app.ui.components.RefreshableList
import com.wirewaypro.app.ui.components.SyncBanner
import com.wirewaypro.app.ui.components.SyncStateChip
import com.wirewaypro.app.ui.components.TabTopBar
import com.wirewaypro.app.ui.util.Format

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoicesScreen(
    onOpenInvoice: (String) -> Unit,
    onAdd: () -> Unit,
    viewModel: InvoicesViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val banner by viewModel.syncBanner.collectAsStateWithLifecycle()
    com.wirewaypro.app.ui.components.RefreshOnReturn(viewModel::refresh)

    Scaffold(
        topBar = { TabTopBar("Invoices") },
        floatingActionButton = {
            FloatingActionButton(onClick = onAdd) {
                Icon(Icons.Filled.Add, contentDescription = "New invoice")
            }
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            SyncBanner(isOffline = banner.isOffline, pendingCount = banner.pendingCount)
            RefreshableList(
                isLoading = state.isLoading,
                isRefreshing = state.isRefreshing,
                error = state.error,
                isEmpty = state.isEmpty,
                emptyMessage = "No invoices yet.",
                onRefresh = viewModel::refresh,
                modifier = Modifier.weight(1f),
            ) {
                items(state.items, key = { it.id }) { invoice ->
                    InvoiceRow(invoice = invoice, onClick = { onOpenInvoice(invoice.id) })
                }
            }
        }
    }
}

@Composable
private fun InvoiceRow(invoice: QuoteSummary, onClick: () -> Unit) {
    val number = invoice.quoteNumber?.let { "#$it" }
    val title = invoice.clientName?.takeIf { it.isNotBlank() }
        ?: invoice.jobName?.takeIf { it.isNotBlank() }
        ?: number
        ?: "Invoice"
    // Invoices surface payment state: explicit paid flag wins, else the row status.
    val statusLabel = if (invoice.invoicePaid) "paid" else (invoice.status ?: "unpaid")
    val due = invoice.invoiceDueDate?.let { "Due ${Format.date(it)}" }

    ListCard(
        title = title,
        onClick = onClick,
        trailing = Format.money(invoice.total),
        subtitle = listOfNotNull(invoice.jobName?.takeIf { it != title }, number)
            .joinToString("  ·  ").ifBlank { null },
        footerStart = due ?: Format.date(invoice.createdAt),
        status = statusLabel,
        trailingChip = if (invoice.syncState != SyncState.SYNCED) {
            { SyncStateChip(invoice.syncState) }
        } else {
            null
        },
    )
}
