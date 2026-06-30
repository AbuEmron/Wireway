package com.wirewaypro.app.ui.money

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wirewaypro.app.domain.model.AgingReport
import com.wirewaypro.app.domain.model.JobPnl
import com.wirewaypro.app.domain.model.JobPnlReport
import com.wirewaypro.app.domain.model.MoneySnapshot
import com.wirewaypro.app.ui.components.BackTopBar
import com.wirewaypro.app.ui.components.GradientButton
import com.wirewaypro.app.ui.components.InfoRow
import com.wirewaypro.app.ui.components.SectionCard
import com.wirewaypro.app.ui.theme.BrandGreen
import com.wirewaypro.app.ui.util.Format
import java.io.File
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoneyScreen(
    onBack: () -> Unit,
    viewModel: MoneyViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val monthLabel = remember(state.snapshot) {
        val ym = state.snapshot?.let { YearMonth.of(it.year, it.month) } ?: YearMonth.now()
        "${ym.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${ym.year}"
    }

    // Hand a freshly-built CSV to the system share sheet, then clear it.
    LaunchedEffect(state.csvExport) {
        val csv = state.csvExport ?: return@LaunchedEffect
        shareCsv(context, "wireway-${viewModel.year}", "Wireway ${viewModel.year} expenses", csv)
        viewModel.csvConsumed()
    }
    LaunchedEffect(state.qbExport) {
        val csv = state.qbExport ?: return@LaunchedEffect
        shareCsv(context, "wireway-quickbooks-${viewModel.year}", "Wireway ${viewModel.year} — QuickBooks import", csv)
        viewModel.qbConsumed()
    }

    Scaffold(
        topBar = {
            BackTopBar(
                title = "Money",
                onBack = onBack,
                actions = {
                    if (state.exporting) {
                        CircularProgressIndicator(Modifier.padding(end = 16.dp).size(22.dp), strokeWidth = 2.dp)
                    } else {
                        IconButton(onClick = viewModel::exportCsv) {
                            Icon(Icons.Outlined.Share, contentDescription = "Export CSV")
                        }
                    }
                },
            )
        },
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = viewModel::refresh,
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
            when {
                state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                state.error != null && state.snapshot == null -> Column(
                    Modifier.fillMaxSize().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(state.error!!, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    TextButton(onClick = viewModel::refresh) { Text("Retry") }
                }
                else -> MoneyContent(state.snapshot, state.aging, state.pnl, monthLabel)
            }
        }
    }
}

@Composable
private fun MoneyContent(
    snap: MoneySnapshot?,
    aging: AgingReport?,
    pnl: JobPnlReport?,
    monthLabel: String,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(monthLabel, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

        if (snap != null) {
            SectionCard(title = "Real profit") {
                Text(
                    Format.money(snap.realProfit),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = if (snap.realProfit >= 0) BrandGreen else MaterialTheme.colorScheme.error,
                )
                Spacer(Modifier.padding(top = 4.dp))
                Text("Collected − spent, this month", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            SectionCard(title = "Money in") {
                InfoRow("Collected", Format.money(snap.collected))
                InfoRow("Quoted (bid)", Format.money(snap.bid))
                InfoRow("Won", Format.money(snap.won))
            }
            SectionCard(title = "Money out") {
                InfoRow("Total spent", Format.money(snap.spent))
                InfoRow("Materials", Format.money(snap.materials))
                InfoRow("Mileage", Format.money(snap.mileage))
                InfoRow("Subcontractors", Format.money(snap.subs))
                InfoRow("Labor", Format.money(snap.labor))
            }
        }

        aging?.let { report ->
            SectionCard(title = "Accounts receivable") {
                InfoRow("Total owed", Format.money(report.buckets.total))
                InfoRow("Current", Format.money(report.buckets.current))
                InfoRow("1–30 days", Format.money(report.buckets.d1_30))
                InfoRow("31–60 days", Format.money(report.buckets.d31_60))
                InfoRow("61–90 days", Format.money(report.buckets.d61_90))
                InfoRow("90+ days", Format.money(report.buckets.d90))
                if (report.items.isEmpty()) {
                    Spacer(Modifier.padding(top = 6.dp))
                    Text("Nothing outstanding.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        pnl?.let { report ->
            if (report.winners.isNotEmpty()) {
                SectionCard(title = "Most profitable jobs") {
                    report.winners.take(5).forEach { JobPnlRow(it) }
                }
            }
            if (report.losers.isNotEmpty()) {
                SectionCard(title = "Jobs losing money") {
                    report.losers.take(5).forEach { JobPnlRow(it) }
                }
            }
        }

        SectionCard(title = "Export for taxes & accounting") {
            Text(
                "One row per money movement for ${viewModel.year}. Accountant CSV is the full ledger; QuickBooks is a bank-import file (Date, Description, Amount).",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.size(12.dp))
            GradientButton(
                text = "Accountant CSV",
                onClick = viewModel::exportCsv,
                enabled = !state.exporting,
                loading = state.exporting,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.size(10.dp))
            GradientButton(
                text = "QuickBooks (CSV)",
                onClick = viewModel::exportQuickBooks,
                enabled = !state.exporting,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun JobPnlRow(job: JobPnl) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(Modifier.padding(end = 12.dp)) {
            Text(job.title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
            Text(
                text = (if (job.settled) "actual · " else "projected · ") + "bid ${Format.money(job.bid)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = Format.money(job.margin),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = if (job.margin >= 0) BrandGreen else MaterialTheme.colorScheme.error,
        )
    }
}

/** Writes the CSV to cache/exports and opens the system share sheet. */
private fun shareCsv(context: Context, fileName: String, subject: String, csv: String) {
    runCatching {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(dir, "$fileName.csv")
        file.writeText(csv)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, subject)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Export").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }
}
