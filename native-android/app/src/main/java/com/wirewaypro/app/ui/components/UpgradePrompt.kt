package com.wirewaypro.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.wirewaypro.app.domain.model.Tier
import com.wirewaypro.app.ui.theme.BrandGradients

/**
 * A contextual upgrade moment (WIREWAY_PRICING_TIERS.md): one hook line and a
 * compact CTA, rendered inline at the exact point of value — never as an
 * interstitial nag wall. [tier] is the tier being offered (PRO or ELITE).
 *
 * Elite is not publicly purchasable yet (product decision), so an ELITE prompt is
 * rendered as a "coming soon" teaser — same hook/detail, but a status pill instead
 * of a buy CTA and no navigation. Internal Elite testers never see this: the caller
 * only shows the prompt when the user's effective tier is BELOW the required tier,
 * so an Elite-entitled user gets the feature itself, not this card.
 */
@Composable
fun UpgradePrompt(
    hook: String,
    tier: Tier,
    onUpgrade: () -> Unit,
    modifier: Modifier = Modifier,
    detail: String? = null,
) {
    val label = if (tier == Tier.ELITE) "Elite" else "Pro"
    val comingSoon = tier == Tier.ELITE
    val shape = MaterialTheme.shapes.medium
    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, BrandGradients.primary, shape),
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.06f),
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.WorkspacePremium,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    if (comingSoon) hook else "$hook — $label",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                if (comingSoon) {
                    Spacer(Modifier.width(8.dp))
                    ComingSoonBadge()
                }
            }
            if (detail != null) {
                Spacer(Modifier.height(6.dp))
                Text(
                    detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(12.dp))
            val pill = RoundedCornerShape(14.dp)
            if (comingSoon) {
                // Status pill, not a CTA — no tap target, nothing implying it's buyable.
                Text(
                    "Elite · Coming soon",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .align(Alignment.End)
                        .clip(pill)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), pill)
                        .padding(horizontal = 18.dp, vertical = 10.dp),
                )
            } else {
                Text(
                    "See $label",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier
                        .align(Alignment.End)
                        .clip(pill)
                        .background(BrandGradients.primary, pill)
                        .clickable(onClick = onUpgrade)
                        .padding(horizontal = 18.dp, vertical = 10.dp),
                )
            }
        }
    }
}

/** Small "Coming soon" status chip used on Elite teaser prompts. */
@Composable
private fun ComingSoonBadge() {
    val pill = RoundedCornerShape(8.dp)
    Text(
        "COMING SOON",
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .clip(pill)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f), pill)
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}
