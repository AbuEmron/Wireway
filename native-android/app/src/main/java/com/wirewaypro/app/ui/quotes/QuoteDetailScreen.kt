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
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wirewaypro.app.domain.model.QuoteDetail
import com.wirewaypro.app.domain.model.QuoteLineItem
import com.wirewaypro.app.domain.model.RateMode
import com.wirewaypro.app.ui.components.ConfirmDialog
import com.wirewaypro.app.ui.components.DetailScaffold
import com.wirewaypro.app.ui.components.InfoRow
import com.wirewaypro.app.ui.components.SectionCard
import com.wirewaypro.app.ui.components.StatusChip
import com.wirewaypro.app.ui.components.WirewayDatePickerDialog
import com.wirewaypro.app.ui.util.Format
import java.io.File

/**
 * Read + light-write detail for a `quotes` row. Used for BOTH estimates and
 * invoices; [QuoteDetail.isInvoice] changes the framing and, for invoices, shows
 * paid/due controls. Editing the body opens the quote builder.
 */
@Composable
fun QuoteDetailScreen(
    onBack: () -> Unit,
    onEdit: (String) -> Unit,
    onPullList: (String) -> Unit = {},
    viewModel: QuoteDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val isInvoice = state.quote?.isInvoice == true
    val kind = if (isInvoice) "Invoice" else "Estimate"
    val context = LocalContext.current

    var confirmDelete by remember { mutableStateOf(false) }
    var editDueDate by remember { mutableStateOf(false) }

    com.wirewaypro.app.ui.components.RefreshOnReturn(viewModel::load)
    LaunchedEffect(state.deleted) { if (state.deleted) onBack() }
    LaunchedEffect(state.pdfToShare) {
        state.pdfToShare?.let { sharePdf(context, it); viewModel.pdfConsumed() }
    }

    DetailScaffold(
        title = state.quote?.quoteNumber?.let { "$kind #$it" } ?: kind,
        onBack = onBack,
        isLoading = state.isLoading,
        error = state.error?.takeIf { state.quote == null }, // load errors only; action errors show inline
        onRetry = viewModel::load,
        actions = {
            if (state.quote != null) {
                if (state.exportingPdf) {
                    CircularProgressIndicator(Modifier.padding(end = 12.dp).size(22.dp), strokeWidth = 2.dp)
                } else {
                    IconButton(onClick = viewModel::exportPdf) {
                        Icon(Icons.Outlined.PictureAsPdf, contentDescription = "Export PDF")
                    }
                }
                IconButton(onClick = { state.quote?.let { onEdit(it.id) } }) {
                    Icon(Icons.Outlined.Edit, contentDescription = "Edit")
                }
                IconButton(onClick = { confirmDelete = true }) {
                    Icon(Icons.Outlined.Delete, contentDescription = "Delete")
                }
            }
        },
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
            state.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }

            Header(quote = quote, kind = kind)

            if (quote.isInvoice) {
                SectionCard(title = "Invoice") {
                    InfoRow("Status", if (quote.invoicePaid) "Paid" else "Unpaid")
                    InfoRow("Due", quote.invoiceDueDate?.let { Format.date(it) } ?: "—")
                    Spacer(Modifier.padding(top = 10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = viewModel::togglePaid,
                            enabled = !state.busy,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(if (quote.invoicePaid) "Mark unpaid" else "Mark paid")
                        }
                        OutlinedButton(
                            onClick = { editDueDate = true },
                            enabled = !state.busy,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("Due date")
                        }
                    }
                }
            }

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
                if (quote.rateMode == RateMode.HOURLY) {
                    InfoRow("Pricing", if (quote.clientBuysAll) "Hourly · just labor" else "Hourly · labor + materials")
                    MoneyRow("Hourly rate", quote.hourlyRate)
                    quote.totalHours?.let { InfoRow("Estimated time", "${trimNum(it)} hrs") }
                    MoneyRow("Labor", (quote.totalHours ?: 0.0) * (quote.hourlyRate ?: 0.0))
                    if (!quote.clientBuysAll) {
                        if (quote.showMaterials) MoneyRow("Materials", quote.totalMaterial)
                        if (quote.taxEnabled) MoneyRow("Tax", (quote.totalMaterial ?: 0.0) * (quote.taxRate ?: 0.0))
                    }
                } else {
                    InfoRow("Pricing", "Flat rate")
                    if (quote.showMaterials) MoneyRow("Materials", quote.totalMaterial)
                    val laborLabel = quote.totalHours?.let { "Labor (${trimNum(it)} hrs)" } ?: "Labor"
                    MoneyRow(laborLabel, quote.totalLabor)
                    MoneyRow("Markup", quote.totalMarkup)
                    if (quote.taxEnabled) MoneyRow("Tax", quote.totalTax)
                }
                Spacer(Modifier.padding(top = 6.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(Modifier.padding(top = 6.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Total", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        Format.money(quote.total),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                if (quote.rateMode == RateMode.HOURLY) {
                    Spacer(Modifier.padding(top = 6.dp))
                    Text(
                        if (quote.clientBuysAll) "Just labor — the client supplies the materials."
                        else "Labor + materials you supply.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Button(
                onClick = { sharePayLink(context, quote.id, quote.quoteNumber, quote.isInvoice) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Outlined.Payments, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                Text("Request payment (share pay link)")
            }
            Text(
                "Client pays by card or bank (ACH) — money goes straight to your connected account.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            OutlinedButton(
                onClick = { onPullList(quote.id) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Outlined.ShoppingCart, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                Text("Material Pull List")
            }

            quote.notes?.takeIf { it.isNotBlank() }?.let { notes ->
                SectionCard(title = "Notes") {
                    Text(notes, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }

    if (confirmDelete) {
        ConfirmDialog(
            title = "Delete $kind?",
            message = "This permanently deletes this record.",
            onConfirm = { confirmDelete = false; viewModel.delete() },
            onDismiss = { confirmDelete = false },
        )
    }

    if (editDueDate) {
        WirewayDatePickerDialog(
            initial = state.quote?.invoiceDueDate.orEmpty(),
            onConfirm = { editDueDate = false; viewModel.setDueDate(it) },
            onDismiss = { editDueDate = false },
        )
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
    }
}

@Composable
private fun LineItemRow(item: QuoteLineItem) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
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

@Composable
private fun MoneyRow(label: String, value: Double?) {
    if (value == null) return
    InfoRow(label, Format.money(value))
}

private fun trimNum(value: Double): String =
    if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()

/**
 * Opens the share sheet with the client-facing pay link — the web's public quote
 * page (/quote/{id}) where the client can view, accept, and pay by card or bank.
 * Same durable link the web app shares; payment requires the contractor's Stripe
 * Connect setup (Settings → Get paid).
 */
private fun sharePayLink(context: Context, quoteId: String, quoteNumber: String?, isInvoice: Boolean) {
    runCatching {
        val url = "https://www.wireway.cc/quote/$quoteId"
        val label = quoteNumber?.takeIf { it.isNotBlank() }?.let { " $it" } ?: ""
        val kind = if (isInvoice) "invoice" else "quote"
        val text = "Here's your $kind$label — view and pay securely here:\n$url"
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(
            Intent.createChooser(intent, "Share pay link").apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) },
        )
    }
}

/** Opens the system share sheet for a generated PDF file. */
private fun sharePdf(context: Context, file: File) {
    runCatching {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(
            Intent.createChooser(intent, "Share PDF").apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) },
        )
    }
}
