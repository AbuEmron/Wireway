package com.wirewaypro.app.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.wirewaypro.app.data.offline.OfflineQueue
import com.wirewaypro.app.data.offline.SyncManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Flushes the offline write queue in the background. WorkManager runs this under
 * a CONNECTED constraint, so it fires when connectivity returns even if the app
 * is closed. If the queue isn't fully drained (still offline, or a transient
 * server error), it returns [Result.retry] so WorkManager re-runs it with
 * exponential backoff.
 */
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val syncManager: SyncManager,
    private val queue: OfflineQueue,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result =
        try {
            syncManager.flush()
            if (queue.all().isEmpty()) Result.success() else Result.retry()
        } catch (e: Exception) {
            Result.retry()
        }
}
