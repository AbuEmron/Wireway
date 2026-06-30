package com.wirewaypro.app.ui.subscription

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wirewaypro.app.ui.components.BackTopBar
import com.wirewaypro.app.ui.components.SectionCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionsScreen(
    onBack: () -> Unit,
    viewModel: SubscriptionsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Scaffold(topBar = { BackTopBar(title = "Subscription", onBack = onBack) }) { padding ->
        when {
            state.connecting -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }

            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    "Wireway Pro unlocks AI takeoff, unlimited estimates, and bookkeeping.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                if (!state.available) {
                    SectionCard(title = "Subscriptions unavailable") {
                        Text(
                            state.status ?: "Subscriptions can't be loaded right now.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    state.products.forEach { product ->
                        SectionCard(title = product.title) {
                            if (product.price.isNotBlank()) {
                                Text(
                                    product.price,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                                Spacer(Modifier.height(10.dp))
                            }
                            Button(
                                onClick = { context.findActivity()?.let { viewModel.purchase(it, product.id) } },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text("Subscribe")
                            }
                        }
                    }
                    state.status?.let {
                        Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                    }
                }

                // Google Play requires the manage/cancel path to run through the Play
                // subscription center — deep-link out to it rather than cancelling in-app.
                // Shown regardless of load state so an existing subscriber always has a way out.
                TextButton(
                    onClick = { openManageSubscriptions(context) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Manage or cancel subscription")
                }
            }
        }
    }
}

/** Opens the Google Play subscription center for this app (manage / cancel / change plan). */
private fun openManageSubscriptions(context: Context) {
    val uri = Uri.parse(
        "https://play.google.com/store/account/subscriptions?package=${context.packageName}",
    )
    val playIntent = Intent(Intent.ACTION_VIEW, uri).setPackage("com.android.vending")
    val launched = runCatching { context.startActivity(playIntent); true }.getOrDefault(false)
    if (!launched) runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) }
}

private fun Context.findActivity(): Activity? {
    var ctx: Context? = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}
