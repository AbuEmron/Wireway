package com.wirewaypro.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/**
 * Thin status strip above a list so the contractor always knows whether their
 * data reached the server. Priority order, because the failure matters most:
 *  - failed → "N couldn't sync" + a Retry button (writes are parked, never lost);
 *  - offline → reassurance that changes are saved locally and will sync;
 *  - online with a backlog → a "syncing N" note;
 *  - otherwise nothing (a quiet bar means everything's saved).
 *
 * [onRetry] re-arms the parked writes and flushes.
 */
@Composable
fun SyncBanner(
    isOffline: Boolean,
    pendingCount: Int,
    failedCount: Int = 0,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    if (failedCount == 0 && !isOffline && pendingCount == 0) return

    val bg: androidx.compose.ui.graphics.Color
    val fg: androidx.compose.ui.graphics.Color
    val icon: ImageVector
    val message: String
    val showRetry: Boolean

    when {
        failedCount > 0 -> {
            bg = MaterialTheme.colorScheme.errorContainer
            fg = MaterialTheme.colorScheme.onErrorContainer
            icon = Icons.Filled.ErrorOutline
            message = "$failedCount ${changes(failedCount)} couldn't sync — saved on this device"
            showRetry = true
        }
        isOffline -> {
            bg = MaterialTheme.colorScheme.secondaryContainer
            fg = MaterialTheme.colorScheme.onSecondaryContainer
            icon = Icons.Filled.CloudOff
            message = if (pendingCount > 0) {
                "Offline — $pendingCount ${changes(pendingCount)} saved here, will sync when you reconnect"
            } else {
                "You're offline — changes are saved on this device and sync when you reconnect"
            }
            showRetry = false
        }
        else -> {
            bg = MaterialTheme.colorScheme.primaryContainer
            fg = MaterialTheme.colorScheme.onPrimaryContainer
            icon = Icons.Filled.Sync
            message = "Syncing $pendingCount ${changes(pendingCount)}…"
            showRetry = false
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(bg)
            .padding(start = 16.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = fg)
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = fg,
            modifier = Modifier
                .padding(start = 10.dp)
                .weight(1f),
        )
        if (showRetry && onRetry != null) {
            TextButton(onClick = onRetry) { Text("Retry", color = fg) }
        }
    }
}

private fun changes(n: Int): String = if (n == 1) "change" else "changes"
