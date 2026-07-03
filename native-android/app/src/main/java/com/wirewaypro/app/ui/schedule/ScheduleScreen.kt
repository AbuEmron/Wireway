package com.wirewaypro.app.ui.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wirewaypro.app.domain.model.Job
import com.wirewaypro.app.ui.components.BackTopBar
import com.wirewaypro.app.ui.components.ListCard
import com.wirewaypro.app.ui.components.RefreshOnReturn
import com.wirewaypro.app.ui.components.SegmentedTabs
import com.wirewaypro.app.ui.jobs.JobsViewModel
import com.wirewaypro.app.ui.theme.Spacing
import com.wirewaypro.app.ui.util.Format
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale
import androidx.compose.ui.unit.dp

/**
 * The scheduling calendar (Pro): week and month views over the contractor's own
 * jobs and appointments. Entries ARE jobs — the same offline-first Room rows the
 * Jobs list syncs — so nothing scheduled here can be lost to a dead zone, and
 * every entry ties to its client/quote through the existing job record.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(
    onBack: () -> Unit,
    onOpenJob: (String) -> Unit,
    onAddForDate: (String) -> Unit,
    viewModel: JobsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    RefreshOnReturn(viewModel::refresh)

    var viewIndex by rememberSaveable { mutableIntStateOf(0) } // 0 = Week, 1 = Month
    var selected by remember { mutableStateOf(LocalDate.now()) }
    var month by remember { mutableStateOf(YearMonth.now()) }
    // Week strips start on Monday — trade parlance ("first thing Monday").
    var weekStart by remember { mutableStateOf(LocalDate.now().with(DayOfWeek.MONDAY)) }

    val byDate = remember(state.items) { state.items.groupBy { it.scheduledDate.orEmpty() } }
    val dayJobs = remember(byDate, selected) {
        byDate[selected.toString()].orEmpty().sortedBy { it.scheduledTime ?: "99:99" }
    }

    Scaffold(
        topBar = { BackTopBar(title = "Schedule", onBack = onBack) },
        floatingActionButton = {
            FloatingActionButton(onClick = { onAddForDate(selected.toString()) }) {
                Icon(Icons.Filled.Add, contentDescription = "Schedule for this day")
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            SegmentedTabs(
                options = listOf("Week", "Month"),
                selectedIndex = viewIndex,
                onSelect = { viewIndex = it },
                modifier = Modifier.padding(horizontal = Spacing.screen, vertical = Spacing.md),
            )

            if (viewIndex == 0) {
                WeekStrip(
                    weekStart = weekStart,
                    selected = selected,
                    countFor = { date -> byDate[date.toString()]?.size ?: 0 },
                    onSelect = { selected = it },
                    onPrev = { weekStart = weekStart.minusWeeks(1); selected = weekStart },
                    onNext = { weekStart = weekStart.plusWeeks(1); selected = weekStart },
                )
            } else {
                MonthHeader(
                    month = month,
                    onPrev = { month = month.minusMonths(1) },
                    onNext = { month = month.plusMonths(1) },
                )
                WeekdayHeader()
                MonthGrid(
                    month = month,
                    selected = selected,
                    today = LocalDate.now(),
                    hasJobs = { date -> byDate[date.toString()]?.isNotEmpty() == true },
                    onSelect = { selected = it },
                )
            }

            Spacer(Modifier.height(10.dp))
            Text(
                text = "${selected.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())}, " +
                    "${selected.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())} ${selected.dayOfMonth}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            Spacer(Modifier.height(8.dp))

            if (dayJobs.isEmpty()) {
                Text(
                    "Nothing scheduled — tap + to book this day.",
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

/**
 * A Monday-first 7-day strip: big (48dp+) day chips with a booked-count badge,
 * prev/next week arrows. One glance = "what does my week look like?".
 */
@Composable
private fun WeekStrip(
    weekStart: LocalDate,
    selected: LocalDate,
    countFor: (LocalDate) -> Int,
    onSelect: (LocalDate) -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
) {
    val weekEnd = weekStart.plusDays(6)
    val title = if (weekStart.month == weekEnd.month) {
        "${weekStart.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())} ${weekStart.dayOfMonth}–${weekEnd.dayOfMonth}"
    } else {
        "${weekStart.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())} ${weekStart.dayOfMonth} – " +
            "${weekEnd.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())} ${weekEnd.dayOfMonth}"
    }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onPrev) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous week")
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onNext) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next week")
            }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
            val today = LocalDate.now()
            for (i in 0..6) {
                val date = weekStart.plusDays(i.toLong())
                val isSelected = date == selected
                val count = countFor(date)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(3.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f) else Color.Transparent,
                        )
                        .then(
                            if (date == today)

                                Modifier.border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(14.dp))
                            else Modifier,
                        )
                        .clickable { onSelect(date) }
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = date.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = date.dayOfMonth.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(2.dp))
                    if (count > 0) {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.secondary),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = count.toString(),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                            )
                        }
                    } else {
                        Spacer(Modifier.size(16.dp))
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
    today: LocalDate,
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
                            isToday = date == today,
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
    isToday: Boolean,
    hasJobs: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(3.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f) else Color.Transparent)
            .then(
                if (isToday) Modifier.border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(10.dp)) else Modifier,
            )
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
                Spacer(Modifier.height(2.dp))
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
