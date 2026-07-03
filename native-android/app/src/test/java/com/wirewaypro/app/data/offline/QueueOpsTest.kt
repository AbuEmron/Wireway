package com.wirewaypro.app.data.offline

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Locks the offline-queue invariant behind the delete-draft fix: once a quote is
 * deleted, NO queued save for that row may survive — a stale upsert flushing
 * after the delete is exactly how a "deleted" draft resurrects on sync.
 */
class QueueOpsTest {

    private fun save(id: String, mode: String, at: Long = 0L) = QueuedSave(
        id = id,
        table = "quotes",
        mode = mode,
        payload = if (mode == "delete") "{}" else """{"id":"$id"}""",
        userId = "user-1",
        createdAt = at,
    )

    @Test
    fun `delete displaces a queued upsert for the same row`() {
        // Offline save → offline delete: the queue must hold ONLY the delete,
        // otherwise the flushed upsert re-inserts the quote on the server.
        val afterSave = QueueOps.enqueue(emptyList(), save("q1", "upsert", at = 1))
        val afterDelete = QueueOps.enqueue(afterSave, save("q1", "delete", at = 2))

        assertEquals(1, afterDelete.size)
        assertEquals("delete", afterDelete.single().mode)
        assertEquals("q1", afterDelete.single().id)
    }

    @Test
    fun `remove drops every queued write for the row`() {
        // Online delete path: the server row is gone; any queued save must go too.
        val queue = QueueOps.enqueue(emptyList(), save("q1", "upsert"))
        val other = QueueOps.enqueue(queue, save("q2", "upsert"))

        val afterRemove = QueueOps.remove(other, "q1")

        assertTrue(afterRemove.none { it.id == "q1" })
        assertEquals(listOf("q2"), afterRemove.map { it.id })
    }

    @Test
    fun `remove of an unknown id leaves the queue untouched`() {
        val queue = QueueOps.enqueue(emptyList(), save("q1", "upsert"))
        assertEquals(queue, QueueOps.remove(queue, "not-queued"))
    }

    @Test
    fun `enqueue keeps one entry per row and preserves other rows`() {
        var queue = emptyList<QueuedSave>()
        queue = QueueOps.enqueue(queue, save("q1", "upsert", at = 1))
        queue = QueueOps.enqueue(queue, save("q2", "upsert", at = 2))
        queue = QueueOps.enqueue(queue, save("q1", "upsert", at = 3)) // edit again

        assertEquals(2, queue.size)
        assertEquals(3, queue.single { it.id == "q1" }.createdAt) // newest wins
        assertEquals(2, queue.single { it.id == "q2" }.createdAt)
    }

    @Test
    fun `a save after a delete replaces the delete`() {
        // Re-creating a row with the same id (rare, but the web app's queue does
        // replace-by-id too): last intent wins.
        val afterDelete = QueueOps.enqueue(emptyList(), save("q1", "delete", at = 1))
        val afterSave = QueueOps.enqueue(afterDelete, save("q1", "upsert", at = 2))

        assertEquals(1, afterSave.size)
        assertEquals("upsert", afterSave.single().mode)
    }
}
