package com.wirewaypro.app.shortcuts

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.wirewaypro.app.Deeplinks
import com.wirewaypro.app.MainActivity
import com.wirewaypro.app.R

/**
 * Registers the dynamic launcher shortcuts (long-press the app icon). These
 * complement the static shortcuts in res/xml/shortcuts.xml; both fire the same
 * `wireway://` deep link. Registering dynamically keeps them present even if the
 * static set is ever trimmed and lets us reorder/relabel at runtime later.
 * Build-safe: any failure (e.g. an OEM that caps shortcut count) is swallowed.
 */
object AppShortcuts {

    fun register(context: Context) {
        runCatching {
            val shortcuts = listOf(
                build(
                    context,
                    id = "new_estimate",
                    short = "New estimate",
                    long = "New estimate",
                    icon = R.drawable.ic_shortcut_estimate,
                    host = Deeplinks.HOST_NEW_ESTIMATE,
                ),
                build(
                    context,
                    id = "snap_receipt",
                    short = "Snap receipt",
                    long = "Snap a receipt",
                    icon = R.drawable.ic_shortcut_receipt,
                    host = Deeplinks.HOST_SNAP_RECEIPT,
                ),
                build(
                    context,
                    id = "ai_takeoff",
                    short = "AI Takeoff",
                    long = "AI Takeoff",
                    icon = R.drawable.ic_shortcut_takeoff,
                    host = Deeplinks.HOST_AI_TAKEOFF,
                ),
            )
            ShortcutManagerCompat.setDynamicShortcuts(context, shortcuts)
        }
    }

    private fun build(
        context: Context,
        id: String,
        short: String,
        long: String,
        icon: Int,
        host: String,
    ): ShortcutInfoCompat {
        // Explicit component + a wireway:// deep link: the component makes it target
        // our app regardless of the build-variant applicationId suffix, and the data
        // Uri carries the destination. CLEAR_TOP reuses an already-open task so
        // MainActivity.onNewIntent handles it.
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(Deeplinks.uri(host)), context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        return ShortcutInfoCompat.Builder(context, id)
            .setShortLabel(short)
            .setLongLabel(long)
            .setIcon(IconCompat.createWithResource(context, icon))
            .setIntent(intent)
            .build()
    }
}
