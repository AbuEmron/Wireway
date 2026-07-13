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
    // Point at the actual static files (public/privacy.html, public/terms.html)
    // so the policy renders directly instead of falling through to the SPA. This
    // resolves on the live site today and stays valid after cleanUrls is enabled
    // (where .html canonically redirects to the clean path), and matches the
    // web footer, which also links the .html pages.
    const val PRIVACY = "$BASE/privacy.html"
    const val TERMS = "$BASE/terms.html"

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
