package com.wirewaypro.app

import android.app.Application
import com.wirewaypro.app.data.offline.SyncManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Application entry point. [HiltAndroidApp] generates the root DI container that
 * every Activity / ViewModel pulls dependencies from.
 */
@HiltAndroidApp
class WirewayApplication : Application() {

    @Inject
    lateinit var syncManager: SyncManager

    override fun onCreate() {
        super.onCreate()
        // Begin watching connectivity and flush any writes queued while offline.
        syncManager.start()
    }
}
