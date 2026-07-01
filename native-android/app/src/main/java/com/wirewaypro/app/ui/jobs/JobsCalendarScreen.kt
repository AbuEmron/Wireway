package com.wirewaypro.app.ui.jobs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wirewaypro.app.domain.model.Job
import com.wirewaypro.app.ui.components.BackTopBar
import com.wirewaypro.app.ui.components.ListCard
import com.wirewaypro.app.ui.components.RefreshOnReturn
import com.wirewaypro.app.ui.util.Format
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

/**
 * Month calendar of scheduled jobs (by jobs.scheduled_date). Days with jobs are
 * dotted; tapping a day shows that day's jobs below, each opening its detail.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JobsCalendarScreen(
    onBack: () -> Unit,
    onOpenJob: (String) -> Unit,
    viewModel: JobsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    RefreshOnReturn(viewModel::refresh)

    var month by remember { mutableStateOf(YearMonth.now()) }
    var selected by remember { mutableStateOf(LocalDate.now()) }

    val byDate = remember(state.items) { state.items.groupBy { it.scheduledDate.orEmpty() } }
    val dayJobs = byDate[selected.toString()].orEmpty()

    Scaffold(topBar = { BackTopBar(title = "Calendar", onBack = onBack) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            MonthHeader(
                month = month,
                onPrev = { month = month.minusMonths(1) },
                onNext = { month = month.plusMonths(1) },
            )
            WeekdayHeader()
            MonthGrid(
                month = month,
                selected = selected,
                hasJobs = { date -> byDate[date.toString()]?.isNotEmpty() == true },
                onSelect = { selected = it },
            )
            Spacer(Modifier.padding(top = 8.dp))
            Text(
                text = "${selected.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())}, " +
                    "${selected.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())} ${selected.dayOfMonth}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            Spacer(Modifier.padding(top = 8.dp))

            if (dayJobs.isEmpty()) {
                Text(
                    "No jobs scheduled this day.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            } else {
                LazyColumn(
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(dayJobs, key = { it.id }) { job ->
                        DayJobRow(job, onClick = { onOpenJob(job.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthHeader(month: YearMonth, onPrev: () -> Unit, onNext: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onPrev) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous month")
        }
        Text(
            text = "${month.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${month.year}",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onNext) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next month")
        }
    }
}

@Composable
private fun WeekdayHeader() {
    Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
        listOf("S", "M", "T", "W", "T", "F", "S").forEach { d ->
            Text(
                text = d,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun MonthGrid(
    month: YearMonth,
    selected: LocalDate,
    hasJobs: (LocalDate) -> Boolean,
    onSelect: (LocalDate) -> Unit,
) {
    val leading = month.atDay(1).dayOfWeek.value % 7 // Sunday-first grid
    val length = month.lengthOfMonth()
    val rows = (leading + length + 6) / 7

    Column(Modifier.padding(horizontal = 8.dp)) {
        for (week in 0 until rows) {
            Row(Modifier.fillMaxWidth()) {
                for (col in 0..6) {
                    val dayNum = week * 7 + col - leading + 1
                    if (dayNum in 1..length) {
                        val date = month.atDay(dayNum)
                        DayCell(
                            day = dayNum,
                            selected = date == selected,
                            hasJobs = hasJobs(date),
                            onClick = { onSelect(date) },
                            modifier = Modifier.weight(1f),
                        )
                    } else {
                        Box(Modifier.weight(1f).aspectRatio(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    day: Int,
    selected: Boolean,
    hasJobs: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(3.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f) else androidx.compose.ui.graphics.Color.Transparent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = day.toString(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            )
            if (hasJobs) {
                Spacer(Modifier.padding(top = 2.dp))
                Box(
                    Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondary),
                )
            }
        }
    }
}

@Composable
private fun DayJobRow(job: Job, onClick: () -> Unit) {
    val time = Format.time(job.scheduledTime)
    ListCard(
        title = job.title,
        onClick = onClick,
        trailing = job.total?.let { Format.money(it) },
        subtitle = job.clientName,
        footerStart = time ?: "All day",
        status = job.status,
    )
}
