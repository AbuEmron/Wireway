package com.wirewaypro.app.ui.expenses

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wirewaypro.app.domain.model.Expense
import com.wirewaypro.app.domain.model.ExpenseCategories
import com.wirewaypro.app.ui.components.BackTopBar
import com.wirewaypro.app.ui.components.ConfirmDialog
import com.wirewaypro.app.ui.components.EmptyState
import com.wirewaypro.app.ui.components.ListCard
import com.wirewaypro.app.ui.components.ListCardSkeleton
import com.wirewaypro.app.ui.components.RefreshOnReturn
import com.wirewaypro.app.ui.components.RefreshableList
import com.wirewaypro.app.ui.util.Format

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpensesScreen(
    onBack: () -> Unit,
    onAdd: () -> Unit,
    viewModel: ExpensesViewModel = hiltViewModel(),
    batchViewModel: BatchReceiptViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val batchState by batchViewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var pendingDelete by remember { mutableStateOf<Expense?>(null) }
    var showBatchCamera by remember { mutableStateOf(false) }
    RefreshOnReturn(viewModel::refresh)

    val cameraPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) showBatchCamera = true
    }
    fun startBatchScan() {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
        if (granted) showBatchCamera = true else cameraPermission.launch(Manifest.permission.CAMERA)
    }

    // Full-screen batch camera overlay: snap many receipts, save each as a draft.
    if (showBatchCamera) {
        CameraCapture(
            onClose = { showBatchCamera = false },
            batchMode = true,
            onBatchDone = { uris ->
                showBatchCamera = false
                batchViewModel.process(uris) { viewModel.refresh() }
            },
        )
        return
    }

    Scaffold(
        topBar = {
            BackTopBar(
                title = "Expenses & receipts",
                onBack = onBack,
                actions = {
                    IconButton(onClick = { startBatchScan() }) {
                        Icon(Icons.Outlined.PhotoCamera, contentDescription = "Scan receipts")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAdd) {
                Icon(Icons.Filled.Add, contentDescription = "New expense")
            }
        },
    ) { padding ->
        RefreshableList(
            isLoading = state.isLoading,
            isRefreshing = state.isRefreshing,
            error = state.error,
            isEmpty = state.isEmpty,
            emptyMessage = "No expenses yet. Snap a receipt to add one.",
            onRefresh = viewModel::refresh,
            modifier = Modifier.padding(padding),
            skeleton = { ListCardSkeleton() },
            emptyContent = {
                EmptyState(
                    icon = Icons.Outlined.PhotoCamera,
                    title = "No expenses yet",
                    message = "Snap a receipt and it's logged — every material run counts against the job's real profit.",
                    actionLabel = "Scan receipts",
                    onAction = { startBatchScan() },
                )
            },
        ) {
            items(state.items, key = { it.id }) { expense ->
                ExpenseRow(expense = expense, onClick = { pendingDelete = expense })
            }
        }
    }

    pendingDelete?.let { expense ->
        ConfirmDialog(
            title = "Delete expense?",
            message = "This permanently deletes this expense" +
                (if (expense.receiptUrl != null) " and unlinks its receipt." else "."),
            onConfirm = { viewModel.delete(expense.id); pendingDelete = null },
            onDismiss = { pendingDelete = null },
        )
    }

    // Blocking progress while the batch saves, then a one-tap summary.
    if (batchState.isProcessing) {
        Surface(color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.6f)) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    CircularProgressIndicator()
                    Text(
                        "Saving receipts… ${batchState.done}/${batchState.total}",
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
        }
    }

    batchState.resultNote?.let { note ->
        AlertDialog(
            onDismissRequest = batchViewModel::clearNote,
            confirmButton = { TextButton(onClick = batchViewModel::clearNote) { Text("OK") } },
            title = { Text("Receipts saved") },
            text = { Text(note) },
        )
    }
}

@Composable
private fun ExpenseRow(expense: Expense, onClick: () -> Unit) {
    val title = expense.vendor?.takeIf { it.isNotBlank() }
        ?: expense.description?.takeIf { it.isNotBlank() }
        ?: ExpenseCategories.label(expense.category)
    val subtitle = buildString {
        append(ExpenseCategories.label(expense.category))
        if (expense.receiptUrl != null) append("  ·  📎 receipt")
    }

    ListCard(
        title = title,
        onClick = onClick,
        trailing = Format.money(expense.amount),
        subtitle = subtitle,
        footerStart = Format.date(expense.expenseDate),
    )
}
