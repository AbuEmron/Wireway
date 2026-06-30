package com.wirewaypro.app.ui.getpaid

import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wirewaypro.app.ui.components.BackTopBar
import com.wirewaypro.app.ui.components.SectionCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GetPaidScreen(
    onBack: () -> Unit,
    viewModel: GetPaidViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Re-check status whenever the screen resumes — i.e. after the user returns
    // from the Stripe onboarding browser tab.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refreshStatus() }

    // Open the Stripe-hosted onboarding URL in a Chrome Custom Tab.
    LaunchedEffect(state.onboardingUrl) {
        val url = state.onboardingUrl ?: return@LaunchedEffect
        runCatching { CustomTabsIntent.Builder().build().launchUrl(context, Uri.parse(url)) }
        viewModel.onboardingUrlConsumed()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { BackTopBar(title = "Get paid", onBack = onBack) },
    ) { padding ->
        if (state.loading && !state.connected && !state.chargesEnabled) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SectionCard(title = "Accept payments") {
                val (headline, body) = when {
                    state.chargesEnabled -> "Connected ✓" to
                        "Your Stripe account is set up — clients can pay your invoices and progress draws by card or bank (ACH)."
                    state.connected -> "Setup not finished" to
                        "You started connecting Stripe but haven't finished. Finish setup to start getting paid."
                    else -> "Get set up to get paid" to
                        "Connect a Stripe account so clients can pay your invoices and draws online by card or bank (ACH). Money goes straight to you — Wireway never holds your funds."
                }
                Text(headline, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

                if (!state.chargesEnabled) {
                    Spacer(Modifier.height(14.dp))
                    Button(
                        onClick = viewModel::startOnboarding,
                        enabled = !state.startingOnboarding,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        if (state.startingOnboarding) {
                            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                            Spacer(Modifier.size(8.dp))
                            Text("Opening Stripe…")
                        } else {
                            Text(if (state.connected) "Finish setup" else "Connect Stripe")
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = viewModel::refreshStatus,
                    enabled = !state.loading,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(if (state.loading) "Checking…" else "Refresh status")
                }
            }

            state.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
