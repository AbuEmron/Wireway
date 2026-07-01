package com.wirewaypro.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Thin status strip shown above a list so the contractor always knows whether
 * their data has reached the server:
 *  - offline → a clear "you're offline, changes are saved locally" reassurance;
 *  - online with a backlog → a "syncing N change(s)" note;
 *  - online and fully synced → nothing (renders only when there's something to say).
 *
 * The reassurance matters as much as the warning — the whole point of offline-
 * first is that the user TRUSTS an edit made with no signal isn't lost.
 */
@Composable
fun SyncBanner(isOffline: Boolean, pendingCount: Int, modifier: Modifier = Modifier) {
    if (!isOffline && pendingCount == 0) return

    val bg = if (isOffline) MaterialTheme.colorScheme.secondaryContainer
    else MaterialTheme.colorScheme.primaryContainer
    val fg = if (isOffline) MaterialTheme.colorScheme.onSecondaryContainer
    else MaterialTheme.colorScheme.onPrimaryContainer

    val message = when {
        isOffline && pendingCount > 0 ->
            "Offline — $pendingCount ${changes(pendingCount)} saved here, will sync when you reconnect"
        isOffline -> "You're offline — changes are saved on this device and sync when you reconnect"
        else -> "Syncing $pendingCount ${changes(pendingCount)}…"
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(bg)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = if (isOffline) Icons.Filled.CloudOff else Icons.Filled.Sync,
            contentDescription = null,
            tint = fg,
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = fg,
            modifier = Modifier.padding(start = 10.dp),
        )
    }
}

private fun changes(n: Int): String = if (n == 1) "change" else "changes"
