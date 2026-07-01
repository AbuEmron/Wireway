package com.wirewaypro.app.ui.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.TrendingDown
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material.icons.outlined.Work
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wirewaypro.app.domain.model.MoneySnapshot
import com.wirewaypro.app.ui.components.NavRow
import com.wirewaypro.app.ui.components.SectionEyebrow
import com.wirewaypro.app.ui.components.StatCard
import com.wirewaypro.app.ui.components.WirewayWordmark
import com.wirewaypro.app.ui.components.animatedCount
import com.wirewaypro.app.ui.theme.BrandGradients
import com.wirewaypro.app.ui.theme.BrandGreen
import com.wirewaypro.app.ui.util.Format

/**
 * Home tab — the flagship dashboard. A gradient hero counts up the money collected
 * this month, a stat grid summarizes the rest, and soft list rows lead into the
 * estimating tools and the core lists.
 */
@Composable
fun HomeScreen(
    onOpenAiQuoteBuilder: () -> Unit,
    onOpenJobs: () -> Unit,
    onOpenClients: () -> Unit,
    onOpenExpenses: () -> Unit,
    onOpenMileage: () -> Unit,
    onOpenTimeTracking: () -> Unit,
    onOpenNec: () -> Unit,
    onOpenLoadAdvisor: () -> Unit,
    onOpenMoney: () -> Unit,
    onOpenTakeoff: () -> Unit,
    onOpenAssemblies: () -> Unit,
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            WirewayWordmark()
        }

        AnimatedVisibility(visible = pending > 0, enter = fadeIn(), exit = fadeOut()) {
            Text(
                text = "⏳ ${if (pending == 1) "1 change" else "$pending changes"} waiting to sync",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        val name = state.profile?.fullName ?: state.profile?.email?.substringBefore("@") ?: "there"
        Text(
            text = "Welcome back, $name",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        HeroCard(
            isLoading = state.isLoading && state.snapshot == null,
            snapshot = state.snapshot,
            onClick = onOpenMoney,
        )

        if (state.error != null && state.profile == null) {
            Text(
                text = state.error!!,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(onClick = viewModel::loadHome) { Text("Retry") }
        }

        // ── Stat grid ────────────────────────────────────────────────────────
        val snap = state.snapshot
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            StatCard(
                label = "Spent",
                value = Format.money(snap?.spent ?: 0.0),
                icon = Icons.Outlined.TrendingDown,
                accent = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.weight(1f),
            )
            StatCard(
                label = "Real profit",
                value = Format.money(snap?.realProfit ?: 0.0),
                icon = Icons.Outlined.TrendingUp,
                accent = if ((snap?.realProfit ?: 0.0) >= 0) BrandGreen else MaterialTheme.colorScheme.error,
                modifier = Modifier.weight(1f),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            StatCard(
                label = if (state.jobCount == 1L) "Scheduled job" else "Scheduled jobs",
                value = state.jobCount?.toString() ?: "—",
                icon = Icons.Outlined.Work,
                modifier = Modifier.weight(1f),
            )
            StatCard(
                label = "Won this month",
                value = Format.money(snap?.won ?: 0.0),
                icon = Icons.Outlined.Savings,
                accent = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.weight(1f),
            )
        }

        // ── Start fast (no AI) ────────────────────────────────────────────────
        // Job templates are the fast path to an estimate without AI — free to all.
        SectionEyebrow("Start fast", modifier = Modifier.padding(top = 4.dp))
        AiHeroCard(
            icon = Icons.Outlined.Bolt,
            title = "Job Templates",
            subtitle = "Panel swap, EV charger, can-lights & more — a pre-filled estimate in one tap",
            onClick = onOpenAssemblies,
        )

        // ── Estimate with AI ─────────────────────────────────────────────────
        // AI Takeoff is an Elite-tier feature; non-Elite users see it as a locked
        // upsell that routes to the Subscription screen.
        val isElite = state.profile?.isElite == true
        SectionEyebrow("Estimate with AI", modifier = Modifier.padding(top = 4.dp))
        AiHeroCard(
            icon = Icons.Outlined.AutoAwesome,
            title = "AI Quote Builder",
            subtitle = "Describe the job in plain English — get a full estimate in seconds",
            onClick = onOpenAiQuoteBuilder,
        )
        AiHeroCard(
            icon = Icons.Outlined.PhotoCamera,
            title = "AI Takeoff",
            subtitle = if (isElite) {
                "Snap or upload a plan photo/PDF — AI reads it and builds the estimate"
            } else {
                "Unlock AI plan takeoff from a photo or PDF with Elite"
            },
            onClick = if (isElite) onOpenTakeoff else onOpenSubscription,
            locked = !isElite,
        )

        // ── Browse ───────────────────────────────────────────────────────────
        SectionEyebrow("Browse", modifier = Modifier.padding(top = 4.dp))
        NavRow(label = "Jobs", icon = Icons.Outlined.Work, onClick = onOpenJobs, modifier = Modifier.fillMaxWidth())
        NavRow(label = "Clients", icon = Icons.Outlined.Groups, onClick = onOpenClients, modifier = Modifier.fillMaxWidth())
        NavRow(label = "Expenses & receipts", icon = Icons.Outlined.ReceiptLong, onClick = onOpenExpenses, modifier = Modifier.fillMaxWidth())
        NavRow(label = "Mileage", icon = Icons.Outlined.DirectionsCar, onClick = onOpenMileage, modifier = Modifier.fillMaxWidth())
        NavRow(label = "Time tracking", icon = Icons.Outlined.Schedule, onClick = onOpenTimeTracking, modifier = Modifier.fillMaxWidth())
        NavRow(label = "NEC code reference", icon = Icons.Outlined.MenuBook, onClick = onOpenNec, modifier = Modifier.fillMaxWidth())
        NavRow(label = "Load advisor", icon = Icons.Outlined.Bolt, onClick = onOpenLoadAdvisor, modifier = Modifier.fillMaxWidth())
        NavRow(label = "Money", icon = Icons.Outlined.Payments, onClick = onOpenMoney, modifier = Modifier.fillMaxWidth())
        NavRow(label = "Bank", icon = Icons.Outlined.AccountBalance, onClick = onOpenBank, modifier = Modifier.fillMaxWidth())
        NavRow(label = "Subscription", icon = Icons.Outlined.WorkspacePremium, onClick = onOpenSubscription, modifier = Modifier.fillMaxWidth())

        Spacer(Modifier.height(4.dp))
        Text(
            text = "Your Estimates and Invoices live in the bottom tabs.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * The gradient hero: an animated blue→purple panel showing the money collected this
 * month, counting up on load. Tapping it opens the Money dashboard.
 */
@Composable
private fun HeroCard(
    isLoading: Boolean,
    snapshot: MoneySnapshot?,
    onClick: () -> Unit,
) {
    // Slow, looping sweep of the gradient for a subtle "live current" shimmer.
    val transition = rememberInfiniteTransition(label = "hero-gradient")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6000),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "hero-progress",
    )
    val collected = snapshot?.collected ?: 0.0
    val shown = animatedCount(collected)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .clickable(onClick = onClick)
            .drawBehind { drawRect(brush = BrandGradients.animated(progress)) }
            .padding(22.dp),
    ) {
        Column {
            Text(
                text = "COLLECTED THIS MONTH",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.85f),
            )
            Spacer(Modifier.height(10.dp))
            if (isLoading) {
                CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(34.dp))
            } else {
                Text(
                    text = Format.money(shown),
                    style = MaterialTheme.typography.displayLarge,
                    color = Color.White,
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                val profit = snapshot?.realProfit ?: 0.0
                Box(
                    modifier = Modifier
                        .drawBehind {
                            drawRoundRect(
                                color = Color.White.copy(alpha = 0.18f),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(20f, 20f),
                            )
                        }
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                ) {
                    Text(
                        text = "${if (profit >= 0) "▲" else "▼"} ${Format.money(profit)} profit",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White,
                    )
                }
                Spacer(Modifier.weight(1f))
                Text(
                    text = "View money →",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White.copy(alpha = 0.9f),
                )
            }
        }
    }
}

/** Prominent headline card for the two AI estimating tools. */
@Composable
private fun AiHeroCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    locked: Boolean = false,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .drawBehind {
                drawRoundRect(
                    brush = BrandGradients.primary,
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(20.dp.toPx(), 20.dp.toPx()),
                    alpha = 0.14f,
                )
            },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .drawBehind {
                        drawRoundRect(
                            brush = BrandGradients.primary,
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(14.dp.toPx(), 14.dp.toPx()),
                        )
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.size(16.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    if (locked) {
                        Spacer(Modifier.size(8.dp))
                        Text(
                            text = "ELITE",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .drawBehind { drawRect(brush = BrandGradients.primary) }
                                .padding(horizontal = 7.dp, vertical = 2.dp),
                        )
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                if (locked) Icons.Outlined.Lock else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}
