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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Work
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
    onOpenJobs: () -> Unit,
    onOpenClients: () -> Unit,
    onOpenExpenses: () -> Unit,
    onOpenMoney: () -> Unit,
    onOpenTakeoff: () -> Unit,
    onOpenBank: () -> Unit,
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
            text = "BROWSE",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        QuickLink(icon = Icons.Outlined.AutoAwesome, label = "AI Takeoff", onClick = onOpenTakeoff)
        QuickLink(icon = Icons.Outlined.Work, label = "Jobs", onClick = onOpenJobs)
        QuickLink(icon = Icons.Outlined.Groups, label = "Clients", onClick = onOpenClients)
        QuickLink(icon = Icons.Outlined.ReceiptLong, label = "Expenses & receipts", onClick = onOpenExpenses)
        QuickLink(icon = Icons.Outlined.Payments, label = "Money", onClick = onOpenMoney)
        QuickLink(icon = Icons.Outlined.AccountBalance, label = "Bank", onClick = onOpenBank)

        Spacer(Modifier.height(4.dp))
        Text(
            text = "Read-only this phase. Estimates and Invoices are in the bottom tabs.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
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
