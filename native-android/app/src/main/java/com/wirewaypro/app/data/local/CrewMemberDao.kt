package com.wirewaypro.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/** Local access to [CrewMemberEntity] — mirrors [JobDao]. Active first, then by name. */
@Dao
interface CrewMemberDao {

    @Query(
        "SELECT * FROM crew_members WHERE userId = :userId AND deleted = 0 " +
            "ORDER BY active DESC, name COLLATE NOCASE ASC",
    )
    fun observeCrew(userId: String): Flow<List<CrewMemberEntity>>

    @Query("SELECT * FROM crew_members WHERE id = :id AND deleted = 0 LIMIT 1")
    suspend fun getById(id: String): CrewMemberEntity?

    @Query("SELECT * FROM crew_members WHERE syncStatus != '${SyncStatus.SYNCED}' ORDER BY updatedAt ASC")
    suspend fun pending(): List<CrewMemberEntity>

    @Query("SELECT COUNT(*) FROM crew_members WHERE syncStatus != '${SyncStatus.SYNCED}'")
    fun pendingCount(): Flow<Int>

    @Query("UPDATE crew_members SET syncStatus = '${SyncStatus.SYNCED}', syncAttempts = 0 WHERE id = :id")
    suspend fun markSynced(id: String)

    @Query("UPDATE crew_members SET syncStatus = '${SyncStatus.ERROR}' WHERE id = :id")
    suspend fun markError(id: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(crew: CrewMemberEntity)

    @Upsert
    suspend fun upsertAll(crew: List<CrewMemberEntity>)

    @Query("DELETE FROM crew_members WHERE id = :id")
    suspend fun hardDelete(id: String)

    @Query("SELECT id FROM crew_members WHERE userId = :userId")
    suspend fun allIds(userId: String): List<String>
}
