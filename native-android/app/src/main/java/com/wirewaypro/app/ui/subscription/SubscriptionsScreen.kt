package com.wirewaypro.app.ui.subscription

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
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
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wirewaypro.app.ui.components.BackTopBar
import com.wirewaypro.app.ui.components.GradientButton
import com.wirewaypro.app.ui.components.SectionCard
import com.wirewaypro.app.ui.theme.BrandGradients

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
                // Elite is not publicly purchasable yet — only Pro/Teams are shown
                // as buyable plans. Internal testers get Elite via their server plan.
                val purchasable = state.products.filter { it.tier != "Elite" }

                if (!state.available) {
                    SectionCard(title = "Subscriptions unavailable") {
                        Text(
                            state.status ?: "Subscriptions can't be loaded right now.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    purchasable.forEach { product ->
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
                            GradientButton(
                                text = "Subscribe",
                                onClick = { context.findActivity()?.let { viewModel.purchase(it, product.id) } },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                    state.status?.let {
                        Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                    }
                }

                // Elite is fully built and live for internal testers, but not yet
                // sold — present it as a teaser, never a purchase.
                EliteComingSoonCard()

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

/**
 * Elite teaser. Elite is fully built and enabled for internal testers (server
 * plan = 'elite'), but not yet publicly purchasable — so this is a "coming soon"
 * card: a badge and a checklist of what's on the way, with NO purchase CTA.
 */
@Composable
private fun EliteComingSoonCard() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .drawBehind { drawRect(brush = BrandGradients.primary) }
            .padding(20.dp),
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.WorkspacePremium,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp),
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "Wireway Pro Elite",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.weight(1f),
                )
                // "Coming soon" status badge — deliberately not a button.
                Text(
                    text = "COMING SOON",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .drawBehind { drawRect(color = Color.White.copy(alpha = 0.22f)) }
                        .padding(horizontal = 9.dp, vertical = 4.dp),
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = "The business tier — here's what's on the way:",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.9f),
            )
            Spacer(Modifier.height(12.dp))
            EliteFeature("Crew roster + time tracking")
            EliteFeature("True job costing — actuals vs. estimate")
            EliteFeature("AI blueprint takeoff — estimate from the plans")
            EliteFeature("Advanced electrical calculators")
            EliteFeature("Deep material manager")
            Spacer(Modifier.height(12.dp))
            Text(
                text = "We'll let you know the moment it's ready.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.85f),
            )
        }
    }
}

@Composable
private fun EliteFeature(text: String) {
    Row(
        modifier = Modifier.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Outlined.CheckCircle,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.92f),
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.95f),
        )
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
