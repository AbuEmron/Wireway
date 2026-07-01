package com.wirewaypro.app

/**
 * Internal deep-link scheme used by launcher shortcuts (static shortcuts.xml +
 * the dynamic shortcuts in AppShortcuts). A custom `wireway://<host>` URI is
 * resolved by MainActivity's intent-filter, which is variant-agnostic — unlike a
 * hardcoded targetPackage, it works whether the installed applicationId carries
 * the debug `.dev`/`.native` suffix or not.
 */
object Deeplinks {
    const val SCHEME = "wireway"
    const val HOST_NEW_ESTIMATE = "new-estimate"
    const val HOST_SNAP_RECEIPT = "snap-receipt"
    const val HOST_AI_TAKEOFF = "ai-takeoff"

    fun uri(host: String): String = "$SCHEME://$host"
}
