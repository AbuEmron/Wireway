package com.wirewaypro.app.ui.esign

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.wirewaypro.app.domain.repository.AuthRepository
import com.wirewaypro.app.esign.EsignManager
import com.wirewaypro.app.esign.EsignRecord
import com.wirewaypro.app.esign.SignatureMethod
import com.wirewaypro.app.esign.VerificationResult
import com.wirewaypro.app.esign.data.EsignRepository
import com.wirewaypro.app.ui.components.GradientButton
import com.wirewaypro.app.ui.components.InfoRow
import com.wirewaypro.app.ui.components.SectionCard
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class EsignStatusUiState(
    val record: EsignRecord? = null,
    val verifying: Boolean = false,
    val verifyResult: VerificationResult? = null,
    val shareFile: File? = null,
)

/**
 * Read-only signed-status for a quote, shown on the quote detail. Live-observes the
 * latest sealed record (Room-backed) so it updates the moment a signing flow seals a
 * document and pops back. Offers "Verify integrity" (recompute + compare the hash)
 * and re-share of the sealed PDF.
 */
@HiltViewModel
class EsignStatusViewModel @Inject constructor(
    private val repository: EsignRepository,
    private val manager: EsignManager,
    private val auth: AuthRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val quoteId: String = checkNotNull(savedStateHandle[ARG_ID]) { "Missing quote id" }

    private val _state = MutableStateFlow(EsignStatusUiState())
    val state: StateFlow<EsignStatusUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeForQuote(quoteId).collect { records ->
                _state.update { it.copy(record = records.firstOrNull()) }
            }
        }
    }

    fun verify() {
        val record = _state.value.record ?: return
        val userId = auth.currentUserId() ?: return
        _state.update { it.copy(verifying = true, verifyResult = null) }
        viewModelScope.launch {
            manager.verify(userId, record)
                .onSuccess { r -> _state.update { it.copy(verifying = false, verifyResult = r) } }
                .onFailure { _state.update { it.copy(verifying = false) } }
        }
    }

    fun share() = _state.update { it.copy(shareFile = it.record?.let { r -> File(r.sealedPdfPath) }) }
    fun shareConsumed() = _state.update { it.copy(shareFile = null) }

    companion object {
        const val ARG_ID = "id"
    }
}

/**
 * The e-signature section on a quote detail. Estimates only (the caller gates this).
 * Shows "Sign this proposal" when unsigned, or the signed status + verify/share when
 * a sealed record exists.
 */
@Composable
fun EsignStatusCard(
    onSign: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EsignStatusViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(state.shareFile) {
        state.shareFile?.let { shareEsignPdf(context, it); viewModel.shareConsumed() }
    }

    SectionCard(title = "Electronic signature") {
        val record = state.record
        if (record == null) {
            Text(
                "Have your client sign this proposal right on your phone — a legally binding " +
                    "electronic signature (US ESIGN/UETA), sealed with a tamper-evident fingerprint.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            GradientButton(text = "Sign this proposal", onClick = onSign, modifier = Modifier.fillMaxWidth())
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text("Signed", style = MaterialTheme.typography.titleSmall)
            }
            Spacer(Modifier.height(8.dp))
            InfoRow("Signer", record.signer.name)
            InfoRow("Signed", fmtDate(record.signedAtMillis))
            InfoRow("Method", if (record.method == SignatureMethod.TYPED) "Typed" else "Drawn on device")
            InfoRow("Fingerprint", record.contentSha256.take(16) + "…")
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = viewModel::verify, enabled = !state.verifying, modifier = Modifier.weight(1f)) {
                    Text(if (state.verifying) "Verifying…" else "Verify integrity")
                }
                OutlinedButton(onClick = viewModel::share, modifier = Modifier.weight(1f)) {
                    Text("Share PDF")
                }
            }
            state.verifyResult?.let { r ->
                Spacer(Modifier.height(8.dp))
                Text(
                    if (r.intact) "✓ Verified — intact and unchanged since signing."
                    else "⚠ This document no longer matches its recorded fingerprint.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (r.intact) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

private fun fmtDate(millis: Long): String = runCatching {
    java.time.Instant.ofEpochMilli(millis)
        .atZone(java.time.ZoneId.systemDefault())
        .format(java.time.format.DateTimeFormatter.ofPattern("MMM d, yyyy 'at' h:mm a"))
}.getOrDefault("")

private fun shareEsignPdf(context: Context, file: File) {
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
