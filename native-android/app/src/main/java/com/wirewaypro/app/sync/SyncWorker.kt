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
            // Done once nothing is left that CAN be flushed — parked (failed) writes
            // stay in the queue for a manual retry, so don't spin WorkManager on them.
            if (queue.hasFlushable()) Result.retry() else Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
}
