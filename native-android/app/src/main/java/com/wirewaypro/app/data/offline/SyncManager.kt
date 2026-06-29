package com.wirewaypro.app.data.offline

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
                    val body = json.parseToJsonElement(item.payload) as? JsonObject ?: run {
                        queue.remove(item.id); continue
                    }
                    when (item.mode) {
                        "upsert" -> client.postgrest.from(item.table).upsert(body)
                        else -> client.postgrest.from(item.table).insert(body)
                    }
                    queue.remove(item.id)
                } catch (e: Exception) {
                    if (isConnectivityError(e)) break // still offline — retry later
                    val attempts = queue.bumpAttempts(item.id)
                    if (attempts >= 5) queue.remove(item.id)
                }
            }
        } finally {
            flushing.set(false)
        }
    }
}
