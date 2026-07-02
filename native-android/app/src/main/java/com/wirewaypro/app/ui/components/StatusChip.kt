package com.wirewaypro.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.wirewaypro.app.domain.model.QuoteExpiry
import com.wirewaypro.app.ui.theme.BrandAmber
import com.wirewaypro.app.ui.theme.BrandGreen
import com.wirewaypro.app.ui.util.Format

/**
 * A small pill showing a record's status, tinted by meaning. Unknown statuses
 * fall back to a neutral steel tint.
 */
@Composable
fun StatusChip(status: String?, modifier: Modifier = Modifier) {
    val key = status?.lowercase()?.trim()
    val tint: Color = when (key) {
        "paid", "accepted", "complete", "completed", "active" -> BrandGreen
        "sent", "invoiced", "scheduled", "in_progress" -> MaterialTheme.colorScheme.primary
        "cancelled", "canceled", "declined", "overdue" -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Text(
        text = Format.status(status),
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = tint,
        modifier = modifier
            .background(tint.copy(alpha = 0.14f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}

/**
 * A pill flagging an estimate that's expiring soon (amber) or already expired
 * (red), prompting a follow-up before the bid goes cold.
 */
@Composable
fun ExpiryChip(status: QuoteExpiry.Status, modifier: Modifier = Modifier) {
    val tint: Color = if (status.level == QuoteExpiry.Level.EXPIRED) {
        MaterialTheme.colorScheme.error
    } else {
        BrandAmber
    }
    Text(
        text = status.label,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = tint,
        modifier = modifier
            .background(tint.copy(alpha = 0.14f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}
