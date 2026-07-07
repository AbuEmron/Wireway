package com.wirewaypro.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/** Local access to [JobEntity] — mirrors [QuoteDao]. Newest scheduled first. */
@Dao
interface JobDao {

    @Query(
        "SELECT * FROM jobs WHERE userId = :userId AND deleted = 0 " +
            "ORDER BY scheduledDate IS NULL, scheduledDate DESC, createdAt DESC, updatedAt DESC",
    )
    fun observeJobs(userId: String): Flow<List<JobEntity>>

    @Query("SELECT * FROM jobs WHERE id = :id AND deleted = 0 LIMIT 1")
    suspend fun getById(id: String): JobEntity?

    @Query("SELECT * FROM jobs WHERE syncStatus != '${SyncStatus.SYNCED}' ORDER BY updatedAt ASC")
    suspend fun pending(): List<JobEntity>

    @Query("SELECT COUNT(*) FROM jobs WHERE syncStatus != '${SyncStatus.SYNCED}'")
    fun pendingCount(): Flow<Int>

    @Query("UPDATE jobs SET syncStatus = '${SyncStatus.SYNCED}', syncAttempts = 0 WHERE id = :id")
    suspend fun markSynced(id: String)

    @Query("UPDATE jobs SET syncStatus = '${SyncStatus.ERROR}' WHERE id = :id")
    suspend fun markError(id: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(job: JobEntity)

    @Upsert
    suspend fun upsertAll(jobs: List<JobEntity>)

    @Query("DELETE FROM jobs WHERE id = :id")
    suspend fun hardDelete(id: String)

    @Query("SELECT id FROM jobs WHERE userId = :userId")
    suspend fun allIds(userId: String): List<String>
}
