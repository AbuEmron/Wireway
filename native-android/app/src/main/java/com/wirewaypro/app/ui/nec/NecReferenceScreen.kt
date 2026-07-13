package com.wirewaypro.app.ui.nec

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wirewaypro.app.domain.model.Tier
import com.wirewaypro.app.domain.nec.NecArticle
import com.wirewaypro.app.domain.nec.NecReference
import com.wirewaypro.app.ui.components.BackTopBar
import com.wirewaypro.app.ui.components.SearchField
import com.wirewaypro.app.ui.components.UpgradePrompt
import com.wirewaypro.app.ui.components.pressScale
import com.wirewaypro.app.ui.components.rememberWirewayHaptics
import com.wirewaypro.app.ui.theme.MotionTokens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NecReferenceScreen(
    onBack: () -> Unit,
    onOpenSubscription: () -> Unit = {},
    viewModel: NecReferenceViewModel = hiltViewModel(),
) {
    var query by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf<String?>("Residential/210") }
    val tier by viewModel.tier.collectAsStateWithLifecycle()
    val isElite = tier?.atLeast(Tier.ELITE) == true
    val results = NecReference.search(query, includeElite = isElite)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { BackTopBar(title = "NEC reference", onBack = onBack) },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
            SearchField(
                value = query,
                onValueChange = { query = it },
                placeholder = "Search articles (GFCI, EV, pool, 210…)",
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            )
            Text(
                if (isElite) {
                    "NEC 2023 reference — residential, commercial, industrial & health care · " +
                        "educational only — confirm against the adopted code + local amendments."
                } else {
                    "NEC 2023 residential reference · educational only — confirm against the adopted code + local amendments."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(10.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Duplicate article numbers exist across sectors (e.g. a residential
                // and a commercial 210), so keys and expansion track sector+number.
                items(results, key = { "${it.sector}/${it.number}" }) { article ->
                    val cardKey = "${article.sector}/${article.number}"
                    ArticleCard(
                        article = article,
                        expanded = expanded == cardKey,
                        onToggle = { expanded = if (expanded == cardKey) null else cardKey },
                        modifier = Modifier.animateItem(),
                    )
                }
                if (!isElite && tier != null) {
                    item(key = "elite-upsell") {
                        UpgradePrompt(
                            hook = "Working commercial, industrial or medical?",
                            detail = "Elite extends the code reference beyond the dwelling: " +
                                "non-dwelling loads (210/220), motors (430), transformers (450), " +
                                "hazardous locations (500–516), IT rooms (645), emergency & standby " +
                                "power (700/701), controls (725), and health care facilities (517).",
                            tier = Tier.ELITE,
                            onUpgrade = onOpenSubscription,
                            modifier = Modifier.padding(top = 6.dp),
                        )
                    }
                }
                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
private fun ArticleCard(
    article: NecArticle,
    expanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = rememberWirewayHaptics()
    val chevron by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = MotionTokens.springBouncy(),
        label = "article-chevron",
    )
    Card(
        modifier = modifier
            .fillMaxWidth()
            .pressScale(pressedScale = 0.985f)
            // Expansion is animated by the inner AnimatedVisibility below; a card
            // animateContentSize on top of it produced a second, competing size
            // animation that flashed an oversized grey (surface) overlay.
            .clickable {
                haptics.tap()
                onToggle()
            },
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (expanded) 3.dp else 1.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Text(
                    "Art. ${article.number}",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    article.title,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 8.dp).weight(1f),
                )
                Icon(
                    Icons.Outlined.KeyboardArrowDown,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.rotate(chevron),
                )
            }
            if (article.sector != "Residential") {
                Text(
                    article.sector.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                article.summary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(animationSpec = MotionTokens.springGentle()) + fadeIn(),
                exit = shrinkVertically(animationSpec = MotionTokens.springGentle()) + fadeOut(),
            ) {
                Column {
                    Spacer(Modifier.height(12.dp))
                    Text("KEY RULES", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(4.dp))
                    article.rules.forEach { Bullet(it) }
                    if (article.violations.isNotEmpty()) {
                        Spacer(Modifier.height(10.dp))
                        Text("COMMON VIOLATIONS", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.height(4.dp))
                        article.violations.forEach { Bullet(it) }
                    }
                }
            }
        }
    }
}

@Composable
private fun Bullet(text: String) {
    Row(Modifier.padding(vertical = 3.dp)) {
        Text("•  ", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}
