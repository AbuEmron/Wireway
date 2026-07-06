package com.wirewaypro.app.ui.esign

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wirewaypro.app.esign.SignatureMethod
import com.wirewaypro.app.esign.consent.ConsentDisclosures
import com.wirewaypro.app.ui.components.DetailScaffold
import com.wirewaypro.app.ui.components.FormField
import com.wirewaypro.app.ui.components.GradientButton
import com.wirewaypro.app.ui.components.InfoRow
import com.wirewaypro.app.ui.components.SectionCard
import com.wirewaypro.app.ui.util.Format
import java.io.File

/**
 * The in-person electronic-signature flow: consent → confirm → sign → sealed.
 * Implements WIREWAY_ESIGN_CONSENT_FLOW.md. Honest language only ("electronic
 * signature", never notarized/certified); legally-operative wording is the
 * versioned copy in [ConsentDisclosures] and carries [COUNSEL] review markers.
 */
@Composable
fun EsignFlowScreen(
    onBack: () -> Unit,
    viewModel: EsignFlowViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(state.shareFile) {
        state.shareFile?.let { shareSignedPdf(context, it); viewModel.shareConsumed() }
    }

    val title = when (state.step) {
        EsignStep.CONSENT -> "Before you sign"
        EsignStep.CONFIRM -> "You're about to sign"
        EsignStep.SIGN -> "Sign"
        EsignStep.SEALED -> "Signed"
        EsignStep.PAPER -> "Paper copy"
    }

    DetailScaffold(
        title = title,
        onBack = onBack,
        isLoading = state.isLoading,
        error = state.error?.takeIf { state.quote == null },
        onRetry = null,
    ) { padding ->
        val quote = state.quote ?: return@DetailScaffold
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            state.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }
            when (state.step) {
                EsignStep.CONSENT -> ConsentStep(state, viewModel)
                EsignStep.CONFIRM -> ConfirmStep(state, viewModel)
                EsignStep.SIGN -> SignStep(state, viewModel)
                EsignStep.SEALED -> SealedStep(state, viewModel, onBack)
                EsignStep.PAPER -> PaperStep(onBack)
            }
        }
    }
}

@Composable
private fun ConsentStep(state: EsignUiState, vm: EsignFlowViewModel) {
    val d = ConsentDisclosures.current
    Text(d.subhead, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

    d.sections.forEach { section ->
        SectionCard(title = section.title) {
            Text(section.body, style = MaterialTheme.typography.bodyMedium)
        }
    }

    // Honest legal-review note — this is disclosure copy, not certified legal advice.
    Text(
        "Legal review pending — this is an electronic signature under the U.S. ESIGN Act / UETA. " +
            "It is not a notarization. Disclosure version ${d.version}.",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    // Consent control — never pre-checked; primary button disabled until checked.
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = state.consentChecked, onCheckedChange = vm::setConsentChecked)
        Spacer(Modifier.width(4.dp))
        Text(d.checkboxLabel, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 2.dp))
    }

    GradientButton(
        text = d.agreeButton,
        onClick = vm::agreeAndContinue,
        enabled = state.consentChecked && !state.busy,
        modifier = Modifier.fillMaxWidth(),
    )
    // "I'd rather use paper" — always visible pre-signature (a real, unburied choice).
    TextButton(onClick = vm::declineForPaper, modifier = Modifier.fillMaxWidth()) {
        Text(d.paperButton)
    }
}

@Composable
private fun ConfirmStep(state: EsignUiState, vm: EsignFlowViewModel) {
    val quote = state.quote!!
    Text(
        "This is the last step. Take a second to confirm.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    SectionCard {
        InfoRow("Document", quote.quoteNumber?.let { "Proposal #$it" } ?: "Proposal")
        quote.jobName?.takeIf { it.isNotBlank() }?.let { InfoRow("Job", it) }
        InfoRow("From", state.contractorName ?: "Your contractor")
        InfoRow("Total", Format.money(quote.total))
        InfoRow("You're signing as", state.signerName.ifBlank { "—" })
        state.signerEmail.takeIf { it.isNotBlank() }?.let { InfoRow("Email", it) }
    }
    Text(
        "By tapping “Sign now,” you're placing your legal signature on this document.",
        style = MaterialTheme.typography.bodyMedium,
    )
    GradientButton(
        text = "Sign now",
        onClick = vm::confirmAndSign,
        enabled = !state.busy,
        modifier = Modifier.fillMaxWidth(),
    )
    OutlinedButton(onClick = vm::backToReview, modifier = Modifier.fillMaxWidth()) {
        Text("Go back and review")
    }
}

@Composable
private fun SignStep(state: EsignUiState, vm: EsignFlowViewModel) {
    val padState = rememberSignaturePadState()

    SectionCard(title = "Who's signing") {
        FormField(state.signerName, vm::setSignerName, "Full name")
        Spacer(Modifier.height(10.dp))
        FormField(state.signerEmail, vm::setSignerEmail, "Email (for their copy)")
    }

    SectionCard(title = if (state.useTyped) "Type your signature" else "Draw your signature") {
        if (state.useTyped) {
            FormField(state.typedName, vm::setTypedName, "Type your full name")
        } else {
            SignaturePad(state = padState)
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = { padState.clear() }) { Text("Clear") }
        }
        Spacer(Modifier.height(4.dp))
        TextButton(onClick = { vm.setUseTyped(!state.useTyped) }) {
            Text(if (state.useTyped) "Draw it instead" else "Type it instead")
        }
    }

    GradientButton(
        text = "Sign",
        onClick = { if (state.useTyped) vm.sealTyped() else vm.sealDrawn(padState.toBitmap()) },
        enabled = !state.busy,
        loading = state.busy,
        modifier = Modifier.fillMaxWidth(),
    )
    OutlinedButton(onClick = vm::backToReview, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) {
        Text("Go back")
    }
}

@Composable
private fun SealedStep(state: EsignUiState, vm: EsignFlowViewModel, onBack: () -> Unit) {
    val record = state.sealedRecord
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            Icons.Outlined.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.width(8.dp))
        Text("Signed and sealed", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    }
    Text(
        "The signature is flattened into the proposal and a Completion Certificate is appended. " +
            "The document is sealed with a secure fingerprint so any later change is detectable.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    if (record != null) {
        SectionCard {
            InfoRow("Signer", record.signer.name)
            InfoRow("Method", if (record.method == SignatureMethod.TYPED) "Typed" else "Drawn on device")
            InfoRow("Fingerprint", record.contentSha256.take(16) + "…")
        }
    }

    GradientButton(
        text = "Share signed PDF",
        onClick = vm::shareSealed,
        modifier = Modifier.fillMaxWidth(),
    )
    OutlinedButton(
        onClick = vm::verify,
        enabled = !state.verifying,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Icon(Icons.Outlined.Lock, contentDescription = null)
        Spacer(Modifier.width(6.dp))
        Text(if (state.verifying) "Verifying…" else "Verify integrity")
    }
    state.verifyResult?.let { r ->
        Text(
            if (r.intact) "✓ Verified — the signed document is intact and unchanged."
            else "⚠ This document no longer matches its recorded fingerprint.",
            style = MaterialTheme.typography.bodyMedium,
            color = if (r.intact) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
        )
    }
    TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Done") }
}

@Composable
private fun PaperStep(onBack: () -> Unit) {
    Text("No problem — you can sign on paper.", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    Text(
        "Ask your contractor for a printed copy of this proposal to review, keep, and sign by hand. " +
            "You can always come back and sign electronically instead.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Start,
    )
    OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Back") }
}

/** Share the sealed PDF via the existing FileProvider flow. */
private fun shareSignedPdf(context: Context, file: File) {
    runCatching {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(
            Intent.createChooser(intent, "Share signed PDF").apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) },
        )
    }
}
