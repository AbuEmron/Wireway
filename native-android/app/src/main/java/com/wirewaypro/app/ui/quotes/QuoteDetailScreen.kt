package com.wirewaypro.app.ui.quotes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wirewaypro.app.domain.model.QuoteDetail
import com.wirewaypro.app.domain.model.QuoteLineItem
import com.wirewaypro.app.ui.components.DetailScaffold
import com.wirewaypro.app.ui.components.InfoRow
import com.wirewaypro.app.ui.components.SectionCard
import com.wirewaypro.app.ui.components.StatusChip
import com.wirewaypro.app.ui.util.Format

/**
 * Read-only detail for a `quotes` row. Used for BOTH estimates and invoices;
 * [QuoteDetail.isInvoice] only changes the framing (title + payment line).
 */
@Composable
fun QuoteDetailScreen(
    onBack: () -> Unit,
    viewModel: QuoteDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val isInvoice = state.quote?.isInvoice == true
    val kind = if (isInvoice) "Invoice" else "Estimate"

    DetailScaffold(
        title = state.quote?.quoteNumber?.let { "$kind #$it" } ?: kind,
        onBack = onBack,
        isLoading = state.isLoading,
        error = state.error,
        onRetry = viewModel::load,
    ) { padding ->
        val quote = state.quote ?: return@DetailScaffold
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Header(quote = quote, kind = kind)

            if (quote.clientName != null || quote.clientEmail != null || quote.clientPhone != null) {
                SectionCard(title = "Client") {
                    quote.clientName?.let { InfoRow("Name", it) }
                    quote.clientEmail?.let { InfoRow("Email", it) }
                    quote.clientPhone?.let { InfoRow("Phone", it) }
                    quote.jobName?.takeIf { it.isNotBlank() }?.let { InfoRow("Job", it) }
                }
            }

            if (quote.lineItems.isNotEmpty()) {
                SectionCard(title = "Line items") {
                    quote.lineItems.forEach { LineItemRow(it) }
                }
            }

            SectionCard(title = "Totals") {
                if (quote.showMaterials) MoneyRow("Materials", quote.totalMaterial)
                val laborLabel = quote.totalHours
                    ?.let { "Labor (${trimNum(it)} hrs)" } ?: "Labor"
                MoneyRow(laborLabel, quote.totalLabor)
                MoneyRow("Markup", quote.totalMarkup)
                if (quote.taxEnabled) MoneyRow("Tax", quote.totalTax)
                Spacer(Modifier.padding(top = 6.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(Modifier.padding(top = 6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "Total",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = Format.money(quote.total),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            quote.notes?.takeIf { it.isNotBlank() }?.let { notes ->
                SectionCard(title = "Notes") {
                    Text(
                        text = notes,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}

@Composable
private fun Header(quote: QuoteDetail, kind: String) {
    Column {
        Text(
            text = quote.jobName?.takeIf { it.isNotBlank() }
                ?: quote.clientName?.takeIf { it.isNotBlank() }
                ?: kind,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.padding(top = 6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            StatusChip(status = if (quote.isInvoice && quote.invoicePaid) "paid" else quote.status)
            Spacer(Modifier.padding(start = 12.dp))
            Text(
                text = Format.money(quote.total),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        if (quote.isInvoice && quote.invoiceDueDate != null) {
            Spacer(Modifier.padding(top = 6.dp))
            Text(
                text = "Due ${Format.date(quote.invoiceDueDate)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun LineItemRow(item: QuoteLineItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.Top,
    ) {
        val prefix = if (item.kind == "mileage") "🚗 " else ""
        val qty = if (item.quantity != 1.0) " × ${trimNum(item.quantity)}" else ""
        Text(
            text = "$prefix${item.label}$qty",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        if (item.amount != null) {
            Spacer(Modifier.padding(start = 12.dp))
            Text(
                text = Format.money(item.amount),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Totals line that hides itself when the value is null. */
@Composable
private fun MoneyRow(label: String, value: Double?) {
    if (value == null) return
    InfoRow(label, Format.money(value))
}

private fun trimNum(value: Double): String =
    if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()
