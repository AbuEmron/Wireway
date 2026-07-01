package com.wirewaypro.app.ui.expenses

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wirewaypro.app.data.ocr.OcrService
import com.wirewaypro.app.domain.model.ExpenseInput
import com.wirewaypro.app.domain.repository.AuthRepository
import com.wirewaypro.app.domain.repository.ExpenseRepository
import com.wirewaypro.app.ui.util.ImageUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import javax.inject.Inject

data class BatchReceiptUiState(
    val isProcessing: Boolean = false,
    val total: Int = 0,
    val done: Int = 0,
    val saved: Int = 0,
    val failed: Int = 0,
    val resultNote: String? = null,
)

/**
 * Saves a batch of receipt photos as draft expenses. Each photo is downscaled,
 * OCR'd, and inserted via [ExpenseRepository.addExpense] with whatever the scan
 * found (amount defaults to 0 when unreadable — the user fixes it later from the
 * list). Best-effort: a single failure doesn't abort the rest.
 */
@HiltViewModel
class BatchReceiptViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val auth: AuthRepository,
    private val expenseRepository: ExpenseRepository,
    private val ocrService: OcrService,
) : ViewModel() {

    private val _state = MutableStateFlow(BatchReceiptUiState())
    val state: StateFlow<BatchReceiptUiState> = _state.asStateFlow()

    fun process(uris: List<Uri>, onComplete: () -> Unit) {
        if (uris.isEmpty()) {
            onComplete()
            return
        }
        val userId = auth.currentUserId()
        if (userId == null) {
            _state.update { it.copy(resultNote = "Session expired — couldn't save receipts.") }
            onComplete()
            return
        }
        _state.update {
            BatchReceiptUiState(isProcessing = true, total = uris.size)
        }
        viewModelScope.launch {
            var saved = 0
            var failed = 0
            for (uri in uris) {
                val ok = saveOne(userId, uri)
                if (ok) saved++ else failed++
                _state.update { it.copy(done = it.done + 1, saved = saved, failed = failed) }
            }
            _state.update {
                it.copy(
                    isProcessing = false,
                    resultNote = buildString {
                        append("Saved $saved receipt")
                        if (saved != 1) append("s")
                        if (failed > 0) append(" · $failed couldn't be saved")
                        append(". Tap each to set its amount.")
                    },
                )
            }
            onComplete()
        }
    }

    private suspend fun saveOne(userId: String, uri: Uri): Boolean {
        val bytes = withContext(Dispatchers.IO) { ImageUtil.downscaleToJpeg(appContext, uri) }
            ?: return false
        val ocr = ocrService.ocr(bytes).getOrNull()
        val input = ExpenseInput(
            expenseDate = ocr?.date ?: LocalDate.now().toString(),
            amount = ocr?.amount ?: 0.0,
            category = ocr?.category ?: "materials",
            vendor = ocr?.vendor,
            description = ocr?.summary,
            jobId = null,
        )
        return expenseRepository.addExpense(userId, input, bytes).isSuccess
    }

    fun clearNote() = _state.update { it.copy(resultNote = null) }
}
