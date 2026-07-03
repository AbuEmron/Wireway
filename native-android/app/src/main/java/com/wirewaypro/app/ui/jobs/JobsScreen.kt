package com.wirewaypro.app.ui.jobs

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Work
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wirewaypro.app.domain.model.Job
import com.wirewaypro.app.domain.model.SyncState
import com.wirewaypro.app.ui.components.BackTopBar
import com.wirewaypro.app.ui.components.EmptyState
import com.wirewaypro.app.ui.components.ListCard
import com.wirewaypro.app.ui.components.ListCardSkeleton
import com.wirewaypro.app.ui.components.RefreshableList
import com.wirewaypro.app.ui.components.SegmentedTabs
import com.wirewaypro.app.ui.components.SyncBanner
import com.wirewaypro.app.ui.components.SyncStateChip
import com.wirewaypro.app.ui.theme.Spacing
import com.wirewaypro.app.ui.util.Format

/**
 * Job statuses collapse into three field-meaningful buckets plus All. "Active" is
 * the crew-on-site money view; "Upcoming" answers "what's on the schedule?".
 * Cancelled jobs only appear under All — they're history, not work.
 */
private enum class JobFilter(val label: String) {
    ALL("All"),
    UPCOMING("Upcoming"),
    ACTIVE("Active"),
    DONE("Done");

    fun matches(status: String?): Boolean {
        val key = status?.lowercase()?.trim()
        return when (this) {
            ALL -> true
            UPCOMING -> key == null || key.isBlank() || key == "scheduled"
            ACTIVE -> key == "in_progress"
            DONE -> key == "complete" || key == "completed"
        }
    }
}

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

    var filterIndex by rememberSaveable { mutableIntStateOf(0) }
    val filters = remember { JobFilter.entries }
    val activeFilter = filters[filterIndex.coerceIn(0, filters.lastIndex)]
    val visibleItems = remember(state.items, activeFilter) {
        state.items.filter { activeFilter.matches(it.status) }
    }
    // Empty only when there are genuinely no jobs — a filter hiding everything
    // gets a calm inline note instead of the full first-run empty state.
    val noneAtAll = state.items.isEmpty()

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
            if (!noneAtAll) {
                SegmentedTabs(
                    options = filters.map { it.label },
                    selectedIndex = filterIndex,
                    onSelect = { filterIndex = it },
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
                emptyMessage = "No jobs scheduled yet.",
                onRefresh = viewModel::refresh,
                modifier = Modifier.weight(1f),
                skeleton = { ListCardSkeleton() },
                emptyContent = {
                    EmptyState(
                        icon = Icons.Outlined.Work,
                        title = "No jobs on the books",
                        message = "Win an estimate and schedule it here — or add a job directly to start tracking time and materials.",
                        actionLabel = "New job",
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
                    items(visibleItems, key = { it.id }) { job ->
                        JobRow(job = job, onClick = { onOpenJob(job.id) })
                    }
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
