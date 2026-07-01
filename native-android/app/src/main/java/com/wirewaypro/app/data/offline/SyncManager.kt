package com.wirewaypro.app.data.offline

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
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val json = Json { ignoreUnknownKeys = true }
    private val flushing = AtomicBoolean(false)

    val pendingCount: Flow<Int> = queue.countFlow

    fun start() {
        network.start()
        scope.launch { network.online.collect { online -> if (online) flush() } }
    }

    suspend fun flush() {
        if (!network.isOnline()) return
        if (!flushing.compareAndSet(false, true)) return
        try {
            for (item in queue.all()) {
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
                    if (attempts >= 5) {
                        // Don't silently drop the user's write: surface it as an
                        // error state they can retry, then stop the retry loop.
                        reflectError(item)
                        queue.remove(item.id)
                    }
                }
            }
        } finally {
            flushing.set(false)
        }
    }

    /**
     * Mirror a queued push's outcome into the Room source of truth. Only quotes
     * are Room-backed today (expenses still live in the queue alone), so this is
     * a no-op for other tables until they migrate.
     */
    private suspend fun reflectSuccess(item: QueuedSave) {
        if (item.table != "quotes") return
        if (item.mode == "delete") quoteDao.hardDelete(item.id) else quoteDao.markSynced(item.id)
    }

    private suspend fun reflectError(item: QueuedSave) {
        if (item.table == "quotes") quoteDao.markError(item.id)
    }
}
