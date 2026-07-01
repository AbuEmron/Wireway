package com.wirewaypro.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/**
 * Local access to [QuoteEntity]. Reads are [Flow]s so the UI updates the instant
 * a local write or a background sync touches a row. Tombstoned ([deleted]) rows
 * are hidden from list/detail reads but still returned by [pending] so their
 * delete can be pushed.
 *
 * Ordering mirrors the app's "newest first": freshly-created local rows (no
 * server `createdAt` yet) sort to the top, then by server timestamp, then by
 * local edit time as a stable tiebreaker.
 */
@Dao
interface QuoteDao {

    @Query(
        "SELECT * FROM quotes WHERE userId = :userId AND deleted = 0 AND invoiceMode = 0 " +
            "ORDER BY createdAt IS NULL DESC, createdAt DESC, updatedAt DESC",
    )
    fun observeEstimates(userId: String): Flow<List<QuoteEntity>>

    @Query(
        "SELECT * FROM quotes WHERE userId = :userId AND deleted = 0 AND invoiceMode = 1 " +
            "ORDER BY createdAt IS NULL DESC, createdAt DESC, updatedAt DESC",
    )
    fun observeInvoices(userId: String): Flow<List<QuoteEntity>>

    @Query("SELECT * FROM quotes WHERE id = :id AND deleted = 0 LIMIT 1")
    fun observeById(id: String): Flow<QuoteEntity?>

    @Query("SELECT * FROM quotes WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): QuoteEntity?

    /** Rows with local changes to push (edits, creates, and delete tombstones). */
    @Query("SELECT * FROM quotes WHERE syncStatus != '${SyncStatus.SYNCED}' ORDER BY updatedAt ASC")
    suspend fun pending(): List<QuoteEntity>

    @Query("SELECT COUNT(*) FROM quotes WHERE syncStatus != '${SyncStatus.SYNCED}'")
    fun pendingCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(quote: QuoteEntity)

    @Upsert
    suspend fun upsertAll(quotes: List<QuoteEntity>)

    /** Hard-remove a row (after its delete is confirmed on the server). */
    @Query("DELETE FROM quotes WHERE id = :id")
    suspend fun hardDelete(id: String)

    /** Server ids currently known locally for a user (drives refresh reconciliation). */
    @Query("SELECT id FROM quotes WHERE userId = :userId")
    suspend fun allIds(userId: String): List<String>
}
