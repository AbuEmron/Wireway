package com.wirewaypro.app.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Local offline-first copy of a `job_draws` row (a job's progress-billing line).
 * [payloadJson] holds the full JobDrawDto JSON; [jobId]/[userId]/[status]/
 * [dueDate]/[sortOrder] are columns so the per-job list and the "due pending
 * draws" query run without parsing JSON. Sync bookkeeping mirrors [QuoteEntity].
 */
@Entity(
    tableName = "job_draws",
    indices = [Index("jobId"), Index("userId"), Index("syncStatus")],
)
data class JobDrawEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val jobId: String,
    val status: String = "pending",
    val dueDate: String? = null,
    val sortOrder: Int = 0,
    val payloadJson: String,
    val syncStatus: String = SyncStatus.SYNCED,
    val deleted: Boolean = false,
    val updatedAt: Long = 0L,
    val syncAttempts: Int = 0,
)
