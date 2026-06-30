package com.wirewaypro.app.ui.quotes

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wirewaypro.app.domain.model.PullItem
import com.wirewaypro.app.domain.model.PullListResult
import com.wirewaypro.app.ui.components.BackTopBar
import com.wirewaypro.app.ui.components.SectionCard
import com.wirewaypro.app.ui.util.Format

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaterialPullListScreen(
    onBack: () -> Unit,
    viewModel: MaterialPullViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    val locationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> if (granted) viewModel.useMyLocation() }

    fun requestGps() {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        if (granted) viewModel.useMyLocation() else locationPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { BackTopBar(title = "Material Pull List", onBack = onBack) },
    ) { padding ->
        if (state.isLoadingQuote) {
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
            if (!state.isPro) {
                SectionCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.WorkspacePremium, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(10.dp))
                        Text("A Pro feature", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "The Material Pull List builds an itemized shopping list with live local " +
                            "pricing. It's part of Wireway Pro — upgrade from the Subscription screen to use it.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                return@Column
            }

            SectionCard(title = "Where's the job?") {
                Text(
                    "I'll check live prices at the supply houses near this location.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = state.locationInput,
                    onValueChange = viewModel::setLocationInput,
                    label = { Text("Job location (city, state or address)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = { requestGps() }, enabled = !state.locatingArea, modifier = Modifier.fillMaxWidth()) {
                    if (state.locatingArea) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text("Finding your location…")
                    } else {
                        Icon(Icons.Outlined.MyLocation, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                        Text("Use my GPS location")
                    }
                }
            }

            Button(
                onClick = viewModel::build,
                enabled = !state.building,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (state.building) "Building…" else "Build pull list")
            }

            if (state.building) {
                SectionCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(12.dp))
                        Text(
                            "Building your pull list and checking live prices at the local stores… " +
                                "this takes about 45–60 seconds.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            state.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }

            state.result?.let { result ->
                ResultHeader(result)
                result.sections.forEach { section ->
                    SectionCard(title = section.service) {
                        section.items.forEachIndexed { i, item ->
                            if (i > 0) Spacer(Modifier.height(10.dp))
                            PullItemRow(item)
                        }
                    }
                }
                result.notes?.takeIf { it.isNotBlank() }?.let { notes ->
                    SectionCard(title = "Notes") {
                        Text(notes, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
                OutlinedButton(
                    onClick = { clipboard.setText(AnnotatedString(buildCopyText(result))) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Outlined.ContentCopy, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                    Text("Copy checklist")
                }
                Text(
                    "Wire, panels, breakers & big-ticket items are priced live; small parts are " +
                        "estimated — verify in cart. Local electrical distributors often beat big-box on wire and breakers.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ResultHeader(result: PullListResult) {
    SectionCard {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Estimated total", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                Format.money(result.estTotal),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        if (result.savings > 0.5) {
            Spacer(Modifier.height(4.dp))
            Text(
                "You save about ${Format.money(result.savings)} buying each item at its cheapest store.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PullItemRow(item: PullItem) {
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                buildString {
                    append(hoursOrQty(item.qty))
                    item.unit?.takeIf { it.isNotBlank() }?.let { append(" ").append(it) }
                    append(" · ").append(item.name)
                },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(12.dp))
            Text(
                Format.money(item.lineTotal),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        item.spec?.takeIf { it.isNotBlank() }?.let {
            Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (item.prices.isNotEmpty()) {
            val cheapest = item.prices.minByOrNull { it.price }?.store
            Text(
                item.prices.joinToString("   ") { sp ->
                    val mark = if (sp.store == cheapest) "✓ " else ""
                    "$mark${sp.store} ${Format.money(sp.price)}"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
            )
        } else if (!item.live) {
            Text("estimated", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun hoursOrQty(q: Double): String = if (q % 1.0 == 0.0) q.toLong().toString() else q.toString()

private fun buildCopyText(result: PullListResult): String =
    result.sections.joinToString("\n\n") { s ->
        s.service.uppercase() + "\n" + s.items.joinToString("\n") { i ->
            val spec = i.spec?.takeIf { it.isNotBlank() }?.let { " ($it)" } ?: ""
            "  [ ] ${hoursOrQty(i.qty)} ${i.unit ?: ""} — ${i.name}$spec — ~${Format.money(i.lineTotal)}"
        }
    } + "\n\nESTIMATED TOTAL: ${Format.money(result.estTotal)}"
