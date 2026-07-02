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
import com.wirewaypro.app.domain.model.QuoteExpiry
import com.wirewaypro.app.domain.model.QuoteSummary
import com.wirewaypro.app.domain.model.SyncState
import com.wirewaypro.app.ui.components.ExpiryChip
import com.wirewaypro.app.ui.components.ListCard
import com.wirewaypro.app.ui.components.RefreshableList
import com.wirewaypro.app.ui.components.SyncBanner
import com.wirewaypro.app.ui.components.SyncStateChip
import com.wirewaypro.app.ui.components.TabTopBar
import com.wirewaypro.app.ui.util.Format
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EstimatesScreen(
    onOpenEstimate: (String) -> Unit,
    onAdd: () -> Unit,
    viewModel: EstimatesViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val banner by viewModel.syncBanner.collectAsStateWithLifecycle()
    com.wirewaypro.app.ui.components.RefreshOnReturn(viewModel::refresh)

    Scaffold(
        topBar = { TabTopBar("Estimates") },
        floatingActionButton = {
            FloatingActionButton(onClick = onAdd) {
                Icon(Icons.Filled.Add, contentDescription = "New estimate")
            }
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            SyncBanner(
                isOffline = banner.isOffline,
                pendingCount = banner.pendingCount,
                failedCount = banner.failedCount,
                onRetry = viewModel::retrySync,
            )
            RefreshableList(
                isLoading = state.isLoading,
                isRefreshing = state.isRefreshing,
                error = state.error,
                isEmpty = state.isEmpty,
                emptyMessage = "No estimates yet.",
                onRefresh = viewModel::refresh,
                modifier = Modifier.weight(1f),
            ) {
                items(state.items, key = { it.id }) { quote ->
                    QuoteRow(quote = quote, onClick = { onOpenEstimate(quote.id) })
                }
            }
        }
    }
}

@Composable
internal fun QuoteRow(quote: QuoteSummary, onClick: () -> Unit) {
    val number = quote.quoteNumber?.let { "#$it" }
    val title = quote.jobName?.takeIf { it.isNotBlank() }
        ?: quote.clientName?.takeIf { it.isNotBlank() }
        ?: number
        ?: "Untitled"
    val subtitle = listOfNotNull(quote.clientName, number)
        .distinct()
        .joinToString("  ·  ")
        .ifBlank { null }

    val expiry = QuoteExpiry.of(quote, LocalDate.now())

    ListCard(
        title = title,
        onClick = onClick,
        trailing = Format.money(quote.total),
        subtitle = subtitle,
        footerStart = Format.date(quote.createdAt),
        status = quote.status,
        trailingChip = if (quote.syncState != SyncState.SYNCED) {
            { SyncStateChip(quote.syncState) }
        } else {
            null
        },
        footerBadge = if (expiry != null) {
            { ExpiryChip(expiry) }
        } else {
            null
        },
    )
}
