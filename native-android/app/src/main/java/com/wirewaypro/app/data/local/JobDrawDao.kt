package com.wirewaypro.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/** Local access to [JobDrawEntity] — mirrors [QuoteDao]. Ordered by sort order. */
@Dao
interface JobDrawDao {

    @Query(
        "SELECT * FROM job_draws WHERE jobId = :jobId AND deleted = 0 " +
            "ORDER BY sortOrder ASC",
    )
    fun observeForJob(jobId: String): Flow<List<JobDrawEntity>>

    @Query(
        "SELECT * FROM job_draws WHERE userId = :userId AND deleted = 0 " +
            "AND status = 'pending' AND dueDate IS NOT NULL AND dueDate <= :onOrBeforeDate " +
            "ORDER BY dueDate ASC",
    )
    suspend fun duePending(userId: String, onOrBeforeDate: String): List<JobDrawEntity>

    @Query("SELECT * FROM job_draws WHERE id = :id AND deleted = 0 LIMIT 1")
    suspend fun getById(id: String): JobDrawEntity?

    @Query("SELECT * FROM job_draws WHERE syncStatus != '${SyncStatus.SYNCED}' ORDER BY updatedAt ASC")
    suspend fun pending(): List<JobDrawEntity>

    @Query("UPDATE job_draws SET syncStatus = '${SyncStatus.SYNCED}', syncAttempts = 0 WHERE id = :id")
    suspend fun markSynced(id: String)

    @Query("UPDATE job_draws SET syncStatus = '${SyncStatus.ERROR}' WHERE id = :id")
    suspend fun markError(id: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(draw: JobDrawEntity)

    @Upsert
    suspend fun upsertAll(draws: List<JobDrawEntity>)

    @Query("DELETE FROM job_draws WHERE id = :id")
    suspend fun hardDelete(id: String)

    @Query("SELECT id FROM job_draws WHERE jobId = :jobId")
    suspend fun allIdsForJob(jobId: String): List<String>
}
