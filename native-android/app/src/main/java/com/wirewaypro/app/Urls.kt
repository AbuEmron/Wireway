package com.wirewaypro.app

import android.content.Context
import android.content.Intent
import android.net.Uri

/**
 * Single source of truth for the app's outbound web links (the hosted legal
 * pages). Centralized here so the base domain lives in one place and every
 * surface — Settings and the signed-out login/About area — points at the same
 * URLs.
 */
object Urls {
    const val BASE = "https://www.wireway.cc"
    const val PRIVACY = "$BASE/privacy"
    const val TERMS = "$BASE/terms"

    /**
     * Opens [url] in the user's browser (a Custom Tab if their default browser
     * provides one, otherwise a plain browser window). Best-effort — a device
     * with no browser simply does nothing rather than crashing.
     */
    fun open(context: Context, url: String) {
        runCatching {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }
}
