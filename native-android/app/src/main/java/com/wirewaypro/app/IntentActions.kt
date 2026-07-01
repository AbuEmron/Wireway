package com.wirewaypro.app

/**
 * Custom intent actions used by launcher shortcuts (static shortcuts.xml + the
 * dynamic shortcuts registered at launch). MainActivity maps each to a nested
 * navigation route. They target MainActivity explicitly, so the action strings
 * are private to the app and need no intent-filter to be delivered.
 */
object IntentActions {
    const val NEW_ESTIMATE = "com.wirewaypro.app.action.NEW_ESTIMATE"
    const val SNAP_RECEIPT = "com.wirewaypro.app.action.SNAP_RECEIPT"
    const val AI_TAKEOFF = "com.wirewaypro.app.action.AI_TAKEOFF"
}
