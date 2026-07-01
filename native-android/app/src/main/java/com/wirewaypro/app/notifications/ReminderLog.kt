package com.wirewaypro.app.notifications

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

private val Context.reminderDataStore by preferencesDataStore(name = "wireway_reminders")

/**
 * Remembers which reminders have already fired so the periodic worker doesn't
 * re-notify the same invoice/job/draw on every run. Keys embed the date
 * (`type:id:yyyy-MM-dd`); after each run the set is pruned to the given day, so it
 * stays small and an item can re-notify on a later day if still outstanding.
 */
@Singleton
class ReminderLog @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val setKey = stringSetPreferencesKey("notified_keys")

    /** Of [keys], those not yet notified (per the persisted set). */
    suspend fun unseen(keys: List<String>): List<String> {
        val current = context.reminderDataStore.data.first()[setKey] ?: emptySet()
        return keys.filter { it !in current }
    }

    /** Record [keys] as notified, pruning anything not tagged with [keepDate]. */
    suspend fun markSeen(keys: List<String>, keepDate: String) {
        context.reminderDataStore.edit { prefs ->
            val merged = (prefs[setKey] ?: emptySet()) + keys
            prefs[setKey] = merged.filter { it.endsWith(":$keepDate") }.toSet()
        }
    }
}
