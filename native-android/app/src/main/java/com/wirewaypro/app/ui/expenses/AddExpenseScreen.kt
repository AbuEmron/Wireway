package com.wirewaypro.app.ui.expenses

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wirewaypro.app.domain.model.ExpenseCategories
import com.wirewaypro.app.ui.components.DateField
import com.wirewaypro.app.ui.components.FormField
import com.wirewaypro.app.ui.components.SaveTopBar
import com.wirewaypro.app.ui.components.SectionCard

@Composable
fun AddExpenseScreen(
    onClose: () -> Unit,
    viewModel: AddExpenseViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showCamera by remember { mutableStateOf(false) }

    LaunchedEffect(state.saved) { if (state.saved) onClose() }

    val cameraPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) showCamera = true
    }
    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { viewModel.setImageFromUri(it) }
    }

    // Full-screen in-app camera overlay.
    if (showCamera) {
        CameraCapture(
            onCaptured = { uri -> viewModel.setImageFromUri(uri); showCamera = false },
            onClose = { showCamera = false },
        )
        return
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            SaveTopBar(
                title = "New expense",
                onBack = onClose,
                onSave = viewModel::save,
                saveEnabled = !state.isSaving,
                saving = state.isSaving,
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SectionCard(title = "Receipt") {
                val bytes = state.imageBytes
                if (bytes != null) {
                    val bitmap = remember(bytes) {
                        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
                    }
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap,
                            contentDescription = "Receipt",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxWidth().height(220.dp),
                        )
                    }
                    if (state.isScanning) {
                        Spacer(Modifier.padding(top = 8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(Modifier.height(18.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.padding(start = 10.dp))
                            Text("Scanning receipt…", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    state.scanNote?.let {
                        Spacer(Modifier.padding(top = 6.dp))
                        Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.padding(top = 8.dp))
                    TextButton(onClick = viewModel::clearImage) { Text("Remove photo") }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(
                            onClick = {
                                val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                                    PackageManager.PERMISSION_GRANTED
                                if (granted) showCamera = true else cameraPermission.launch(Manifest.permission.CAMERA)
                            },
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.Outlined.PhotoCamera, contentDescription = null, modifier = Modifier.padding(end = 6.dp))
                            Text("Camera")
                        }
                        OutlinedButton(
                            onClick = {
                                pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            },
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.Outlined.PhotoLibrary, contentDescription = null, modifier = Modifier.padding(end = 6.dp))
                            Text("Gallery")
                        }
                    }
                }
            }

            SectionCard(title = "Expense") {
                FormField(state.amount, viewModel::setAmount, "Amount $", keyboardType = KeyboardType.Number)
                Spacer(Modifier.padding(top = 10.dp))
                DateField("Date", state.date, viewModel::setDate)
                Spacer(Modifier.padding(top = 12.dp))
                Text("Category", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.padding(top = 6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ExpenseCategories.ALL.forEach { cat ->
                        FilterChip(
                            selected = cat.id == state.category,
                            onClick = { viewModel.setCategory(cat.id) },
                            label = { Text(cat.label) },
                        )
                    }
                }
                Spacer(Modifier.padding(top = 12.dp))
                FormField(state.vendor, viewModel::setVendor, "Vendor")
                Spacer(Modifier.padding(top = 10.dp))
                FormField(state.description, viewModel::setDescription, "Description", singleLine = false)
            }

            if (state.error != null) {
                Text(state.error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
