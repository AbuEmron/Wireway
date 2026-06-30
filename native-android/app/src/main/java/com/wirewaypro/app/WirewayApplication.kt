package com.wirewaypro.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.wirewaypro.app.data.offline.SyncManager
import com.wirewaypro.app.notifications.ReminderScheduler
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Application entry point. [HiltAndroidApp] generates the root DI container that
 * every Activity / ViewModel pulls dependencies from. Also provides the
 * [HiltWorkerFactory] so WorkManager can build @HiltWorker workers with their
 * injected dependencies.
 */
@HiltAndroidApp
class WirewayApplication : Application(), Configuration.Provider {

    @Inject lateinit var syncManager: SyncManager
    @Inject lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        // Begin watching connectivity and flush any writes queued while offline.
        syncManager.start()
        // Schedule periodic on-device reminders (overdue invoices, jobs, draws).
        ReminderScheduler.schedule(this)
    }
}
