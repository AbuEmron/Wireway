package com.wirewaypro.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/** Local access to [ClientEntity] — mirrors [QuoteDao]. Ordered by name. */
@Dao
interface ClientDao {

    @Query(
        "SELECT * FROM clients WHERE userId = :userId AND deleted = 0 " +
            "ORDER BY name COLLATE NOCASE ASC",
    )
    fun observeClients(userId: String): Flow<List<ClientEntity>>

    @Query("SELECT * FROM clients WHERE id = :id AND deleted = 0 LIMIT 1")
    suspend fun getById(id: String): ClientEntity?

    @Query("SELECT * FROM clients WHERE syncStatus != '${SyncStatus.SYNCED}' ORDER BY updatedAt ASC")
    suspend fun pending(): List<ClientEntity>

    @Query("SELECT COUNT(*) FROM clients WHERE syncStatus != '${SyncStatus.SYNCED}'")
    fun pendingCount(): Flow<Int>

    @Query("UPDATE clients SET syncStatus = '${SyncStatus.SYNCED}', syncAttempts = 0 WHERE id = :id")
    suspend fun markSynced(id: String)

    @Query("UPDATE clients SET syncStatus = '${SyncStatus.ERROR}' WHERE id = :id")
    suspend fun markError(id: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(client: ClientEntity)

    @Upsert
    suspend fun upsertAll(clients: List<ClientEntity>)

    @Query("DELETE FROM clients WHERE id = :id")
    suspend fun hardDelete(id: String)

    @Query("SELECT id FROM clients WHERE userId = :userId")
    suspend fun allIds(userId: String): List<String>
}
