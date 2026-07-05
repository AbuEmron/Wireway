package com.wirewaypro.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/** Local access to [UserAssemblyEntity] — mirrors [JobDao]. Newest edit first. */
@Dao
interface UserAssemblyDao {

    @Query(
        "SELECT * FROM user_assemblies WHERE userId = :userId AND deleted = 0 " +
            "ORDER BY name COLLATE NOCASE ASC",
    )
    fun observe(userId: String): Flow<List<UserAssemblyEntity>>

    @Query("SELECT * FROM user_assemblies WHERE id = :id AND deleted = 0 LIMIT 1")
    suspend fun getById(id: String): UserAssemblyEntity?

    @Query("SELECT * FROM user_assemblies WHERE syncStatus != '${SyncStatus.SYNCED}' ORDER BY updatedAt ASC")
    suspend fun pending(): List<UserAssemblyEntity>

    @Query("UPDATE user_assemblies SET syncStatus = '${SyncStatus.SYNCED}', syncAttempts = 0 WHERE id = :id")
    suspend fun markSynced(id: String)

    @Query("UPDATE user_assemblies SET syncStatus = '${SyncStatus.ERROR}' WHERE id = :id")
    suspend fun markError(id: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(row: UserAssemblyEntity)

    @Upsert
    suspend fun upsertAll(rows: List<UserAssemblyEntity>)

    @Query("SELECT id FROM user_assemblies WHERE userId = :userId AND deleted = 0")
    suspend fun allIds(userId: String): List<String>
}
