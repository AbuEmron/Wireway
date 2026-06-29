package com.wirewaypro.app.ui.takeoff

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wirewaypro.app.data.ai.AiTakeoffService
import com.wirewaypro.app.data.ai.TakeoffHandoff
import com.wirewaypro.app.domain.model.QuoteCatalogEntry
import com.wirewaypro.app.domain.model.TakeoffResult
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
import javax.inject.Inject

data class TakeoffUiState(
    val prompt: String = "",
    val imageBytes: ByteArray? = null,
    val attachmentLabel: String? = null, // e.g. "Photo attached" / "PDF attached"
    val isAnalyzing: Boolean = false,
    val result: TakeoffResult? = null,
    val selected: Set<String> = emptySet(),
    val error: String? = null,
    val applied: Boolean = false,
)

@HiltViewModel
class TakeoffViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val takeoffService: AiTakeoffService,
    private val handoff: TakeoffHandoff,
) : ViewModel() {

    private val _state = MutableStateFlow(TakeoffUiState())
    val state: StateFlow<TakeoffUiState> = _state.asStateFlow()

    fun setPrompt(v: String) = _state.update { it.copy(prompt = v, error = null) }

    fun clearAttachment() = _state.update { it.copy(imageBytes = null, attachmentLabel = null) }

    fun setImageFromUri(uri: Uri) = decode("Photo attached") { ImageUtil.downscaleToJpeg(appContext, uri) }

    fun setPdfFromUri(uri: Uri) = decode("PDF attached") { ImageUtil.pdfFirstPageToJpeg(appContext, uri) }

    private fun decode(label: String, block: () -> ByteArray?) {
        viewModelScope.launch {
            val bytes = withContext(Dispatchers.IO) { block() }
            if (bytes == null) {
                _state.update { it.copy(error = "Couldn't read that file.") }
            } else {
                _state.update { it.copy(imageBytes = bytes, attachmentLabel = label, error = null) }
            }
        }
    }

    fun analyze() {
        val s = _state.value
        if (s.prompt.isBlank() && s.imageBytes == null) {
            _state.update { it.copy(error = "Describe the job or attach a plan first.") }
            return
        }
        _state.update { it.copy(isAnalyzing = true, error = null, result = null, selected = emptySet()) }
        viewModelScope.launch {
            takeoffService.analyze(s.prompt.trim(), s.imageBytes)
                .onSuccess { result ->
                    _state.update {
                        it.copy(
                            isAnalyzing = false,
                            result = result,
                            selected = result.suggestions.map { sug -> sug.serviceId }.toSet(),
                        )
                    }
                }
                .onFailure { _state.update { it.copy(isAnalyzing = false, error = "Couldn't analyze. Try again or add detail.") } }
        }
    }

    fun toggle(serviceId: String) = _state.update {
        it.copy(selected = if (serviceId in it.selected) it.selected - serviceId else it.selected + serviceId)
    }

    /** Stash the selected suggestions for the builder and signal navigation. */
    fun applyToEstimate() {
        val result = _state.value.result ?: return
        val selected = _state.value.selected
        val entries = result.suggestions
            .filter { it.serviceId in selected }
            .map { QuoteCatalogEntry(it.serviceId, it.qty, it.variantIdx, it.clientBuys) }
        if (entries.isEmpty()) {
            _state.update { it.copy(error = "Select at least one item.") }
            return
        }
        handoff.put(entries)
        _state.update { it.copy(applied = true) }
    }
}
