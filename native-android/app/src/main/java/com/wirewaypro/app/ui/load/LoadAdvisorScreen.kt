package com.wirewaypro.app.ui.load

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
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.wirewaypro.app.domain.load.LoadAdvisor
import com.wirewaypro.app.ui.components.BackTopBar
import com.wirewaypro.app.ui.components.SectionCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoadAdvisorScreen(onBack: () -> Unit) {
    var serviceAmps by remember { mutableStateOf(200) }
    var sqft by remember { mutableStateOf("") }
    var heatAcVa by remember { mutableStateOf("") }
    var loadId by remember { mutableStateOf(LoadAdvisor.NEW_LOADS.first().id) }

    val sqftVal = sqft.toIntOrNull()
    val newLoad = LoadAdvisor.NEW_LOADS.firstOrNull { it.id == loadId }
    val result = if (sqftVal != null && sqftVal > 0 && newLoad != null) {
        LoadAdvisor.calculate(
            serviceAmps = serviceAmps,
            sqft = sqftVal,
            existingFixedVa = 0.0,
            newLoadVa = newLoad.va,
            heatOrAcVa = heatAcVa.toDoubleOrNull() ?: 0.0,
        )
    } else null

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { BackTopBar(title = "Load advisor", onBack = onBack) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SectionCard(title = "Service size") {
                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    LoadAdvisor.SERVICE_SIZES.forEach { amps ->
                        FilterChip(
                            selected = serviceAmps == amps,
                            onClick = { serviceAmps = amps },
                            label = { Text("$amps A") },
                        )
                    }
                }
            }

            SectionCard(title = "The home") {
                OutlinedTextField(
                    value = sqft, onValueChange = { sqft = it },
                    label = { Text("Living area (sq ft)") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = heatAcVa, onValueChange = { heatAcVa = it },
                    label = { Text("Existing heat/AC load (VA, optional)") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            SectionCard(title = "Load to add") {
                LoadAdvisor.NEW_LOADS.forEach { load ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(selected = loadId == load.id, onClick = { loadId = load.id })
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = loadId == load.id, onClick = { loadId = load.id })
                        Text(load.label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }

            if (result != null) {
                SectionCard(title = "Result (NEC 220.83)") {
                    Text(
                        "${"%.0f".format(result.amps)} A needed",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = if (result.withinEightyPct) MaterialTheme.colorScheme.primary
                        else if (result.fits) MaterialTheme.colorScheme.tertiary
                        else MaterialTheme.colorScheme.error,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Calculated demand ${"%,.0f".format(result.totalVa)} VA on a ${result.serviceAmps} A service.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(result.recommendation, style = MaterialTheme.typography.bodyLarge)
                }
            }

            Text(
                "Estimate using NEC 220.83. A licensed load calculation on the actual panel governs.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
