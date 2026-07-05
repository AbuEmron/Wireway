package com.wirewaypro.app.ui.quotes

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wirewaypro.app.domain.model.QuoteSummary
import com.wirewaypro.app.domain.model.SyncState
import com.wirewaypro.app.ui.components.EmptyState
import com.wirewaypro.app.ui.components.ListCard
import com.wirewaypro.app.ui.components.ListCardSkeleton
import com.wirewaypro.app.ui.components.RefreshableList
import com.wirewaypro.app.ui.components.SearchField
import com.wirewaypro.app.ui.components.SegmentedTabs
import com.wirewaypro.app.ui.components.SyncBanner
import com.wirewaypro.app.ui.components.SyncStateChip
import com.wirewaypro.app.ui.components.TabTopBar
import com.wirewaypro.app.ui.theme.Spacing
import com.wirewaypro.app.ui.util.Format
import java.time.LocalDate

/**
 * Invoices bucket by payment state — the one question this tab answers is
 * "who owes me money?", so Unpaid and Overdue get their own taps.
 */
private enum class InvoiceFilter(val label: String) {
    ALL("All"),
    UNPAID("Unpaid"),
    OVERDUE("Overdue"),
    PAID("Paid");

    fun matches(invoice: QuoteSummary, today: LocalDate): Boolean {
        val paid = invoice.invoicePaid || invoice.status?.lowercase()?.trim() == "paid"
        return when (this) {
            ALL -> true
            UNPAID -> !paid
            OVERDUE -> !paid && invoice.invoiceDueDate.isBefore(today)
            PAID -> paid
        }
    }

    private fun String?.isBefore(today: LocalDate): Boolean =
        this?.let { runCatching { LocalDate.parse(it.take(10)) }.getOrNull() }?.isBefore(today) == true
}

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

    var filterIndex by rememberSaveable { mutableIntStateOf(0) }
    var query by rememberSaveable { mutableStateOf("") }
    val filters = remember { InvoiceFilter.entries }
    val activeFilter = filters[filterIndex.coerceIn(0, filters.lastIndex)]
    val today = remember { LocalDate.now() }
    val visibleItems = remember(state.items, activeFilter, today, query) {
        val q = query.trim()
        state.items.filter { invoice ->
            activeFilter.matches(invoice, today) && (
                q.isBlank() ||
                    invoice.clientName?.contains(q, ignoreCase = true) == true ||
                    invoice.jobName?.contains(q, ignoreCase = true) == true ||
                    invoice.quoteNumber?.contains(q, ignoreCase = true) == true
                )
        }
    }
    val noneAtAll = state.items.isEmpty()

    Scaffold(
        topBar = { TabTopBar("Invoices") },
        floatingActionButton = {
            FloatingActionButton(onClick = onAdd) {
                Icon(Icons.Filled.Add, contentDescription = "New invoice")
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
            if (!noneAtAll) {
                SearchField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = "Search invoices…",
                    modifier = Modifier.padding(
                        horizontal = Spacing.screen,
                        vertical = Spacing.sm,
                    ),
                )
                SegmentedTabs(
                    options = filters.map { it.label },
                    selectedIndex = filterIndex,
                    onSelect = { filterIndex = it },
                    modifier = Modifier.padding(
                        horizontal = Spacing.screen,
                        vertical = Spacing.sm,
                    ),
                )
            }
            RefreshableList(
                isLoading = state.isLoading,
                isRefreshing = state.isRefreshing,
                error = state.error,
                isEmpty = noneAtAll,
                emptyMessage = "No invoices yet.",
                onRefresh = viewModel::refresh,
                modifier = Modifier.weight(1f),
                skeleton = { ListCardSkeleton() },
                emptyContent = {
                    EmptyState(
                        icon = Icons.Outlined.ReceiptLong,
                        title = "No invoices yet",
                        message = "Turn a won estimate into an invoice — or start one from scratch — and get paid by card or bank.",
                        actionLabel = "New invoice",
                        onAction = onAdd,
                    )
                },
            ) {
                if (visibleItems.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = Spacing.xxl, vertical = Spacing.xxxl),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "Nothing under “${activeFilter.label}” right now.",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                } else {
                    items(visibleItems, key = { it.id }) { invoice ->
                        InvoiceRow(
                            invoice = invoice,
                            onClick = { onOpenInvoice(invoice.id) },
                            modifier = Modifier.animateItem(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InvoiceRow(invoice: QuoteSummary, onClick: () -> Unit, modifier: Modifier = Modifier) {
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
        modifier = modifier,
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
