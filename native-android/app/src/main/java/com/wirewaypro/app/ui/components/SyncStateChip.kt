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
import com.wirewaypro.app.domain.model.SyncState

/**
 * Small pill telling the contractor whether a record has reached the server.
 * [SyncState.SYNCED] shows nothing — the absence of a chip is the signal that
 * everything's saved, so synced rows stay visually quiet.
 */
@Composable
fun SyncStateChip(state: SyncState, modifier: Modifier = Modifier) {
    val label: String
    val tint: Color
    when (state) {
        SyncState.SYNCED -> return
        SyncState.PENDING -> {
            label = "Pending sync"
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        }
        SyncState.ERROR -> {
            label = "Sync failed"
            tint = MaterialTheme.colorScheme.error
        }
    }
    Text(
        text = label,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = tint,
        modifier = modifier
            .background(tint.copy(alpha = 0.14f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}
