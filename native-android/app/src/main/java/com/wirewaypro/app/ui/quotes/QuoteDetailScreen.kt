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
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wirewaypro.app.domain.model.QuoteDetail
import com.wirewaypro.app.domain.model.QuoteExpiry
import com.wirewaypro.app.domain.model.QuoteLineItem
import com.wirewaypro.app.domain.model.RateMode
import com.wirewaypro.app.domain.model.Tier
import com.wirewaypro.app.ui.components.ConfirmDialog
import com.wirewaypro.app.ui.components.DetailScaffold
import com.wirewaypro.app.ui.components.FormField
import com.wirewaypro.app.ui.components.InfoRow
import com.wirewaypro.app.ui.components.SectionCard
import com.wirewaypro.app.ui.components.StatusChip
import com.wirewaypro.app.ui.components.UpgradePrompt
import com.wirewaypro.app.ui.components.WirewayDatePickerDialog
import com.wirewaypro.app.ui.util.Format
import java.io.File
import java.time.LocalDate

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
    onOpenInvoice: (String) -> Unit = {},
    onOpenSubscription: () -> Unit = {},
    viewModel: QuoteDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val isInvoice = state.quote?.isInvoice == true
    val kind = if (isInvoice) "Invoice" else "Estimate"
    val context = LocalContext.current

    var confirmDelete by remember { mutableStateOf(false) }
    var editDueDate by remember { mutableStateOf(false) }
    var acceptOpen by remember { mutableStateOf(false) }

    com.wirewaypro.app.ui.components.RefreshOnReturn(viewModel::load)
    LaunchedEffect(state.deleted) { if (state.deleted) onBack() }
    LaunchedEffect(state.pdfToShare) {
        state.pdfToShare?.let { sharePdf(context, it); viewModel.pdfConsumed() }
    }
    LaunchedEffect(state.createdInvoiceId) {
        state.createdInvoiceId?.let { onOpenInvoice(it); viewModel.createdInvoiceConsumed() }
    }
    LaunchedEffect(state.duplicatedId) {
        // Open the fresh copy in the builder so the contractor can tweak it.
        state.duplicatedId?.let { onEdit(it); viewModel.duplicatedConsumed() }
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

            if (!quote.isInvoice) {
                SectionCard(title = "Acceptance") {
                    if (quote.sigName != null) {
                        InfoRow("Accepted by", quote.sigName!!)
                        quote.signedAt?.let { InfoRow("On", Format.date(it.take(10))) }
                        quote.depositDue?.let { dep -> InfoRow("Deposit due now", Format.money(dep)) }
                    } else {
                        Text(
                            "Client ready to go? Have them accept right on your phone \u2014 it stamps their name, the date and the time onto this estimate.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.padding(top = 10.dp))
                        Button(
                            onClick = { acceptOpen = true },
                            enabled = !state.busy,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Client accepts \u2014 sign in person")
                        }
                    }
                }
            }

            // Client financing (Elite): a real Wisetack offer on this estimate,
            // or the Pro→Elite moment. Free isn't shown this — they already get
            // the Pro prompt below; never stack two walls on one screen.
            if (!quote.isInvoice) {
                when {
                    state.tier.atLeast(Tier.ELITE) -> FinancingSection(
                        state = state,
                        onOffer = viewModel::offerFinancing,
                        onWithdraw = viewModel::stopOfferingFinancing,
                        onShare = { url -> shareFinancingLink(context, url) },
                    )
                    state.tier == Tier.PRO -> UpgradePrompt(
                        hook = "Close bigger jobs with monthly payments",
                        detail = "Elite adds client financing: your customer applies in " +
                            "minutes and pays over time — you still get paid in full.",
                        tier = Tier.ELITE,
                        onUpgrade = onOpenSubscription,
                    )
                }
            }

            // The Free→Pro moment (WIREWAY_PRICING_TIERS.md): this is where the
            // contractor sends the document, and Free exports go out watermarked
            // without their logo.
            if (state.tier == Tier.FREE) {
                UpgradePrompt(
                    hook = "Send it clean, with your logo",
                    detail = "Free PDFs carry a Wireway watermark. Pro puts your " +
                        "logo and colors on every ${kind.lowercase()} — no watermark.",
                    tier = Tier.PRO,
                    onUpgrade = onOpenSubscription,
                )
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
                if (!quote.isInvoice) {
                    quote.depositDue?.let { dep ->
                        Spacer(Modifier.padding(top = 6.dp))
                        InfoRow("Deposit to accept (${quote.depositPercent}%)", Format.money(dep))
                    }
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

            // "Same as last job" — copy this record into a fresh editable draft.
            OutlinedButton(
                onClick = viewModel::duplicate,
                enabled = !state.busy,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Outlined.ContentCopy, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                Text("Duplicate ${kind.lowercase()}")
            }

            // Estimate → get-paid: spin up an invoice from this bid (keeps the estimate).
            if (!quote.isInvoice) {
                OutlinedButton(
                    onClick = viewModel::convertToInvoice,
                    enabled = !state.busy,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Outlined.ReceiptLong, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                    Text("Convert to invoice")
                }
            }

            // Follow-up nudge for an open (unaccepted) estimate — a stale bid that
            // never gets a reminder is a lost job.
            if (!quote.isInvoice && quote.sigName == null) {
                OutlinedButton(
                    onClick = { shareFollowUp(context, quote) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Outlined.Send, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                    Text("Follow up with client")
                }
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

    if (acceptOpen) {
        var signedName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { acceptOpen = false },
            title = { Text("Accept this estimate") },
            text = {
                Column {
                    Text(
                        "By signing, " + (state.quote?.clientName ?: "the client") +
                            " agrees to the work and price in this estimate" +
                            (state.quote?.depositDue?.let { ", with a deposit of " + Format.money(it) + " due now." } ?: "."),
                    )
                    Spacer(Modifier.padding(top = 10.dp))
                    FormField(signedName, { signedName = it }, "Client's full name (signature)")
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.acceptInPerson(signedName); acceptOpen = false },
                    enabled = signedName.trim().length >= 2,
                ) { Text("Accept estimate") }
            },
            dismissButton = { TextButton(onClick = { acceptOpen = false }) { Text("Cancel") } },
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

/**
 * Opens the share sheet with a friendly, pre-filled follow-up the contractor can
 * send (text/email) to nudge a client toward accepting an open estimate. Tailors
 * the validity line to whether the estimate is still valid or already lapsed, and
 * includes the public quote link so the client can accept in one tap.
 */
private fun shareFollowUp(context: Context, quote: QuoteDetail) {
    runCatching {
        val hi = quote.clientName?.takeIf { it.isNotBlank() }?.let { "Hi $it, " } ?: "Hi, "
        val num = quote.quoteNumber?.takeIf { it.isNotBlank() }?.let { " #$it" } ?: ""
        val job = quote.jobName?.takeIf { it.isNotBlank() }?.let { " for $it" } ?: ""
        val total = quote.total?.let { " (${Format.money(it)})" } ?: ""

        val validThrough = runCatching { LocalDate.parse(quote.createdAt?.take(10)) }
            .getOrNull()?.plusDays(QuoteExpiry.VALID_DAYS)
        val validity = when {
            validThrough == null -> ""
            validThrough.isBefore(LocalDate.now()) ->
                " It expired on ${Format.date(validThrough.toString())}, but I'd be glad to refresh it for you."
            else -> " It's valid through ${Format.date(validThrough.toString())}."
        }

        val url = "https://www.wireway.cc/quote/${quote.id}"
        val text = "${hi}just following up on your estimate$num$job$total.$validity " +
            "You can review and accept it here:\n$url\n\nHappy to answer any questions — thanks!"
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(
            Intent.createChooser(intent, "Follow up").apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) },
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

private fun shareFinancingLink(context: Context, url: String) {
    runCatching {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "Apply for financing for this project — it takes a few minutes: $url")
        }
        context.startActivity(
            Intent.createChooser(intent, "Share financing link").apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) },
        )
    }
}

/**
 * Elite client financing on an estimate. Three honest states: not connected
 * (setup, never a dead toggle), connected with no offer (the "Offer financing"
 * switch), and a live offer (status + provider-reported "as low as" + share).
 * Nothing here invents a number — amounts and statuses are provider-reported.
 */
@Composable
private fun FinancingSection(
    state: QuoteDetailUiState,
    onOffer: () -> Unit,
    onWithdraw: () -> Unit,
    onShare: (String) -> Unit,
) {
    val setup = state.financingSetup
    val offer = state.financingOffer
    val uriHandler = LocalUriHandler.current
    SectionCard(title = "Client financing") {
        when {
            setup == null -> Text(
                "Checking financing availability…",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            !setup.connected -> {
                Text(
                    "Let customers pay over time while you get paid in full — powered by " +
                        "Wisetack. Your financing account isn't connected yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.padding(top = 10.dp))
                val connectUrl = setup.connectUrl
                if (connectUrl != null) {
                    Button(onClick = { uriHandler.openUri(connectUrl) }, modifier = Modifier.fillMaxWidth()) {
                        Text("Connect Wisetack")
                    }
                } else {
                    Text(
                        "Financing setup for your account is coming online — you'll connect " +
                            "Wisetack right here once it's ready.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            else -> {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Offer financing", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            if (offer != null) "On — the pay-over-time option rides on this proposal."
                            else "Adds a pay-over-time option to this estimate's proposal.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (state.financingBusy) {
                        CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                    } else {
                        Switch(
                            checked = offer != null,
                            onCheckedChange = { on -> if (on) onOffer() else onWithdraw() },
                        )
                    }
                }
                if (offer != null) {
                    Spacer(Modifier.padding(top = 8.dp))
                    InfoRow("Status", offer.status.label)
                    offer.asLowAsMonthly?.let { m ->
                        InfoRow("As low as", "${Format.money(m)}/mo (provider-quoted)")
                    }
                    Spacer(Modifier.padding(top = 8.dp))
                    OutlinedButton(
                        onClick = { onShare(offer.applicationUrl) },
                        enabled = !state.financingBusy,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Share application link")
                    }
                }
                state.financingError?.let { message ->
                    Spacer(Modifier.padding(top = 6.dp))
                    Text(
                        message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}
