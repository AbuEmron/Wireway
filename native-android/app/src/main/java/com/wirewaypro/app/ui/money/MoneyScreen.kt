package com.wirewaypro.app.ui.money

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wirewaypro.app.domain.model.MoneySnapshot
import com.wirewaypro.app.ui.components.BackTopBar
import com.wirewaypro.app.ui.components.InfoRow
import com.wirewaypro.app.ui.components.SectionCard
import com.wirewaypro.app.ui.theme.BrandGreen
import com.wirewaypro.app.ui.util.Format
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
    val monthLabel = remember(state.snapshot) {
        val ym = state.snapshot?.let { YearMonth.of(it.year, it.month) } ?: YearMonth.now()
        "${ym.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${ym.year}"
    }

    Scaffold(topBar = { BackTopBar(title = "Money", onBack = onBack) }) { padding ->
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
                state.snapshot != null -> MoneyContent(state.snapshot!!, monthLabel)
            }
        }
    }
}

@Composable
private fun MoneyContent(snap: MoneySnapshot, monthLabel: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(monthLabel, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

        SectionCard(title = "Real profit") {
            Text(
                Format.money(snap.realProfit),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.SemiBold,
                color = if (snap.realProfit >= 0) BrandGreen else MaterialTheme.colorScheme.error,
            )
            Spacer(Modifier.padding(top = 4.dp))
            Text(
                "Collected − spent, this month",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
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

        Text(
            "Mirrors the web money dashboard. AR aging, per-job P&L, and CSV export arrive in a later phase.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
