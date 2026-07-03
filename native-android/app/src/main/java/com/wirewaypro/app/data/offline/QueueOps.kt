package com.wirewaypro.app.data.offline

/**
 * Pure transforms over the offline-queue list, extracted from [OfflineQueue] so
 * the resurrection invariant — a delete always displaces any queued save for the
 * same row — is unit-testable without DataStore.
 */
object QueueOps {

    /** Replace-by-id: at most one queued write per row, and the newest wins. */
    fun enqueue(current: List<QueuedSave>, save: QueuedSave): List<QueuedSave> =
        current.filterNot { it.id == save.id } + save

    fun remove(current: List<QueuedSave>, id: String): List<QueuedSave> =
        current.filterNot { it.id == id }
}
