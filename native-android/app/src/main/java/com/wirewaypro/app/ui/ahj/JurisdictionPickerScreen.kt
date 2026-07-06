package com.wirewaypro.app.ui.ahj

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wirewaypro.app.ui.components.BackTopBar
import com.wirewaypro.app.ui.components.FormField
import com.wirewaypro.app.ui.components.GradientButton
import com.wirewaypro.app.ui.components.PickerField
import com.wirewaypro.app.ui.components.SearchField
import com.wirewaypro.app.ui.components.SectionCard

/**
 * The universal AHJ jurisdiction picker: anyone sets their Authority Having
 * Jurisdiction — state (required) → county → city/AHJ (optional). A "Use my
 * location" assist can pre-suggest it, but the user always confirms and can
 * override. The honest coverage for the current selection previews live below.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JurisdictionPickerScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit = onBack,
    viewModel: JurisdictionViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var statePickerOpen by remember { mutableStateOf(false) }

    val locationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> viewModel.onLocationPermissionResult(granted) }

    // Leave the screen once the save has committed to Room (offline-safe).
    androidx.compose.runtime.LaunchedEffect(state.saved) { if (state.saved) onSaved() }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { BackTopBar(title = "Jurisdiction", onBack = onBack) },
    ) { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                "Set the jurisdiction that inspects your work. We check your estimates against the " +
                    "code your local inspector actually enforces — not a hardcoded national default.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            OutlinedButton(
                onClick = {
                    if (state.needsLocationPermission) {
                        locationPermission.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                    } else {
                        viewModel.useMyLocation()
                    }
                },
                enabled = !state.gpsBusy,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (state.gpsBusy) {
                    CircularProgressIndicator(Modifier.height(18.dp).padding(end = 8.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Outlined.MyLocation, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                }
                Text("Use my location")
            }
            if (state.gpsApplied) {
                Text(
                    "Pre-filled from your location — confirm it's right or change it below.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            SectionCard(title = "Your jurisdiction") {
                PickerField(
                    label = "State / territory",
                    value = state.selectedState?.name ?: "",
                    placeholder = "Select your state",
                    icon = Icons.Outlined.Place,
                    onClick = { statePickerOpen = true },
                )
                Spacer(Modifier.height(12.dp))
                FormField(
                    value = state.draft.county,
                    onValueChange = viewModel::setCounty,
                    label = "County (optional)",
                    enabled = state.draft.countyEnabled,
                )
                Spacer(Modifier.height(12.dp))
                FormField(
                    value = state.draft.city,
                    onValueChange = viewModel::setCity,
                    label = "City / local AHJ (optional)",
                    enabled = state.draft.cityEnabled,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "State alone is enough — county and city just narrow it down.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // The honest coverage for the current selection, updating as they pick.
            SectionCard(title = "What we'll check against") {
                AhjCoverageContent(state.preview)
            }

            state.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }

            GradientButton(
                text = if (state.draft.id == null) "Save jurisdiction" else "Update jurisdiction",
                onClick = viewModel::save,
                enabled = state.canSave,
                loading = state.saving,
            )
            Spacer(Modifier.height(8.dp))
        }
    }

    if (statePickerOpen) {
        StatePickerDialog(
            states = state.states,
            onSelect = { code -> viewModel.selectState(code); statePickerOpen = false },
            onDismiss = { statePickerOpen = false },
        )
    }
}

/** A searchable state/territory selector — 56 entries, so a filtered list beats a menu. */
@Composable
private fun StatePickerDialog(
    states: List<com.wirewaypro.app.domain.ahj.UsState>,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(query, states) {
        val q = query.trim().lowercase()
        if (q.isBlank()) states
        else states.filter { it.name.lowercase().contains(q) || it.code.lowercase() == q }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select state") },
        text = {
            Column {
                SearchField(value = query, onValueChange = { query = it }, placeholder = "Search states")
                Spacer(Modifier.height(8.dp))
                LazyColumn(Modifier.heightIn(max = 360.dp).fillMaxWidth()) {
                    items(filtered, key = { it.code }) { s ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(s.code) }
                                .padding(vertical = 14.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                s.code,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.width(44.dp),
                            )
                            Text(s.name, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
