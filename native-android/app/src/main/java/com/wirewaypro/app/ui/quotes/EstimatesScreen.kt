package com.wirewaypro.app.ui.quotes

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wirewaypro.app.domain.model.QuoteExpiry
import com.wirewaypro.app.domain.model.QuoteSummary
import com.wirewaypro.app.domain.model.SyncState
import com.wirewaypro.app.ui.components.EmptyState
import com.wirewaypro.app.ui.components.ExpiryChip
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
 * The estimate statuses collapse into four field-meaningful buckets. "Open" is the
 * one that matters most — bids out the door with money in flight — so it gets its own
 * tab for a one-tap "what am I waiting to hear back on?".
 */
private enum class EstimateFilter(val label: String) {
    ALL("All"),
    DRAFT("Drafts"),
    OPEN("Open"),
    WON("Won");

    fun matches(status: String?): Boolean {
        val key = status?.lowercase()?.trim()
        return when (this) {
            ALL -> true
            DRAFT -> key == null || key == "draft" || key.isBlank()
            OPEN -> key == "sent"
            WON -> key == "accepted" || key == "paid" || key == "complete" || key == "completed"
        }
    }
}

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

    var filterIndex by rememberSaveable { mutableIntStateOf(0) }
    var query by rememberSaveable { mutableStateOf("") }
    val filters = remember { EstimateFilter.entries }
    val activeFilter = filters[filterIndex.coerceIn(0, filters.lastIndex)]
    val visibleItems = remember(state.items, activeFilter, query) {
        val q = query.trim()
        state.items.filter { quote ->
            activeFilter.matches(quote.status) && (
                q.isBlank() ||
                    quote.jobName?.contains(q, ignoreCase = true) == true ||
                    quote.clientName?.contains(q, ignoreCase = true) == true ||
                    quote.quoteNumber?.contains(q, ignoreCase = true) == true
                )
        }
    }
    // The list is "empty" for state purposes only when there are genuinely no
    // estimates — never merely because the current filter hides them all. That keeps
    // the loading/error plumbing honest and lets us show a filter-specific note below.
    val noneAtAll = state.items.isEmpty()

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
            // Only show the filter once there's something to filter — no point
            // offering tabs over an empty screen.
            if (!noneAtAll) {
                SearchField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = "Search estimates…",
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
                emptyMessage = "No estimates yet.",
                onRefresh = viewModel::refresh,
                modifier = Modifier.weight(1f),
                skeleton = { ListCardSkeleton() },
                emptyContent = {
                    EmptyState(
                        icon = Icons.Outlined.Description,
                        title = "No estimates yet",
                        message = "Build your first estimate in under five minutes — priced, professional, and ready to send.",
                        actionLabel = "New estimate",
                        onAction = onAdd,
                    )
                },
            ) {
                if (visibleItems.isEmpty()) {
                    // Data exists, but this filter hides it all — a calm inline note,
                    // not the full empty state (there ARE estimates, just not here).
                    item {
                        FilterEmptyNote(activeFilter)
                    }
                } else {
                    items(visibleItems, key = { it.id }) { quote ->
                        QuoteRow(
                            quote = quote,
                            onClick = { onOpenEstimate(quote.id) },
                            modifier = Modifier.animateItem(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterEmptyNote(filter: EstimateFilter) {
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.xxl, vertical = Spacing.xxxl),
        contentAlignment = androidx.compose.ui.Alignment.Center,
    ) {
        androidx.compose.material3.Text(
            text = "Nothing under “${filter.label}” right now.",
            style = androidx.compose.material3.MaterialTheme.typography.bodyLarge,
            color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}

@Composable
internal fun QuoteRow(quote: QuoteSummary, onClick: () -> Unit, modifier: Modifier = Modifier) {
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
        modifier = modifier,
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
