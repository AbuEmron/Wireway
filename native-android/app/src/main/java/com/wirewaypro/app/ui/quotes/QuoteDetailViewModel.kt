package com.wirewaypro.app.ui.quotes

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wirewaypro.app.domain.model.QuoteDetail
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
)

/**
 * Shared by the estimate and invoice detail screens — both render the same
 * `quotes` row; the screen just frames it differently based on [QuoteDetail.isInvoice].
 */
@HiltViewModel
class QuoteDetailViewModel @Inject constructor(
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
                .onSuccess { quote -> _state.update { QuoteDetailUiState(isLoading = false, quote = quote, error = null) } }
                .onFailure { _state.update { it.copy(isLoading = false, error = "Couldn't load this record.") } }
        }
    }

    companion object {
        const val ARG_ID = "id"
    }
}
