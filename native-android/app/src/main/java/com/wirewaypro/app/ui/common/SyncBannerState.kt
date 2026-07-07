package com.wirewaypro.app.ui.common

/** Drives the list-screen [com.wirewaypro.app.ui.components.SyncBanner]. */
data class SyncBannerState(
    val isOffline: Boolean = false,
    val pendingCount: Int = 0,
    /** Writes parked after exhausting auto-retry — surfaced with a manual "Retry". */
    val failedCount: Int = 0,
)
