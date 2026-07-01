package com.wirewaypro.app.ui.clients

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
import com.wirewaypro.app.domain.model.Client
import com.wirewaypro.app.ui.components.BackTopBar
import com.wirewaypro.app.ui.components.ListCard
import com.wirewaypro.app.ui.components.RefreshableList
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
    com.wirewaypro.app.ui.components.RefreshOnReturn(viewModel::refresh)

    Scaffold(
        topBar = { BackTopBar(title = "Clients", onBack = onBack) },
        floatingActionButton = {
            FloatingActionButton(onClick = onAdd) {
                Icon(Icons.Filled.Add, contentDescription = "New client")
            }
        },
    ) { padding ->
        RefreshableList(
            isLoading = state.isLoading,
            isRefreshing = state.isRefreshing,
            error = state.error,
            isEmpty = state.isEmpty,
            emptyMessage = "No clients yet.",
            onRefresh = viewModel::refresh,
            modifier = Modifier.padding(padding),
        ) {
            items(state.items, key = { it.id }) { client ->
                ClientRow(client = client, onClick = { onOpenClient(client.id) })
            }
        }
    }
}

@Composable
private fun ClientRow(client: Client, onClick: () -> Unit) {
    val jobs = client.jobCount ?: 0
    val jobsLabel = if (jobs == 1) "1 job" else "$jobs jobs"

    ListCard(
        title = client.name,
        onClick = onClick,
        trailing = client.totalBilled?.let { Format.money(it) },
        subtitle = listOfNotNull(client.email, client.phone).joinToString("  ·  ").ifBlank { null },
        footerStart = jobsLabel,
    )
}
