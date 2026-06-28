package com.wirewaypro.app.ui.expenses

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wirewaypro.app.domain.model.Expense
import com.wirewaypro.app.domain.model.ExpenseCategories
import com.wirewaypro.app.ui.components.BackTopBar
import com.wirewaypro.app.ui.components.ConfirmDialog
import com.wirewaypro.app.ui.components.ListCard
import com.wirewaypro.app.ui.components.RefreshOnReturn
import com.wirewaypro.app.ui.components.RefreshableList
import com.wirewaypro.app.ui.util.Format

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpensesScreen(
    onBack: () -> Unit,
    onAdd: () -> Unit,
    viewModel: ExpensesViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var pendingDelete by remember { mutableStateOf<Expense?>(null) }
    RefreshOnReturn(viewModel::refresh)

    Scaffold(
        topBar = { BackTopBar(title = "Expenses & receipts", onBack = onBack) },
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
