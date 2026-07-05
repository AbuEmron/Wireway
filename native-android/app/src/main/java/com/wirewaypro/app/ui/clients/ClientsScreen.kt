package com.wirewaypro.app.ui.clients

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wirewaypro.app.domain.model.Client
import com.wirewaypro.app.domain.model.SyncState
import com.wirewaypro.app.ui.components.BackTopBar
import com.wirewaypro.app.ui.components.EmptyState
import com.wirewaypro.app.ui.components.ListCard
import com.wirewaypro.app.ui.components.ListCardSkeleton
import com.wirewaypro.app.ui.components.RefreshableList
import com.wirewaypro.app.ui.components.SearchField
import com.wirewaypro.app.ui.components.SyncBanner
import com.wirewaypro.app.ui.components.SyncStateChip
import com.wirewaypro.app.ui.theme.Spacing
import com.wirewaypro.app.ui.util.Format

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientsScreen(
    onBack: () -> Unit,
    onOpenClient: (String) -> Unit,
    onAdd: () -> Unit,
    viewModel: ClientsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val banner by viewModel.syncBanner.collectAsStateWithLifecycle()
    com.wirewaypro.app.ui.components.RefreshOnReturn(viewModel::refresh)

    // Presentation-only name/email/phone search over the already-loaded list —
    // fast enough to filter on every keystroke, works fully offline.
    var query by rememberSaveable { mutableStateOf("") }
    val q = query.trim().lowercase()
    val visibleItems = remember(state.items, q) {
        if (q.isBlank()) {
            state.items
        } else {
            state.items.filter { client ->
                client.name.lowercase().contains(q) ||
                    client.email?.lowercase()?.contains(q) == true ||
                    client.phone?.lowercase()?.contains(q) == true
            }
        }
    }
    val noneAtAll = state.items.isEmpty()

    Scaffold(
        topBar = { BackTopBar(title = "Clients", onBack = onBack) },
        floatingActionButton = {
            FloatingActionButton(onClick = onAdd) {
                Icon(Icons.Filled.Add, contentDescription = "New client")
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
                    placeholder = "Search name, email, phone",
                    modifier = Modifier.padding(
                        horizontal = Spacing.screen,
                        vertical = Spacing.md,
                    ),
                )
            }
            RefreshableList(
                isLoading = state.isLoading,
                isRefreshing = state.isRefreshing,
                error = state.error,
                isEmpty = noneAtAll,
                emptyMessage = "No clients yet.",
                onRefresh = viewModel::refresh,
                modifier = Modifier.weight(1f),
                skeleton = { ListCardSkeleton() },
                emptyContent = {
                    EmptyState(
                        icon = Icons.Outlined.Groups,
                        title = "No clients yet",
                        message = "Add your first client — every estimate, invoice, and job hangs off their card.",
                        actionLabel = "New client",
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
                                text = "No clients match “$query”.",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                } else {
                    items(visibleItems, key = { it.id }) { client ->
                        ClientRow(
                            client = client,
                            onClick = { onOpenClient(client.id) },
                            modifier = Modifier.animateItem(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ClientRow(client: Client, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val jobs = client.jobCount ?: 0
    val jobsLabel = if (jobs == 1) "1 job" else "$jobs jobs"

    ListCard(
        title = client.name,
        onClick = onClick,
        modifier = modifier,
        trailing = client.totalBilled?.let { Format.money(it) },
        subtitle = listOfNotNull(client.email, client.phone).joinToString("  ·  ").ifBlank { null },
        footerStart = jobsLabel,
        trailingChip = if (client.syncState != SyncState.SYNCED) {
            { SyncStateChip(client.syncState) }
        } else {
            null
        },
    )
}
