package com.wirewaypro.app.ui.quotes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.wirewaypro.app.ui.components.BackTopBar
import com.wirewaypro.app.ui.components.ListCard

/**
 * Job-template picker — the fast path to an estimate without AI. Tapping a
 * template seeds the builder and opens a fresh quote pre-filled with its line
 * items ([onPicked] navigates to the new-quote builder).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssembliesScreen(
    onBack: () -> Unit,
    onPicked: () -> Unit,
    viewModel: AssembliesViewModel = hiltViewModel(),
) {
    Scaffold(
        topBar = { BackTopBar(title = "Job Templates", onBack = onBack) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(viewModel.assemblies, key = { it.id }) { assembly ->
                ListCard(
                    title = assembly.label,
                    onClick = {
                        viewModel.seed(assembly)
                        onPicked()
                    },
                    subtitle = assembly.description,
                    footerStart = if (assembly.itemCount == 1) "1 line item" else "${assembly.itemCount} line items",
                )
            }
        }
    }
}
