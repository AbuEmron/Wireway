package com.wirewaypro.app.data.widget

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** The numbers shown on the home-screen widget. */
data class WidgetSnapshot(
    val collectedThisMonth: Double = 0.0,
    val unpaidInvoices: Int = 0,
    val todayJobs: Int = 0,
    val hasData: Boolean = false,
)

private val Context.widgetDataStore by preferencesDataStore(name = "wireway_widget")

/**
 * Caches the widget's snapshot in DataStore so the Glance widget can render
 * instantly from disk (it runs in a separate, ViewModel-less context). Written by
 * [WidgetUpdater] when the app is foregrounded; read by the widget on each update.
 */
@Singleton
class WidgetSnapshotStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val collectedKey = doublePreferencesKey("collected_this_month")
    private val unpaidKey = intPreferencesKey("unpaid_invoices")
    private val todayJobsKey = intPreferencesKey("today_jobs")
    private val hasDataKey = booleanPreferencesKey("has_data")

    val snapshot: Flow<WidgetSnapshot> = context.widgetDataStore.data.map { p ->
        WidgetSnapshot(
            collectedThisMonth = p[collectedKey] ?: 0.0,
            unpaidInvoices = p[unpaidKey] ?: 0,
            todayJobs = p[todayJobsKey] ?: 0,
            hasData = p[hasDataKey] ?: false,
        )
    }

    suspend fun current(): WidgetSnapshot = snapshot.first()

    suspend fun save(value: WidgetSnapshot) {
        context.widgetDataStore.edit {
            it[collectedKey] = value.collectedThisMonth
            it[unpaidKey] = value.unpaidInvoices
            it[todayJobsKey] = value.todayJobs
            it[hasDataKey] = true
        }
    }
}
