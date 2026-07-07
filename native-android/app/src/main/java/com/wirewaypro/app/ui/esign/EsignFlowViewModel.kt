package com.wirewaypro.app.ui.esign

import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wirewaypro.app.data.prefs.SettingsPrefs
import com.wirewaypro.app.domain.model.QuoteDetail
import com.wirewaypro.app.domain.repository.AuthRepository
import com.wirewaypro.app.domain.repository.ProfileRepository
import com.wirewaypro.app.domain.repository.QuoteRepository
import com.wirewaypro.app.esign.EsignManager
import com.wirewaypro.app.esign.EsignRecord
import com.wirewaypro.app.esign.SignatureMethod
import com.wirewaypro.app.esign.Signer
import com.wirewaypro.app.esign.VerificationResult
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

/** The step the signer is on (see WIREWAY_ESIGN_CONSENT_FLOW.md). */
enum class EsignStep { CONSENT, CONFIRM, SIGN, SEALED, PAPER }

data class EsignUiState(
    val isLoading: Boolean = true,
    val quote: QuoteDetail? = null,
    val contractorName: String? = null,
    val step: EsignStep = EsignStep.CONSENT,
    // Consent control — unchecked by default (affirmative act = evidence of intent).
    val consentChecked: Boolean = false,
    val signerName: String = "",
    val signerEmail: String = "",
    val useTyped: Boolean = false,
    val typedName: String = "",
    val busy: Boolean = false,
    val error: String? = null,
    val sealedRecord: EsignRecord? = null,
    val sealedFile: File? = null,
    val verifying: Boolean = false,
    val verifyResult: VerificationResult? = null,
    val shareFile: File? = null, // one-shot for the share sheet
)

/**
 * Drives the in-person signing flow end to end against [EsignManager]. Holds the
 * signing [EsignManager.Session] (minted on entry so consent events reference the
 * record they'll seal into) and sequences consent → confirm → sign → seal → verify.
 */
@HiltViewModel
class EsignFlowViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val auth: AuthRepository,
    private val quoteRepository: QuoteRepository,
    private val profileRepository: ProfileRepository,
    private val settingsPrefs: SettingsPrefs,
    private val manager: EsignManager,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val quoteId: String = checkNotNull(savedStateHandle[ARG_ID]) { "Missing quote id" }

    private val _state = MutableStateFlow(EsignUiState())
    val state: StateFlow<EsignUiState> = _state.asStateFlow()

    private var session: EsignManager.Session? = null

    init { load() }

    private fun load() {
        viewModelScope.launch {
            val userId = auth.currentUserId()
            if (userId == null) {
                _state.update { it.copy(isLoading = false, error = "Session expired. Please sign in again.") }
                return@launch
            }
            quoteRepository.getQuote(quoteId)
                .onSuccess { q ->
                    val title = (q.quoteNumber?.let { "Proposal #$it" } ?: "Proposal") +
                        (q.jobName?.takeIf { it.isNotBlank() }?.let { " — $it" } ?: "")
                    // Open the session + record CONSENT_PRESENTED before the screen shows.
                    session = manager.beginSession(userId, quoteId, title)
                    val contractor = runCatching {
                        profileRepository.getProfile(userId).getOrNull()?.businessInfo()?.name
                    }.getOrNull()
                    _state.update {
                        it.copy(
                            isLoading = false,
                            quote = q,
                            contractorName = contractor,
                            signerName = q.clientName.orEmpty(),
                            signerEmail = q.clientEmail.orEmpty(),
                            error = null,
                        )
                    }
                }
                .onFailure {
                    _state.update { it.copy(isLoading = false, error = "Couldn't load this proposal.") }
                }
        }
    }

    fun setConsentChecked(checked: Boolean) = _state.update { it.copy(consentChecked = checked) }
    fun setSignerName(v: String) = _state.update { it.copy(signerName = v) }
    fun setSignerEmail(v: String) = _state.update { it.copy(signerEmail = v) }
    fun setUseTyped(v: Boolean) = _state.update { it.copy(useTyped = v) }
    fun setTypedName(v: String) = _state.update { it.copy(typedName = v) }
    fun clearError() = _state.update { it.copy(error = null) }

    /** "Agree and continue" — records the affirmative consent act, then advances. */
    fun agreeAndContinue() {
        val s = session ?: return
        if (!_state.value.consentChecked) return
        viewModelScope.launch {
            manager.recordConsentGiven(s)
            _state.update { it.copy(step = EsignStep.CONFIRM) }
        }
    }

    /** "I'd rather use paper" — the always-available off-ramp (records the choice). */
    fun declineForPaper() {
        val s = session ?: return
        viewModelScope.launch {
            manager.recordDeclinedForPaper(s)
            _state.update { it.copy(step = EsignStep.PAPER) }
        }
    }

    /** "Sign now" on the final confirmation → the signature pad. */
    fun confirmAndSign() {
        val s = session ?: return
        viewModelScope.launch {
            manager.recordSignatureConfirmed(s)
            _state.update { it.copy(step = EsignStep.SIGN) }
        }
    }

    fun backToReview() = _state.update { it.copy(step = EsignStep.CONFIRM, error = null) }

    /** Seal a DRAWN signature. [bitmap] is the flattened pad capture. */
    fun sealDrawn(bitmap: Bitmap?) {
        if (bitmap == null) {
            _state.update { it.copy(error = "Please draw your signature first.") }
            return
        }
        seal(SignatureMethod.DRAWN, bitmap, typedName = null)
    }

    /** Seal a TYPED signature (fallback). */
    fun sealTyped() {
        val typed = _state.value.typedName.trim()
        if (typed.length < 2) {
            _state.update { it.copy(error = "Please type your full name.") }
            return
        }
        seal(SignatureMethod.TYPED, bitmap = null, typedName = typed)
    }

    private fun seal(method: SignatureMethod, bitmap: Bitmap?, typedName: String?) {
        val s = session ?: return
        val quote = _state.value.quote ?: return
        val name = _state.value.signerName.trim()
        if (name.length < 2) {
            _state.update { it.copy(error = "Please enter the signer's full name.") }
            return
        }
        _state.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            // Brand the signed proposal with the contractor's business + accent (local,
            // offline-safe). Logo is skipped in V1 to avoid a network fetch mid-signing.
            val userId = auth.currentUserId()
            val business = userId?.let { profileRepository.getProfile(it).getOrNull()?.businessInfo() }
            val accent = runCatching {
                settingsPrefs.brandColorHex.first().takeIf { it.isNotBlank() }
                    ?.let { android.graphics.Color.parseColor(it) }
            }.getOrNull()

            val signer = Signer(name = name, email = _state.value.signerEmail.trim().ifBlank { null })
            manager.seal(
                context = appContext,
                session = s,
                quote = quote,
                signer = signer,
                method = method,
                signatureBitmap = bitmap,
                typedName = typedName,
                business = business,
                logo = null,
                accent = accent,
            ).onSuccess { record ->
                _state.update {
                    it.copy(busy = false, step = EsignStep.SEALED, sealedRecord = record, sealedFile = File(record.sealedPdfPath))
                }
            }.onFailure { e ->
                _state.update { it.copy(busy = false, error = e.message ?: "Couldn't seal the signed document.") }
            }
        }
    }

    /** "Verify integrity" on the sealed screen — recompute + compare the hash. */
    fun verify() {
        val record = _state.value.sealedRecord ?: return
        val userId = auth.currentUserId() ?: return
        _state.update { it.copy(verifying = true, verifyResult = null) }
        viewModelScope.launch {
            manager.verify(userId, record)
                .onSuccess { r -> _state.update { it.copy(verifying = false, verifyResult = r) } }
                .onFailure { e -> _state.update { it.copy(verifying = false, error = e.message) } }
        }
    }

    fun shareSealed() = _state.update { it.copy(shareFile = it.sealedFile) }
    fun shareConsumed() = _state.update { it.copy(shareFile = null) }

    companion object {
        const val ARG_ID = "id"
    }
}
