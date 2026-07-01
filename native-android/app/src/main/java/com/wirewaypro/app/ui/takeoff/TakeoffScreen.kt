package com.wirewaypro.app.ui.takeoff

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wirewaypro.app.domain.catalog.Catalog
import com.wirewaypro.app.domain.model.QuoteCalculator
import com.wirewaypro.app.domain.model.QuoteCatalogEntry
import com.wirewaypro.app.domain.model.TakeoffSuggestion
import com.wirewaypro.app.ui.components.BackTopBar
import com.wirewaypro.app.ui.components.SectionCard
import com.wirewaypro.app.ui.expenses.CameraCapture
import com.wirewaypro.app.ui.util.Format

private const val PREVIEW_RATE = 85.0 // catalog base rate; the builder uses the user's rate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TakeoffScreen(
    onBack: () -> Unit,
    onCreateEstimate: () -> Unit,
    mode: AiEstimateMode = AiEstimateMode.TAKEOFF,
    viewModel: TakeoffViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showCamera by remember { mutableStateOf(false) }

    LaunchedEffect(state.applied) { if (state.applied) onCreateEstimate() }

    val cameraPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) showCamera = true
    }
    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { viewModel.setImageFromUri(it) }
    }
    val pickPdf = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { viewModel.setPdfFromUri(it) }
    }

    if (showCamera) {
        CameraCapture(
            onCaptured = { uri -> viewModel.setImageFromUri(uri); showCamera = false },
            onClose = { showCamera = false },
        )
        return
    }

    Scaffold(topBar = { BackTopBar(title = mode.title, onBack = onBack) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SectionCard(title = mode.cardTitle) {
                Text(
                    mode.cardSubtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.padding(top = 12.dp))
                OutlinedTextField(
                    value = state.prompt,
                    onValueChange = viewModel::setPrompt,
                    label = { Text(mode.promptLabel) },
                    placeholder = { Text(mode.promptPlaceholder) },
                    minLines = 3,
                    keyboardOptions = KeyboardOptions.Default,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.padding(top = 10.dp))
                Text(
                    if (state.attachmentLabel != null) "" else mode.attachHint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.padding(top = 6.dp))
                if (state.attachmentLabel != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(state.attachmentLabel!!, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
                        TextButton(onClick = viewModel::clearAttachment) { Text("Remove") }
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                                if (granted) showCamera = true else cameraPermission.launch(Manifest.permission.CAMERA)
                            },
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.Outlined.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                        OutlinedButton(
                            onClick = { pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.Outlined.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                        OutlinedButton(
                            onClick = { pickPdf.launch("application/pdf") },
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.Outlined.Description, contentDescription = "PDF", modifier = Modifier.size(18.dp))
                        }
                    }
                }
                Spacer(Modifier.padding(top = 12.dp))
                Button(
                    onClick = viewModel::analyze,
                    enabled = !state.isAnalyzing,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (state.isAnalyzing) {
                        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                        Spacer(Modifier.padding(start = 8.dp))
                        Text("Analyzing…")
                    } else {
                        Text("Analyze")
                    }
                }
            }

            state.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }

            state.result?.let { result ->
                if (result.summary.isNotBlank()) {
                    SectionCard(title = "Summary") {
                        Text(result.summary, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
                if (result.suggestions.isEmpty()) {
                    SectionCard(title = "Result") {
                        Text("No catalog services matched. Add more detail and analyze again.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    val selectedTotal = result.suggestions
                        .filter { it.serviceId in state.selected }
                        .sumOf { QuoteCalculator.catalogLineAmount(it.toEntry(), PREVIEW_RATE) ?: 0.0 }

                    SectionCard(title = "Proposed line items") {
                        result.suggestions.forEach { sug ->
                            SuggestionRow(
                                suggestion = sug,
                                checked = sug.serviceId in state.selected,
                                onToggle = { viewModel.toggle(sug.serviceId) },
                            )
                        }
                        Spacer(Modifier.padding(top = 8.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Selected subtotal", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(Format.money(selectedTotal), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                        }
                    }

                    if (result.assumptions.isNotEmpty()) {
                        SectionCard(title = "Assumptions to confirm") {
                            result.assumptions.forEach { a ->
                                Text("• $a", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    Button(onClick = viewModel::applyToEstimate, modifier = Modifier.fillMaxWidth()) {
                        Text("Create estimate from selection")
                    }
                    Text(
                        "Amounts preview at the $${PREVIEW_RATE.toInt()}/hr base rate; adjust your rate in the builder.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun SuggestionRow(suggestion: TakeoffSuggestion, checked: Boolean, onToggle: () -> Unit) {
    val service = Catalog.service(suggestion.serviceId)
    val variant = service?.variants?.getOrNull(suggestion.variantIdx)?.label
    val amount = QuoteCalculator.catalogLineAmount(suggestion.toEntry(), PREVIEW_RATE)
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Checkbox(checked = checked, onCheckedChange = { onToggle() })
        Column(Modifier.weight(1f).padding(top = 12.dp)) {
            Text(
                buildString {
                    append(service?.label ?: suggestion.serviceId)
                    if (variant != null && (service?.variants?.size ?: 0) > 1) append(" · $variant")
                    if (suggestion.qty != 1.0) append("  ×${suggestion.qty.toInt()}")
                },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            suggestion.reason?.takeIf { it.isNotBlank() }?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Text(
            Format.money(amount),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 12.dp),
        )
    }
}

private fun TakeoffSuggestion.toEntry() =
    QuoteCatalogEntry(serviceId, qty, variantIdx, clientBuys)
