package com.wirewaypro.app.ui.jobs

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
import com.wirewaypro.app.domain.model.Job
import com.wirewaypro.app.ui.components.BackTopBar
import com.wirewaypro.app.ui.components.ListCard
import com.wirewaypro.app.ui.components.RefreshableList
import com.wirewaypro.app.ui.util.Format

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JobsScreen(
    onBack: () -> Unit,
    onOpenJob: (String) -> Unit,
    onAdd: () -> Unit,
    viewModel: JobsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    com.wirewaypro.app.ui.components.RefreshOnReturn(viewModel::refresh)

    Scaffold(
        topBar = { BackTopBar(title = "Jobs", onBack = onBack) },
        floatingActionButton = {
            FloatingActionButton(onClick = onAdd) {
                Icon(Icons.Filled.Add, contentDescription = "New job")
            }
        },
    ) { padding ->
        RefreshableList(
            isLoading = state.isLoading,
            isRefreshing = state.isRefreshing,
            error = state.error,
            isEmpty = state.isEmpty,
            emptyMessage = "No jobs scheduled yet.",
            onRefresh = viewModel::refresh,
            modifier = Modifier.padding(padding),
        ) {
            items(state.items, key = { it.id }) { job ->
                JobRow(job = job, onClick = { onOpenJob(job.id) })
            }
        }
    }
}

@Composable
private fun JobRow(job: Job, onClick: () -> Unit) {
    val date = Format.date(job.scheduledDate)
    val time = Format.time(job.scheduledTime)
    val whenText = if (time != null) "$date · $time" else date

    ListCard(
        title = job.title,
        onClick = onClick,
        trailing = job.total?.let { Format.money(it) },
        subtitle = job.clientName ?: job.jobAddress,
        footerStart = whenText,
        status = job.status,
    )
}
