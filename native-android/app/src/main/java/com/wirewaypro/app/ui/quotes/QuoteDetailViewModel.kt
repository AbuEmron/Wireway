package com.wirewaypro.app.ui.quotes

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wirewaypro.app.domain.model.QuoteDetail
import com.wirewaypro.app.domain.repository.AuthRepository
import com.wirewaypro.app.domain.repository.ProfileRepository
import com.wirewaypro.app.domain.repository.QuoteRepository
import com.wirewaypro.app.ui.util.QuotePdfGenerator
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

data class QuoteDetailUiState(
    val isLoading: Boolean = true,
    val quote: QuoteDetail? = null,
    val error: String? = null,
    val busy: Boolean = false,
    val deleted: Boolean = false,
    val exportingPdf: Boolean = false,
    val pdfToShare: File? = null, // one-shot: non-null when ready for the share sheet
)

/**
 * Shared by the estimate and invoice detail screens — both render the same
 * `quotes` row; the screen frames it via [QuoteDetail.isInvoice].
 */
@HiltViewModel
class QuoteDetailViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val auth: AuthRepository,
    private val quoteRepository: QuoteRepository,
    private val profileRepository: ProfileRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val quoteId: String = checkNotNull(savedStateHandle[ARG_ID]) { "Missing quote id" }

    private val _state = MutableStateFlow(QuoteDetailUiState())
    val state: StateFlow<QuoteDetailUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        // Only show the full-screen spinner on the first load; a reload (e.g. after
        // an edit) keeps the current content visible and swaps it in silently.
        _state.update { it.copy(isLoading = it.quote == null, error = null) }
        viewModelScope.launch {
            quoteRepository.getQuote(quoteId)
                .onSuccess { quote -> _state.update { it.copy(isLoading = false, quote = quote, error = null) } }
                .onFailure {
                    _state.update { st ->
                        st.copy(isLoading = false, error = if (st.quote == null) "Couldn't load this record." else st.error)
                    }
                }
        }
    }

    fun togglePaid() {
        val userId = auth.currentUserId() ?: return
        val quote = _state.value.quote ?: return
        _state.update { it.copy(busy = true) }
        viewModelScope.launch {
            quoteRepository.setInvoicePaid(userId, quoteId, !quote.invoicePaid)
                .onSuccess { updated -> _state.update { it.copy(busy = false, quote = updated) } }
                .onFailure { _state.update { it.copy(busy = false, error = "Couldn't update payment status.") } }
        }
    }

    fun setDueDate(date: String?) {
        val userId = auth.currentUserId() ?: return
        _state.update { it.copy(busy = true) }
        viewModelScope.launch {
            quoteRepository.setInvoiceDueDate(userId, quoteId, date?.ifBlank { null })
                .onSuccess { updated -> _state.update { it.copy(busy = false, quote = updated) } }
                .onFailure { _state.update { it.copy(busy = false, error = "Couldn't update the due date.") } }
        }
    }

    /** In-person acceptance: typed client signature marks the estimate accepted. */
    fun acceptInPerson(name: String) {
        val userId = auth.currentUserId() ?: return
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        _state.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            quoteRepository.markAccepted(userId, quoteId, trimmed)
                .onSuccess { updated -> _state.update { it.copy(busy = false, quote = updated) } }
                .onFailure { e -> _state.update { it.copy(busy = false, error = e.message ?: "Couldn't record the acceptance.") } }
        }
    }

    fun delete() {
        val userId = auth.currentUserId() ?: return
        viewModelScope.launch {
            quoteRepository.deleteQuote(userId, quoteId)
                .onSuccess { _state.update { it.copy(deleted = true) } }
                .onFailure { _state.update { it.copy(error = "Couldn't delete this record.") } }
        }
    }

    /** Render the quote to a PDF off the main thread, then hand it to the screen. */
    fun exportPdf() {
        val quote = _state.value.quote ?: return
        _state.update { it.copy(exportingPdf = true) }
        viewModelScope.launch {
            // Business header comes from the user's profile (best-effort).
            val business = auth.currentUserId()
                ?.let { profileRepository.getProfile(it).getOrNull() }
                ?.businessInfo()
            // Fetch the business logo for the PDF header (best-effort; PDF still
            // renders without it if the download or decode fails).
            val logo = withContext(Dispatchers.IO) {
                business?.logoUrl?.takeIf { it.isNotBlank() }?.let { url ->
                    runCatching {
                        val bytes = java.net.URL(url).openStream().use { it.readBytes() }
                        android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    }.getOrNull()
                }
            }
            val file = withContext(Dispatchers.IO) { QuotePdfGenerator.generate(appContext, quote, business, logo) }
            if (file == null) {
                _state.update { it.copy(exportingPdf = false, error = "Couldn't build the PDF.") }
            } else {
                _state.update { it.copy(exportingPdf = false, pdfToShare = file) }
            }
        }
    }

    fun pdfConsumed() = _state.update { it.copy(pdfToShare = null) }

    companion object {
        const val ARG_ID = "id"
    }
}
