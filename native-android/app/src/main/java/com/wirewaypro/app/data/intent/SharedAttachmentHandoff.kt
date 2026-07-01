package com.wirewaypro.app.data.intent

import android.net.Uri
import javax.inject.Inject
import javax.inject.Singleton

/** A file handed to the app via the Android share sheet (ACTION_SEND). */
data class SharedAttachment(val uri: Uri, val isPdf: Boolean)

/**
 * One-shot, in-memory handoff of a shared photo/PDF from [com.wirewaypro.app.MainActivity]
 * to the AI Takeoff screen. Mirrors [com.wirewaypro.app.data.ai.TakeoffHandoff]:
 * the share-sheet grants this process read access to the content Uri, and the
 * takeoff view-model consumes it exactly once on creation.
 */
@Singleton
class SharedAttachmentHandoff @Inject constructor() {

    @Volatile
    private var pending: SharedAttachment? = null

    fun put(attachment: SharedAttachment) {
        pending = attachment
    }

    fun take(): SharedAttachment? {
        val p = pending
        pending = null
        return p
    }
}
