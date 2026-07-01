package com.wirewaypro.app.ui.jobs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wirewaypro.app.domain.model.Job
import com.wirewaypro.app.domain.model.SyncState
import com.wirewaypro.app.ui.components.BackTopBar
import com.wirewaypro.app.ui.components.ListCard
import com.wirewaypro.app.ui.components.RefreshableList
import com.wirewaypro.app.ui.components.SyncBanner
import com.wirewaypro.app.ui.components.SyncStateChip
import com.wirewaypro.app.ui.util.Format

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JobsScreen(
    onBack: () -> Unit,
    onOpenJob: (String) -> Unit,
    onAdd: () -> Unit,
    onOpenCalendar: () -> Unit,
    viewModel: JobsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val banner by viewModel.syncBanner.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Today's dispatch route: this morning's jobs that have an address, in
    // time order, ready to open as one multi-stop Google Maps route.
    val todayStops = remember(state.items) {
        val today = java.time.LocalDate.now().toString()
        state.items
            .filter { it.scheduledDate == today && !it.jobAddress.isNullOrBlank() && it.status != "cancelled" }
            .sortedBy { it.scheduledTime ?: "99:99" }
            .mapNotNull { it.jobAddress }
    }
    com.wirewaypro.app.ui.components.RefreshOnReturn(viewModel::refresh)

    Scaffold(
        topBar = {
            BackTopBar(
                title = "Jobs",
                onBack = onBack,
                actions = {
                    if (todayStops.isNotEmpty()) {
                        IconButton(onClick = { openTodaysRoute(context, todayStops) }) {
                            Icon(Icons.Outlined.Map, contentDescription = "Today's route")
                        }
                    }
                    IconButton(onClick = onOpenCalendar) {
                        Icon(Icons.Outlined.CalendarMonth, contentDescription = "Calendar")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAdd) {
                Icon(Icons.Filled.Add, contentDescription = "New job")
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
                emptyMessage = "No jobs scheduled yet.",
                onRefresh = viewModel::refresh,
                modifier = Modifier.weight(1f),
            ) {
                items(state.items, key = { it.id }) { job ->
                    JobRow(job = job, onClick = { onOpenJob(job.id) })
                }
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
        trailingChip = if (job.syncState != SyncState.SYNCED) {
            { SyncStateChip(job.syncState) }
        } else {
            null
        },
    )
}

/**
 * Opens today's jobs as a single multi-stop driving route in Google Maps
 * (falls back to any maps app that handles the universal dir URL).
 */
private fun openTodaysRoute(context: android.content.Context, addresses: List<String>) {
    runCatching {
        val enc = addresses.map { java.net.URLEncoder.encode(it, "UTF-8") }
        val url = buildString {
            append("https://www.google.com/maps/dir/?api=1&travelmode=driving&destination=")
            append(enc.last())
            if (enc.size > 1) {
                append("&waypoints=")
                append(enc.dropLast(1).joinToString("%7C"))
            }
        }
        context.startActivity(
            android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
                .apply { addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK) },
        )
    }
}
