package com.wirewaypro.app.data.offline

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

private val Context.offlineDataStore by preferencesDataStore(name = "wireway_offline_queue")

/**
 * DataStore-backed list of pending writes. Mirrors the web app's localStorage
 * quote queue: replace-by-id on enqueue, drop after too many failed attempts.
 */
@Singleton
class OfflineQueue @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val key = stringPreferencesKey("pending_saves_v1")

    val countFlow: Flow<Int> = context.offlineDataStore.data.map { prefs ->
        decode(prefs[key]).size
    }

    suspend fun all(): List<QueuedSave> = decode(context.offlineDataStore.data.first()[key])

    suspend fun enqueue(save: QueuedSave) = mutate { current ->
        current.filterNot { it.id == save.id } + save
    }

    suspend fun remove(id: String) = mutate { current ->
        current.filterNot { it.id == id }
    }

    suspend fun bumpAttempts(id: String): Int {
        var attempts = 0
        mutate { current ->
            current.map {
                if (it.id == id) it.copy(attempts = it.attempts + 1).also { u -> attempts = u.attempts }
                else it
            }
        }
        return attempts
    }

    private suspend fun mutate(transform: (List<QueuedSave>) -> List<QueuedSave>) {
        context.offlineDataStore.edit { prefs ->
            val updated = transform(decode(prefs[key]))
            prefs[key] = json.encodeToString(updated)
        }
    }

    private fun decode(raw: String?): List<QueuedSave> =
        if (raw.isNullOrBlank()) emptyList()
        else runCatching { json.decodeFromString<List<QueuedSave>>(raw) }.getOrDefault(emptyList())
}
