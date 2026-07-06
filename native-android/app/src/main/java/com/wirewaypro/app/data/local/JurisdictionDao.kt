package com.wirewaypro.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/**
 * Local access to [JurisdictionEntity] — mirrors [CrewMemberDao]. The user keeps
 * one active jurisdiction, so the "current" queries return the most-recently
 * updated non-deleted row.
 */
@Dao
interface JurisdictionDao {

    /** The user's current jurisdiction as a live stream (null until they pick one). */
    @Query(
        "SELECT * FROM user_jurisdictions WHERE userId = :userId AND deleted = 0 " +
            "ORDER BY updatedAt DESC LIMIT 1",
    )
    fun observeCurrent(userId: String): Flow<JurisdictionEntity?>

    /** One-shot read of the current jurisdiction (same ordering as [observeCurrent]). */
    @Query(
        "SELECT * FROM user_jurisdictions WHERE userId = :userId AND deleted = 0 " +
            "ORDER BY updatedAt DESC LIMIT 1",
    )
    suspend fun getCurrent(userId: String): JurisdictionEntity?

    @Query("SELECT * FROM user_jurisdictions WHERE id = :id AND deleted = 0 LIMIT 1")
    suspend fun getById(id: String): JurisdictionEntity?

    @Query("SELECT * FROM user_jurisdictions WHERE syncStatus != '${SyncStatus.SYNCED}' ORDER BY updatedAt ASC")
    suspend fun pending(): List<JurisdictionEntity>

    @Query("SELECT COUNT(*) FROM user_jurisdictions WHERE syncStatus != '${SyncStatus.SYNCED}'")
    fun pendingCount(): Flow<Int>

    @Query("UPDATE user_jurisdictions SET syncStatus = '${SyncStatus.SYNCED}', syncAttempts = 0 WHERE id = :id")
    suspend fun markSynced(id: String)

    @Query("UPDATE user_jurisdictions SET syncStatus = '${SyncStatus.ERROR}' WHERE id = :id")
    suspend fun markError(id: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(row: JurisdictionEntity)

    @Upsert
    suspend fun upsertAll(rows: List<JurisdictionEntity>)

    @Query("DELETE FROM user_jurisdictions WHERE id = :id")
    suspend fun hardDelete(id: String)

    @Query("SELECT id FROM user_jurisdictions WHERE userId = :userId")
    suspend fun allIds(userId: String): List<String>
}
