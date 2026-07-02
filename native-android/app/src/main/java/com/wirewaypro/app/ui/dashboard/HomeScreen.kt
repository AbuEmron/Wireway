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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import com.wirewaypro.app.ui.components.ErrorState
import com.wirewaypro.app.ui.components.SectionEyebrow
import com.wirewaypro.app.ui.components.ShimmerBox
import com.wirewaypro.app.ui.components.StatCard
import com.wirewaypro.app.ui.components.WirewayWordmark
import com.wirewaypro.app.ui.components.animatedCount
import com.wirewaypro.app.ui.components.shimmer
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
            text = "${greeting()}, $name",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        HeroCard(
            isLoading = state.isLoading && state.snapshot == null,
            snapshot = state.snapshot,
            onClick = onOpenMoney,
        )

        if (state.error != null && state.profile == null) {
            ErrorState(
                title = "Couldn't load your dashboard",
                message = state.error!!,
                actionLabel = "Try again",
                onAction = viewModel::loadHome,
            )
        }

        val snap = state.snapshot
        val isElite = state.profile?.isElite == true

        // ── Quick actions — the core money loop, one tap away ─────────────────
        SectionEyebrow("Quick actions")
        QuickActions(
            onTemplates = onOpenAssemblies,
            onAiQuote = onOpenAiQuoteBuilder,
            onTakeoff = if (isElite) onOpenTakeoff else onOpenSubscription,
            onMoney = onOpenMoney,
        )

        // ── This month ────────────────────────────────────────────────────────
        SectionEyebrow("This month", modifier = Modifier.padding(top = 4.dp))
        if (state.isLoading && snap == null) {
            StatGridSkeleton()
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                StatCard(label = "Spent", value = Format.money(snap?.spent ?: 0.0), icon = Icons.Outlined.TrendingDown, accent = MaterialTheme.colorScheme.secondary, modifier = Modifier.weight(1f))
                StatCard(label = "Real profit", value = Format.money(snap?.realProfit ?: 0.0), icon = Icons.Outlined.TrendingUp, accent = if ((snap?.realProfit ?: 0.0) >= 0) BrandGreen else MaterialTheme.colorScheme.error, modifier = Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                StatCard(label = if (state.jobCount == 1L) "Scheduled job" else "Scheduled jobs", value = state.jobCount?.toString() ?: "\u2014", icon = Icons.Outlined.Work, modifier = Modifier.weight(1f))
                StatCard(label = "Won this month", value = Format.money(snap?.won ?: 0.0), icon = Icons.Outlined.Savings, accent = MaterialTheme.colorScheme.secondary, modifier = Modifier.weight(1f))
            }
        }

        // Non-Elite: one upgrade nudge for AI Takeoff (monetization).
        if (!isElite) {
            SectionEyebrow("Unlock more", modifier = Modifier.padding(top = 4.dp))
            AiHeroCard(
                icon = Icons.Outlined.PhotoCamera,
                title = "AI Takeoff",
                subtitle = "Snap a plan photo or PDF \u2014 AI reads it and builds the estimate. Included with Elite.",
                onClick = onOpenSubscription,
                locked = true,
            )
        }
    }
}

private fun greeting(): String {
    val h = java.time.LocalTime.now().hour
    return when {
        h < 12 -> "Good morning"
        h < 17 -> "Good afternoon"
        else -> "Good evening"
    }
}

/** The core-loop shortcuts row: gradient icon tiles for the money loop. */
@Composable
private fun QuickActions(
    onTemplates: () -> Unit,
    onAiQuote: () -> Unit,
    onTakeoff: () -> Unit,
    onMoney: () -> Unit,
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        QuickAction("Templates", Icons.Outlined.Bolt, Modifier.weight(1f), onTemplates)
        QuickAction("AI quote", Icons.Outlined.AutoAwesome, Modifier.weight(1f), onAiQuote)
        QuickAction("AI takeoff", Icons.Outlined.PhotoCamera, Modifier.weight(1f), onTakeoff)
        QuickAction("Money", Icons.Outlined.Payments, Modifier.weight(1f), onMoney)
    }
}

@Composable
private fun QuickAction(label: String, icon: ImageVector, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(RoundedCornerShape(18.dp))
                .drawBehind {
                    drawRoundRect(
                        brush = BrandGradients.primary,
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(18.dp.toPx(), 18.dp.toPx()),
                        alpha = 0.16f,
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
}

/** Two rows of shimmering stat tiles shown while the month's snapshot loads. */
@Composable
private fun StatGridSkeleton() {
    val cardColor = MaterialTheme.colorScheme.surface
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        repeat(2) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                repeat(2) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(20.dp))
                            .drawBehind { drawRect(cardColor) }
                            .padding(16.dp),
                    ) {
                        ShimmerBox(width = 36.dp, height = 36.dp, shape = RoundedCornerShape(12.dp))
                        Spacer(Modifier.height(12.dp))
                        ShimmerBox(width = 84.dp, height = 20.dp)
                        Spacer(Modifier.height(6.dp))
                        ShimmerBox(width = 56.dp, height = 12.dp)
                    }
                }
            }
        }
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
                // A white translucent placeholder shaped like the hero figure — reads
                // as "loading your money" without a jarring spinner on the gradient.
                Box(
                    modifier = Modifier
                        .size(width = 200.dp, height = 46.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .drawBehind { drawRect(Color.White.copy(alpha = 0.18f)) }
                        .shimmer(
                            shape = RoundedCornerShape(12.dp),
                            baseColor = Color.White.copy(alpha = 0.18f),
                            highlightColor = Color.White.copy(alpha = 0.40f),
                        ),
                )
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
