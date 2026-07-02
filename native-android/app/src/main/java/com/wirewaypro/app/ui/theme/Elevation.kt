package com.wirewaypro.app.ui.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Elevation tokens — the shadow ladder for surfaces.
 *
 * The redesign favours a flat, tinted-surface look (the existing cards sit at 1dp),
 * so elevation is used sparingly and semantically rather than as decoration. Naming
 * the rungs keeps every card from inventing its own shadow and keeps dark-mode —
 * where heavy shadows read as muddy halos on a near-black ground — restrained.
 */
object Elevation {
    /** 0dp — flush with the background (list rows on a tinted ground). */
    val none: Dp = 0.dp
    /** 1dp — the default resting card; a whisper of separation. */
    val card: Dp = 1.dp
    /** 3dp — a raised/emphasised card or a selected state. */
    val raised: Dp = 3.dp
    /** 6dp — floating elements: FABs, snackbars, the sync banner. */
    val floating: Dp = 6.dp
    /** 12dp — modal surfaces: dialogs, bottom sheets. */
    val modal: Dp = 12.dp
}
