package com.wirewaypro.app.ui.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.TrendingDown
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material.icons.outlined.Work
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wirewaypro.app.domain.model.MoneySnapshot
import com.wirewaypro.app.domain.model.QuoteSummary
import com.wirewaypro.app.ui.components.AnimatedMoneyText
import com.wirewaypro.app.ui.components.AnimatedNumberText
import com.wirewaypro.app.ui.components.ErrorState
import com.wirewaypro.app.ui.components.ProgressRing
import com.wirewaypro.app.ui.components.SectionEyebrow
import com.wirewaypro.app.ui.components.ShimmerBox
import com.wirewaypro.app.ui.components.StatCard
import com.wirewaypro.app.ui.components.pressScale
import com.wirewaypro.app.ui.components.rememberWirewayHaptics
import com.wirewaypro.app.ui.components.shimmer
import com.wirewaypro.app.ui.theme.BrandGradients
import com.wirewaypro.app.ui.theme.BrandGreen
import com.wirewaypro.app.ui.theme.MotionTokens
import com.wirewaypro.app.ui.util.Format

/**
 * Home tab — the flagship dashboard, matched to the S+ mockup: a personal
 * greeting header, a gradient hero counting up the month's collected money with
 * a live collected-of-won progress ring, animated metric tiles, the quick-action
 * rows, and a "Recent estimates" rail. Sections rise in with a soft stagger.
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
    onOpenTools: () -> Unit,
    onOpenMaterialDb: () -> Unit,
    onOpenLaborCalc: () -> Unit,
    onOpenDrawer: () -> Unit,
    onOpenEstimates: () -> Unit,
    onOpenEstimateDetail: (String) -> Unit,
    onOpenJurisdiction: () -> Unit = {},
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val state by viewModel.home.collectAsStateWithLifecycle()
    val pending by viewModel.pendingSync.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 8.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // ── Greeting header (the mockup's "Good morning, Emron 👋") ───────────
        val name = state.profile?.fullName?.takeIf { it.isNotBlank() }?.substringBefore(" ")
            ?: state.profile?.email?.substringBefore("@")
            ?: "there"
        Row(
            modifier = Modifier.fillMaxWidth().rise(0),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = "${greeting()},",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "$name 👋",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
            val menuFill = MaterialTheme.colorScheme.surfaceVariant
            IconButton(
                onClick = onOpenDrawer,
                modifier = Modifier
                    .clip(CircleShape)
                    .drawBehind { drawCircle(menuFill) },
            ) {
                Icon(
                    Icons.Outlined.Menu,
                    contentDescription = "Open menu",
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
        }

        AnimatedVisibility(visible = pending > 0, enter = fadeIn(), exit = fadeOut()) {
            Text(
                text = "⏳ ${if (pending == 1) "1 change" else "$pending changes"} waiting to sync",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        Box(Modifier.rise(1)) {
            HeroCard(
                isLoading = state.isLoading && state.snapshot == null,
                snapshot = state.snapshot,
                onClick = onOpenMoney,
            )
        }

        // Onboarding nudge — shows only until the user sets their AHJ jurisdiction.
        com.wirewaypro.app.ui.ahj.AhjJurisdictionNudge(onSet = onOpenJurisdiction)

        if (state.error != null && state.profile == null) {
            ErrorState(
                title = "Couldn't load your dashboard",
                message = state.error!!,
                actionLabel = "Try again",
                onAction = viewModel::loadHome,
            )
        }

        val snap = state.snapshot
        // Tier engine: server plan OR Play purchase, so a fresh Play upgrade
        // unlocks Takeoff here before backend entitlement sync lands.
        val isElite = state.tier.atLeast(com.wirewaypro.app.domain.model.Tier.ELITE)

        // ── Quick actions — the core money loop, one tap away ─────────────────
        Column(Modifier.rise(2), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            SectionEyebrow("Quick actions")
            QuickActions(
                onTemplates = onOpenAssemblies,
                onAiQuote = onOpenAiQuoteBuilder,
                onTakeoff = if (isElite) onOpenTakeoff else onOpenSubscription,
                onMoney = onOpenMoney,
            )

            // ── Field tools — the on-the-ladder row, one thumb-tap away ───────
            SectionEyebrow("Field tools")
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                QuickAction("Calculators", Icons.Outlined.Calculate, Modifier.weight(1f), onOpenTools)
                QuickAction("Materials", Icons.Outlined.Category, Modifier.weight(1f), onOpenMaterialDb)
                QuickAction("Labor", Icons.Outlined.Schedule, Modifier.weight(1f), onOpenLaborCalc)
                QuickAction("NEC code", Icons.Outlined.MenuBook, Modifier.weight(1f), onOpenNec)
            }
        }

        // ── This month — animated metric tiles ────────────────────────────────
        Column(Modifier.rise(3), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionEyebrow("This month")
            if (state.isLoading && snap == null) {
                StatGridSkeleton()
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    StatCard(
                        label = "Spent",
                        icon = Icons.Outlined.TrendingDown,
                        accent = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.weight(1f),
                    ) {
                        AnimatedMoneyText(snap?.spent ?: 0.0)
                    }
                    val profit = snap?.realProfit ?: 0.0
                    StatCard(
                        label = "Real profit",
                        icon = Icons.Outlined.TrendingUp,
                        accent = if (profit >= 0) BrandGreen else MaterialTheme.colorScheme.error,
                        modifier = Modifier.weight(1f),
                    ) {
                        AnimatedMoneyText(profit)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    StatCard(
                        label = if (state.jobCount == 1L) "Scheduled job" else "Scheduled jobs",
                        icon = Icons.Outlined.Work,
                        modifier = Modifier.weight(1f),
                    ) {
                        val jobs = state.jobCount
                        if (jobs == null) {
                            Text(
                                "—",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                            )
                        } else {
                            AnimatedNumberText(jobs.toDouble())
                        }
                    }
                    StatCard(
                        label = "Won this month",
                        icon = Icons.Outlined.Savings,
                        accent = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.weight(1f),
                    ) {
                        AnimatedMoneyText(snap?.won ?: 0.0)
                    }
                }
            }
        }

        // ── Recent estimates rail (mockup's "Recent Projects") ────────────────
        if (state.recent.isNotEmpty()) {
            Column(Modifier.rise(4), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SectionEyebrow("Recent estimates", modifier = Modifier.weight(1f))
                    Text(
                        text = "View all",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(onClick = onOpenEstimates)
                            .padding(horizontal = 6.dp, vertical = 4.dp),
                    )
                }
                state.recent.forEach { quote ->
                    RecentEstimateRow(quote = quote, onClick = { onOpenEstimateDetail(quote.id) })
                }
            }
        }

        // Non-Elite: one upgrade nudge for AI Takeoff (monetization).
        if (!isElite) {
            Column(Modifier.rise(5), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionEyebrow("Unlock more")
                AiHeroCard(
                    icon = Icons.Outlined.PhotoCamera,
                    title = "AI Takeoff",
                    subtitle = "Snap a plan photo or PDF — AI reads it and builds the estimate. Included with Elite.",
                    onClick = onOpenSubscription,
                    locked = true,
                )
            }
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

/**
 * Entrance stagger: fades + rises this block once on first show. Pure
 * graphicsLayer, so hidden sections still hold their layout (no reflow pops).
 */
@Composable
private fun Modifier.rise(index: Int): Modifier {
    var on by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { on = true }
    val t by animateFloatAsState(
        targetValue = if (on) 1f else 0f,
        animationSpec = tween(
            durationMillis = 340,
            delayMillis = index * 55,
            easing = MotionTokens.emphasized,
        ),
        label = "rise",
    )
    return graphicsLayer {
        alpha = t
        translationY = (1f - t) * 28f
    }
}

/** One row of the recent-estimates rail: icon chip, title + date, amount. */
@Composable
private fun RecentEstimateRow(quote: QuoteSummary, onClick: () -> Unit) {
    val haptics = rememberWirewayHaptics()
    val interaction = remember { MutableInteractionSource() }
    val title = quote.jobName?.takeIf { it.isNotBlank() }
        ?: quote.clientName?.takeIf { it.isNotBlank() }
        ?: quote.quoteNumber?.let { "#$it" }
        ?: "Untitled"
    val rowFill = MaterialTheme.colorScheme.surface
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pressScale(interaction, pressedScale = 0.98f)
            .clip(MaterialTheme.shapes.medium)
            .drawBehind { drawRect(color = rowFill) }
            .clickable(interactionSource = interaction, indication = null) {
                haptics.tap()
                onClick()
            }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        val accent = MaterialTheme.colorScheme.primary
        Box(
            modifier = Modifier
                .size(40.dp)
                .drawBehind {
                    drawRoundRect(
                        color = accent.copy(alpha = 0.12f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(13.dp.toPx(), 13.dp.toPx()),
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Outlined.Description, contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                Format.date(quote.createdAt),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        quote.total?.let {
            Text(
                Format.money(it),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
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
    val haptics = rememberWirewayHaptics()
    val interaction = remember { MutableInteractionSource() }
    Column(
        modifier = modifier
            .pressScale(interaction, pressedScale = 0.93f)
            .clip(RoundedCornerShape(16.dp))
            .clickable(interactionSource = interaction, indication = null) {
                haptics.tap()
                onClick()
            }
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
 * The gradient hero: an animated blue→purple panel counting up the month's
 * collected money, with a live progress ring showing how much of the won work
 * has been collected. Tapping it opens the Money dashboard.
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
    val won = snapshot?.won ?: 0.0
    val haptics = rememberWirewayHaptics()
    val interaction = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .pressScale(interaction, pressedScale = 0.98f)
            .clip(RoundedCornerShape(26.dp))
            .clickable(interactionSource = interaction, indication = null) {
                haptics.tap()
                onClick()
            }
            .drawBehind { drawRect(brush = BrandGradients.animated(progress)) }
            .padding(22.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
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
                    AnimatedMoneyText(
                        value = collected,
                        style = MaterialTheme.typography.displayMedium,
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
                }
            }
            // Collected-of-won ring — the mockup's progress gauge with real money.
            if (!isLoading && won > 0.0) {
                Spacer(Modifier.size(14.dp))
                val fraction = (collected / won).toFloat().coerceIn(0f, 1f)
                ProgressRing(
                    progress = fraction,
                    size = 76.dp,
                    strokeWidth = 7.dp,
                    tint = Color.White,
                    trackColor = Color.White.copy(alpha = 0.25f),
                ) {
                    Text(
                        text = "${(fraction * 100).toInt()}%",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                }
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
