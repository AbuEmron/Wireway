package com.wirewaypro.app.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.settingsDataStore by preferencesDataStore(name = "wireway_settings")

/** The default hourly rate the catalog/labor math falls back to (matches the web's BASE_HOURLY). */
const val DEFAULT_HOURLY_RATE = 85.0

/**
 * Local device preferences (notification opt-in + baseline pricing rates). Not
 * synced to Supabase. The baseline rates are the contractor's personal starting
 * point — they prefill new quotes and seed the AI pricing recommendation.
 */
@Singleton
class SettingsPrefs @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val notificationsKey = booleanPreferencesKey("notifications_enabled")
    private val hourlyRateKey = doublePreferencesKey("default_hourly_rate")
    private val flatRateKey = doublePreferencesKey("default_flat_rate")
    private val themeModeKey = stringPreferencesKey("theme_mode")

    val notificationsEnabled: Flow<Boolean> =
        context.settingsDataStore.data.map { it[notificationsKey] ?: true }

    /** Light/dark preference as a [ThemeMode] name; defaults to SYSTEM. */
    val themeMode: Flow<String> =
        context.settingsDataStore.data.map { it[themeModeKey] ?: "SYSTEM" }

    /** Contractor's standard hourly labor rate. Falls back to [DEFAULT_HOURLY_RATE]. */
    val defaultHourlyRate: Flow<Double> =
        context.settingsDataStore.data.map { it[hourlyRateKey] ?: DEFAULT_HOURLY_RATE }

    /** Contractor's typical flat-rate baseline (e.g. a service-call minimum). 0 = unset. */
    val defaultFlatRate: Flow<Double> =
        context.settingsDataStore.data.map { it[flatRateKey] ?: 0.0 }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[notificationsKey] = enabled }
    }

    suspend fun setDefaultHourlyRate(rate: Double) {
        context.settingsDataStore.edit { it[hourlyRateKey] = rate }
    }

    suspend fun setDefaultFlatRate(rate: Double) {
        context.settingsDataStore.edit { it[flatRateKey] = rate }
    }

    /** Persist the light/dark preference (store the [ThemeMode] enum name). */
    suspend fun setThemeMode(mode: String) {
        context.settingsDataStore.edit { it[themeModeKey] = mode }
    }
}
