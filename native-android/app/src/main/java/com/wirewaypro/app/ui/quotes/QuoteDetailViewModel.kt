package com.wirewaypro.app.ui.quotes

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wirewaypro.app.domain.model.QuoteDetail
import com.wirewaypro.app.domain.repository.AuthRepository
import com.wirewaypro.app.domain.repository.QuoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class QuoteDetailUiState(
    val isLoading: Boolean = true,
    val quote: QuoteDetail? = null,
    val error: String? = null,
    val busy: Boolean = false,
    val deleted: Boolean = false,
)

/**
 * Shared by the estimate and invoice detail screens — both render the same
 * `quotes` row; the screen frames it via [QuoteDetail.isInvoice].
 */
@HiltViewModel
class QuoteDetailViewModel @Inject constructor(
    private val auth: AuthRepository,
    private val quoteRepository: QuoteRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val quoteId: String = checkNotNull(savedStateHandle[ARG_ID]) { "Missing quote id" }

    private val _state = MutableStateFlow(QuoteDetailUiState())
    val state: StateFlow<QuoteDetailUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            quoteRepository.getQuote(quoteId)
                .onSuccess { quote -> _state.update { QuoteDetailUiState(isLoading = false, quote = quote) } }
                .onFailure { _state.update { it.copy(isLoading = false, error = "Couldn't load this record.") } }
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

    fun delete() {
        val userId = auth.currentUserId() ?: return
        viewModelScope.launch {
            quoteRepository.deleteQuote(userId, quoteId)
                .onSuccess { _state.update { it.copy(deleted = true) } }
                .onFailure { _state.update { it.copy(error = "Couldn't delete this record.") } }
        }
    }

    companion object {
        const val ARG_ID = "id"
    }
}
