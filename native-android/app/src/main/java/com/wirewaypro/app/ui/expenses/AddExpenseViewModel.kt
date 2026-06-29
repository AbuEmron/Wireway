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

data class AddExpenseUiState(
    val date: String = "",
    val amount: String = "",
    val category: String = "materials",
    val vendor: String = "",
    val description: String = "",
    val imageBytes: ByteArray? = null,
    val isSaving: Boolean = false,
    val isScanning: Boolean = false,
    val scanNote: String? = null,
    val error: String? = null,
    val saved: Boolean = false,
)

@HiltViewModel
class AddExpenseViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val auth: AuthRepository,
    private val expenseRepository: ExpenseRepository,
    private val ocrService: OcrService,
) : ViewModel() {

    private val _state = MutableStateFlow(AddExpenseUiState(date = LocalDate.now().toString()))
    val state: StateFlow<AddExpenseUiState> = _state.asStateFlow()

    fun setDate(v: String) = _state.update { it.copy(date = v) }
    fun setAmount(v: String) = _state.update { it.copy(amount = v, error = null) }
    fun setCategory(v: String) = _state.update { it.copy(category = v) }
    fun setVendor(v: String) = _state.update { it.copy(vendor = v) }
    fun setDescription(v: String) = _state.update { it.copy(description = v) }
    fun clearImage() = _state.update { it.copy(imageBytes = null) }

    /** Decode + downscale the picked/captured image, then OCR it to prefill fields. */
    fun setImageFromUri(uri: Uri) {
        viewModelScope.launch {
            val bytes = withContext(Dispatchers.IO) { ImageUtil.downscaleToJpeg(appContext, uri) }
            if (bytes == null) {
                _state.update { it.copy(error = "Couldn't read that image.") }
                return@launch
            }
            _state.update { it.copy(imageBytes = bytes, error = null, isScanning = true, scanNote = null) }
            ocrService.ocr(bytes)
                .onSuccess { applyOcr(it) }
                .onFailure { _state.update { it.copy(isScanning = false, scanNote = "Couldn't scan the receipt — enter details manually.") } }
        }
    }

    /** Prefill only the fields the user hasn't already typed. */
    private fun applyOcr(result: com.wirewaypro.app.data.ocr.OcrResult) {
        _state.update { s ->
            s.copy(
                isScanning = false,
                scanNote = "Scanned — please review before saving.",
                amount = if (s.amount.isBlank()) result.amount?.let { a -> if (a % 1.0 == 0.0) a.toLong().toString() else a.toString() } ?: s.amount else s.amount,
                vendor = if (s.vendor.isBlank()) result.vendor ?: s.vendor else s.vendor,
                date = result.date ?: s.date,
                category = result.category ?: s.category,
                description = if (s.description.isBlank()) result.summary ?: s.description else s.description,
            )
        }
    }

    fun save() {
        val userId = auth.currentUserId()
        if (userId == null) {
            _state.update { it.copy(error = "Session expired.") }
            return
        }
        val amount = _state.value.amount.trim().toDoubleOrNull()
        if (amount == null || amount <= 0.0) {
            _state.update { it.copy(error = "Enter an amount greater than 0.") }
            return
        }
        val s = _state.value
        val input = ExpenseInput(
            expenseDate = s.date.ifBlank { LocalDate.now().toString() },
            amount = amount,
            category = s.category,
            vendor = s.vendor.ifBlank { null },
            description = s.description.ifBlank { null },
            jobId = null,
        )
        _state.update { it.copy(isSaving = true, error = null) }
        viewModelScope.launch {
            expenseRepository.addExpense(userId, input, s.imageBytes)
                .onSuccess { _state.update { it.copy(isSaving = false, saved = true) } }
                .onFailure { _state.update { it.copy(isSaving = false, error = "Couldn't save. Try again.") } }
        }
    }
}
