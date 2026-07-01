package com.wirewaypro.app.ui.common

/** Drives the list-screen [com.wirewaypro.app.ui.components.SyncBanner]. */
data class SyncBannerState(
    val isOffline: Boolean = false,
    val pendingCount: Int = 0,
)
