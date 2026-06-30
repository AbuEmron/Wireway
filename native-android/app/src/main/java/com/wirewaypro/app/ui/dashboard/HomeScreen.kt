package com.wirewaypro.app.ui.dashboard

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Work
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wirewaypro.app.ui.components.SectionCard
import com.wirewaypro.app.ui.components.WirewayWordmark

/**
 * Home tab. Greets the user with data pulled live from Supabase (profile + job
 * count) and offers quick links into the Jobs and Clients lists.
 */
@Composable
fun HomeScreen(
    onOpenAiQuoteBuilder: () -> Unit,
    onOpenJobs: () -> Unit,
    onOpenClients: () -> Unit,
    onOpenExpenses: () -> Unit,
    onOpenMoney: () -> Unit,
    onOpenTakeoff: () -> Unit,
    onOpenBank: () -> Unit,
    onOpenSubscription: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val state by viewModel.home.collectAsStateWithLifecycle()
    val pending by viewModel.pendingSync.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        WirewayWordmark()

        if (pending > 0) {
            Text(
                text = "⏳ ${if (pending == 1) "1 change" else "$pending changes"} waiting to sync",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        when {
            state.isLoading -> Box(
                Modifier.fillMaxWidth().height(160.dp),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }

            state.error != null && state.profile == null -> Column {
                Text(
                    text = state.error!!,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(onClick = viewModel::loadHome) { Text("Retry") }
            }

            else -> {
                val name = state.profile?.fullName
                    ?: state.profile?.email
                    ?: "there"
                Text(
                    text = "Welcome back, $name",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                )

                SectionCard(title = "This month") {
                    Text(
                        text = state.jobCount?.let {
                            if (it == 1L) "1 scheduled job" else "$it scheduled jobs"
                        } ?: "—",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }

        Text(
            text = "ESTIMATE WITH AI",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        AiHeroCard(
            icon = Icons.Outlined.AutoAwesome,
            title = "AI Quote Builder",
            subtitle = "Describe the job in plain English — get a full estimate in seconds",
            onClick = onOpenAiQuoteBuilder,
        )
        AiHeroCard(
            icon = Icons.Outlined.PhotoCamera,
            title = "AI Takeoff",
            subtitle = "Snap or upload a plan photo/PDF — AI reads it and builds the estimate",
            onClick = onOpenTakeoff,
        )

        Text(
            text = "BROWSE",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        QuickLink(icon = Icons.Outlined.Work, label = "Jobs", onClick = onOpenJobs)
        QuickLink(icon = Icons.Outlined.Groups, label = "Clients", onClick = onOpenClients)
        QuickLink(icon = Icons.Outlined.ReceiptLong, label = "Expenses & receipts", onClick = onOpenExpenses)
        QuickLink(icon = Icons.Outlined.Payments, label = "Money", onClick = onOpenMoney)
        QuickLink(icon = Icons.Outlined.AccountBalance, label = "Bank", onClick = onOpenBank)
        QuickLink(icon = Icons.Outlined.WorkspacePremium, label = "Subscription", onClick = onOpenSubscription)

        Spacer(Modifier.height(4.dp))
        Text(
            text = "Your Estimates and Invoices live in the bottom tabs.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Prominent headline card for the two AI estimating tools. */
@Composable
private fun AiHeroCard(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(32.dp),
            )
            Spacer(Modifier.padding(start = 16.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

@Composable
private fun QuickLink(icon: ImageVector, label: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.padding(start = 14.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
