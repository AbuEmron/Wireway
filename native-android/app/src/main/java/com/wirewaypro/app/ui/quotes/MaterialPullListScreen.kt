package com.wirewaypro.app.ui.quotes

import android.Manifest
import android.content.pm.PackageManager
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wirewaypro.app.domain.model.PriceBasis
import com.wirewaypro.app.domain.model.PullItem
import com.wirewaypro.app.domain.model.PullListResult
import com.wirewaypro.app.domain.model.Tier
import com.wirewaypro.app.ui.components.BackTopBar
import com.wirewaypro.app.ui.components.InfoRow
import com.wirewaypro.app.ui.components.SectionCard
import com.wirewaypro.app.ui.components.SectionHeader
import com.wirewaypro.app.ui.components.UpgradePrompt
import com.wirewaypro.app.ui.util.Format

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaterialPullListScreen(
    onBack: () -> Unit,
    onOpenSubscription: () -> Unit = {},
    viewModel: MaterialPullViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    // The tapped line item, paired with its section's scope for the "work this
    // covers" readout. Null = sheet closed.
    var detail by remember { mutableStateOf<Pair<String, PullItem>?>(null) }
    detail?.let { (service, item) ->
        PullItemDetailSheet(service = service, item = item, onDismiss = { detail = null })
    }

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
                UpgradePrompt(
                    hook = "Walk in with the list priced",
                    detail = "The Material Pull List turns this quote into an itemized shopping " +
                        "list with live local supply-house pricing — part of Pro.",
                    tier = Tier.PRO,
                    onUpgrade = onOpenSubscription,
                )
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
                            PullItemRow(item, onClick = { detail = section.service to item })
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
        if (result.unconfirmedCount > 0) {
            Spacer(Modifier.height(4.dp))
            Text(
                "${result.unconfirmedCount} ${if (result.unconfirmedCount == 1) "line needs" else "lines need"} a price check in store — not included in the total.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PullItemRow(item: PullItem, onClick: () -> Unit) {
    // The whole row is one big tap target (48dp+ with its padding) — gloves land it.
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
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
            val total = item.lineTotal
            Text(
                // A line whose price basis is unknown shows "confirm", never a
                // fabricated feet × package-price number (accuracy doctrine).
                text = total?.let { Format.money(it) } ?: "confirm",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = if (total != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Details",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
        }
        item.spec?.takeIf { it.isNotBlank() }?.let {
            Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        // Package math, spelled out: "2 × 25 ft coil" — the number a counter clerk
        // can sanity-check at a glance.
        if (item.basis == PriceBasis.PER_PACKAGE) {
            val pkg = item.packageSize
            val count = item.priceMultiplier?.toInt()
            if (pkg != null && count != null) {
                Text(
                    "$count × ${hoursOrQty(pkg)} ${item.unit ?: "unit"} package",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else if (item.lineTotal == null && item.price != null) {
            Text(
                "price is per package — confirm the count in store",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
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

/**
 * The in-depth line-item readout: full spec, the price math written out step by
 * step (doctrine: show your work — a wrong total should be visible, not hidden),
 * every store's price, and the scope of work the material covers.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PullItemDetailSheet(service: String, item: PullItem, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                item.name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            item.spec?.takeIf { it.isNotBlank() }?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                if (item.live) "Live-searched price — confirm in the cart." else "Typical price estimate — not a live store quote.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(10.dp))
            SectionHeader("Price breakdown")
            InfoRow("Quantity needed", "${hoursOrQty(item.qty)} ${item.unit ?: "ea"}")
            item.price?.let { InfoRow("Unit price", "${Format.money(it)} ${basisLabel(item)}") }
            if (item.basis == PriceBasis.PER_PACKAGE && item.packageSize != null) {
                InfoRow("Package size", "${hoursOrQty(item.packageSize!!)} ${item.unit ?: "ea"} per package")
            }
            // The math, written out — e.g. "50 ft ÷ 25 ft per package = 2 packages
            // × $75.00 = $150.00". If a number is ever wrong, it's wrong in plain
            // sight here, not buried in a total.
            derivationText(item)?.let {
                Spacer(Modifier.height(2.dp))
                Text(
                    it,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Spacer(Modifier.height(4.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Line total", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    item.lineTotal?.let { Format.money(it) } ?: "confirm in store",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (item.lineTotal != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (item.lineTotal == null && item.price != null) {
                Text(
                    "The price basis for this line couldn't be confirmed, so no total is shown — check the package size at the store instead of trusting a guess.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (item.prices.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                SectionHeader("Where to buy")
                val cheapest = item.prices.minByOrNull { it.price }?.store
                item.prices.sortedBy { it.price }.forEach { sp ->
                    InfoRow(
                        if (sp.store == cheapest) "✓ ${sp.store} — best price" else sp.store,
                        "${Format.money(sp.price)} ${basisLabel(item)}",
                    )
                }
            }

            Spacer(Modifier.height(10.dp))
            SectionHeader("Work this covers")
            Text(
                service,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

/** "per 25 ft package", "per ft", "per ea" — what one quoted price buys. */
private fun basisLabel(item: PullItem): String = when (item.basis) {
    PriceBasis.PER_FOOT -> "per ${item.unit?.takeIf { it.isNotBlank() } ?: "ft"}"
    PriceBasis.PER_PACKAGE ->
        item.packageSize?.let { "per ${hoursOrQty(it)} ${item.unit ?: "ea"} package" } ?: "per package"
    PriceBasis.PER_UNIT -> "per ${item.unit?.takeIf { it.isNotBlank() } ?: "ea"}"
    PriceBasis.UNKNOWN -> "(basis unconfirmed)"
}

/** The line total's arithmetic, spelled out step by step. Null when there's no trustworthy math to show. */
private fun derivationText(item: PullItem): String? {
    val p = item.price ?: return null
    val unit = item.unit?.takeIf { it.isNotBlank() } ?: "ea"
    return when {
        item.basis == PriceBasis.PER_PACKAGE && item.packageSize != null && item.priceMultiplier != null -> {
            val n = item.priceMultiplier!!.toInt()
            "${hoursOrQty(item.qty)} $unit ÷ ${hoursOrQty(item.packageSize!!)} $unit per package = " +
                "$n ${if (n == 1) "package" else "packages"} × ${Format.money(p)} = ${Format.money(item.lineTotal)}"
        }
        item.basis == PriceBasis.PER_FOOT && item.lineTotal != null ->
            "${hoursOrQty(item.qty)} $unit × ${Format.money(p)} per $unit = ${Format.money(item.lineTotal)}"
        item.basis == PriceBasis.PER_UNIT && item.lineTotal != null ->
            "${hoursOrQty(item.qty)} $unit × ${Format.money(p)} each = ${Format.money(item.lineTotal)}"
        else -> null
    }
}

private fun hoursOrQty(q: Double): String = if (q % 1.0 == 0.0) q.toLong().toString() else q.toString()

private fun buildCopyText(result: PullListResult): String =
    result.sections.joinToString("\n\n") { s ->
        s.service.uppercase() + "\n" + s.items.joinToString("\n") { i ->
            val spec = i.spec?.takeIf { it.isNotBlank() }?.let { " ($it)" } ?: ""
            val total = i.lineTotal?.let { "~${Format.money(it)}" } ?: "confirm price"
            "  [ ] ${hoursOrQty(i.qty)} ${i.unit ?: ""} — ${i.name}$spec — $total"
        }
    } + "\n\nESTIMATED TOTAL: ${Format.money(result.estTotal)}"
