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
import com.wirewaypro.app.ui.components.AnimatedBarRow
import com.wirewaypro.app.ui.components.AnimatedDonutChart
import com.wirewaypro.app.ui.components.BackTopBar
import com.wirewaypro.app.ui.components.DonutLegendRow
import com.wirewaypro.app.ui.components.DonutSlice
import com.wirewaypro.app.ui.components.GradientButton
import com.wirewaypro.app.ui.components.SectionCard
import com.wirewaypro.app.ui.components.ShimmerBox
import com.wirewaypro.app.ui.theme.BrandAmber
import com.wirewaypro.app.ui.theme.BrandGreen
import com.wirewaypro.app.ui.theme.extended
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
    LaunchedEffect(state.taxExport) {
        val csv = state.taxExport ?: return@LaunchedEffect
        shareCsv(context, "wireway-tax-pnl-${viewModel.year}", "Wireway ${viewModel.year} — tax-ready P&L", csv)
        viewModel.taxConsumed()
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
                state.isLoading -> MoneySkeleton()
                state.error != null && state.snapshot == null -> Column(
                    Modifier.fillMaxSize().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(state.error!!, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    TextButton(onClick = viewModel::refresh) { Text("Retry") }
                }
                else -> MoneyContent(
                    snap = state.snapshot,
                    aging = state.aging,
                    pnl = state.pnl,
                    monthLabel = monthLabel,
                    year = viewModel.year,
                    exporting = state.exporting,
                    onExportCsv = viewModel::exportCsv,
                    onExportQuickBooks = viewModel::exportQuickBooks,
                    onExportTaxSummary = viewModel::exportTaxSummary,
                )
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
    year: Int,
    exporting: Boolean,
    onExportCsv: () -> Unit,
    onExportQuickBooks: () -> Unit,
    onExportTaxSummary: () -> Unit,
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
                com.wirewaypro.app.ui.components.AnimatedMoneyText(
                    value = snap.realProfit,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = if (snap.realProfit >= 0) BrandGreen else MaterialTheme.colorScheme.error,
                )
                Spacer(Modifier.padding(top = 4.dp))
                Text("Collected − spent, this month", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            // Money in — an animated bar graph of the funnel (quoted → won →
            // collected), the exact figures printed on every row.
            SectionCard(title = "Money in") {
                val inMax = maxOf(snap.collected, snap.bid, snap.won)
                AnimatedBarRow(
                    label = "Collected",
                    value = snap.collected,
                    maxValue = inMax,
                    valueText = Format.money(snap.collected),
                    color = BrandGreen,
                )
                AnimatedBarRow(
                    label = "Won",
                    value = snap.won,
                    maxValue = inMax,
                    valueText = Format.money(snap.won),
                    color = MaterialTheme.colorScheme.primary,
                )
                AnimatedBarRow(
                    label = "Quoted (bid)",
                    value = snap.bid,
                    maxValue = inMax,
                    valueText = Format.money(snap.bid),
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
            // Cost breakdown — the mockup's donut, sliced by real spend
            // categories; falls back to plain rows when nothing was spent.
            SectionCard(title = "Where it went") {
                val slices = listOf(
                    DonutSlice("Materials", snap.materials, MaterialTheme.colorScheme.primary),
                    DonutSlice("Labor", snap.labor, MaterialTheme.colorScheme.secondary),
                    DonutSlice("Subcontractors", snap.subs, BrandAmber),
                    DonutSlice("Mileage", snap.mileage, BrandGreen),
                )
                if (snap.spent > 0.0) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AnimatedDonutChart(
                            slices = slices,
                            size = 132.dp,
                            strokeWidth = 16.dp,
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                com.wirewaypro.app.ui.components.AnimatedMoneyText(
                                    value = snap.spent,
                                    style = MaterialTheme.typography.titleMedium,
                                )
                                Text(
                                    "spent",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        Spacer(Modifier.size(16.dp))
                        Column(Modifier.fillMaxWidth()) {
                            slices.filter { it.value > 0.0 }.forEach { slice ->
                                DonutLegendRow(slice, Format.money(slice.value))
                            }
                        }
                    }
                } else {
                    Text(
                        "No spending recorded this month.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        aging?.let { report ->
            SectionCard(title = "Accounts receivable") {
                com.wirewaypro.app.ui.components.AnimatedMoneyText(
                    value = report.buckets.total,
                    style = MaterialTheme.typography.headlineSmall,
                    color = if (report.buckets.total > 0.0) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    "total owed",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (report.items.isEmpty()) {
                    Spacer(Modifier.padding(top = 6.dp))
                    Text("Nothing outstanding.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    Spacer(Modifier.padding(top = 8.dp))
                    // Aging graph — the older the bucket, the hotter the bar.
                    val ext = MaterialTheme.extended
                    val bMax = with(report.buckets) {
                        maxOf(current, d1_30, d31_60, d61_90, d90)
                    }
                    AnimatedBarRow("Current", report.buckets.current, bMax, Format.money(report.buckets.current), BrandGreen)
                    AnimatedBarRow("1–30 days", report.buckets.d1_30, bMax, Format.money(report.buckets.d1_30), MaterialTheme.colorScheme.primary)
                    AnimatedBarRow("31–60 days", report.buckets.d31_60, bMax, Format.money(report.buckets.d31_60), ext.warning)
                    AnimatedBarRow("61–90 days", report.buckets.d61_90, bMax, Format.money(report.buckets.d61_90), BrandAmber)
                    AnimatedBarRow("90+ days", report.buckets.d90, bMax, Format.money(report.buckets.d90), MaterialTheme.colorScheme.error)
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
                "One row per money movement for $year. Accountant CSV is the full ledger; QuickBooks is a bank-import file (Date, Description, Amount).",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.size(12.dp))
            GradientButton(
                text = "Accountant CSV",
                onClick = onExportCsv,
                enabled = !exporting,
                loading = exporting,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.size(10.dp))
            GradientButton(
                text = "QuickBooks (CSV)",
                onClick = onExportQuickBooks,
                enabled = !exporting,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.size(10.dp))
            GradientButton(
                text = "Tax-ready P&L (CSV)",
                onClick = onExportTaxSummary,
                enabled = !exporting,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.size(8.dp))
            Text(
                "Tax-ready P&L: income and every expense category month by month, with net profit — one sheet for your tax preparer.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Shimmer placeholders shaped like the report cards — "your money is arriving". */
@Composable
private fun MoneySkeleton() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ShimmerBox(width = 140.dp, height = 16.dp)
        repeat(3) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
            ) {
                SectionCard {
                    ShimmerBox(width = 120.dp, height = 22.dp)
                    Spacer(Modifier.padding(top = 10.dp))
                    ShimmerBox(width = 220.dp, height = 12.dp)
                    Spacer(Modifier.padding(top = 8.dp))
                    Box(Modifier.fillMaxWidth()) { ShimmerBox(width = 260.dp, height = 12.dp) }
                }
            }
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
