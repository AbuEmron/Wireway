package com.wirewaypro.app.data.offline

import com.wirewaypro.app.data.local.ClientDao
import com.wirewaypro.app.data.local.CrewMemberDao
import com.wirewaypro.app.data.local.JobDao
import com.wirewaypro.app.data.local.JobDrawDao
import com.wirewaypro.app.data.local.QuoteDao
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/** True for errors that look like a dropped connection (mirrors looksOffline). */
fun isConnectivityError(t: Throwable): Boolean {
    if (t is java.io.IOException) return true
    val m = (t.message ?: "").lowercase()
    return "connect" in m || "network" in m || "host" in m || "timeout" in m || "unreachable" in m
}

/**
 * Flushes the offline queue when connectivity returns and at app start — the
 * native equivalent of the web's `online` event + startup flush. After 5 failed
 * (non-connectivity) attempts a row is dropped so it can't loop forever.
 */
@Singleton
class SyncManager @Inject constructor(
    private val client: SupabaseClient,
    private val queue: OfflineQueue,
    private val network: NetworkMonitor,
    private val quoteDao: QuoteDao,
    private val jobDao: JobDao,
    private val clientDao: ClientDao,
    private val jobDrawDao: JobDrawDao,
    private val crewMemberDao: CrewMemberDao,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val json = Json { ignoreUnknownKeys = true }
    private val flushing = AtomicBoolean(false)

    val pendingCount: Flow<Int> = queue.countFlow

    /** Count of writes parked after exhausting auto-retry, awaiting a manual retry. */
    val failedCount: Flow<Int> = queue.failedCountFlow

    fun start() {
        network.start()
        scope.launch { network.online.collect { online -> if (online) flush() } }
    }

    /** Re-arm every parked write and flush — the manual "retry" behind a failed row. */
    suspend fun retryFailed() {
        queue.retryFailed()
        flush()
    }

    suspend fun flush() {
        if (!network.isOnline()) return
        if (!flushing.compareAndSet(false, true)) return
        try {
            for (item in queue.all()) {
                // Skip writes parked at the retry cap — they wait for a manual retry
                // (which re-arms them) so they can't loop forever on their own.
                if (item.attempts >= OfflineQueue.MAX_ATTEMPTS) continue
                try {
                    when (item.mode) {
                        "delete" -> client.postgrest.from(item.table)
                            .delete { filter { eq("id", item.id); eq("user_id", item.userId) } }
                        else -> {
                            val body = json.parseToJsonElement(item.payload) as? JsonObject
                            if (body == null) {
                                // Unparseable payload — drop it; can't ever succeed.
                                queue.remove(item.id)
                                continue
                            }
                            if (item.mode == "upsert") client.postgrest.from(item.table).upsert(body)
                            else client.postgrest.from(item.table).insert(body)
                        }
                    }
                    queue.remove(item.id)
                    reflectSuccess(item)
                } catch (e: Exception) {
                    if (isConnectivityError(e)) break // still offline — retry later
                    val attempts = queue.bumpAttempts(item.id)
                    if (attempts >= OfflineQueue.MAX_ATTEMPTS) {
                        // Don't silently drop the user's write: flag the row ERROR and
                        // PARK it in the queue (full payload kept) so a manual retry can
                        // replay it exactly. Auto-retry stops here to avoid a loop.
                        reflectError(item)
                    }
                }
            }
        } finally {
            flushing.set(false)
        }
    }

    /**
     * Mirror a queued push's outcome into the Room source of truth, routed by
     * table. Room-backed tables reflect success (mark synced, or hard-delete a
     * pushed tombstone) and terminal failure (mark error, never silently drop).
     * Tables with no local mirror yet (e.g. expenses) fall through as no-ops.
     */
    private suspend fun reflectSuccess(item: QueuedSave) = when (item.table) {
        "quotes" -> if (item.mode == "delete") quoteDao.hardDelete(item.id) else quoteDao.markSynced(item.id)
        "jobs" -> if (item.mode == "delete") jobDao.hardDelete(item.id) else jobDao.markSynced(item.id)
        "clients" -> if (item.mode == "delete") clientDao.hardDelete(item.id) else clientDao.markSynced(item.id)
        "job_draws" -> if (item.mode == "delete") jobDrawDao.hardDelete(item.id) else jobDrawDao.markSynced(item.id)
        "crew_members" -> if (item.mode == "delete") crewMemberDao.hardDelete(item.id) else crewMemberDao.markSynced(item.id)
        else -> Unit
    }

    private suspend fun reflectError(item: QueuedSave) = when (item.table) {
        "quotes" -> quoteDao.markError(item.id)
        "jobs" -> jobDao.markError(item.id)
        "clients" -> clientDao.markError(item.id)
        "job_draws" -> jobDrawDao.markError(item.id)
        "crew_members" -> crewMemberDao.markError(item.id)
        else -> Unit
    }
}
