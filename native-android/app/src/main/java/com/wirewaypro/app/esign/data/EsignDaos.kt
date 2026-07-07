package com.wirewaypro.app.esign.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import com.wirewaypro.app.data.local.SyncStatus

/**
 * Local access to signed [EsignRecordEntity] rows. A record is written once and
 * never edited, so there is no update method for its evidentiary fields — only
 * insert, reads, and sync-status flips (local bookkeeping, not content changes).
 */
@Dao
interface EsignRecordDao {

    @Query("SELECT * FROM esign_records WHERE quoteId = :quoteId AND deleted = 0 ORDER BY signedAt DESC")
    fun observeForQuote(quoteId: String): Flow<List<EsignRecordEntity>>

    @Query("SELECT * FROM esign_records WHERE quoteId = :quoteId AND deleted = 0 ORDER BY signedAt DESC LIMIT 1")
    suspend fun latestForQuote(quoteId: String): EsignRecordEntity?

    @Query("SELECT * FROM esign_records WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): EsignRecordEntity?

    @Query("SELECT * FROM esign_records WHERE syncStatus != '${SyncStatus.SYNCED}' ORDER BY updatedAt ASC")
    suspend fun pending(): List<EsignRecordEntity>

    @Query("SELECT COUNT(*) FROM esign_records WHERE syncStatus != '${SyncStatus.SYNCED}'")
    fun pendingCount(): Flow<Int>

    @Query("UPDATE esign_records SET syncStatus = '${SyncStatus.SYNCED}', syncAttempts = 0 WHERE id = :id")
    suspend fun markSynced(id: String)

    @Query("UPDATE esign_records SET syncStatus = '${SyncStatus.ERROR}', syncAttempts = syncAttempts + 1 WHERE id = :id")
    suspend fun markError(id: String)

    // A sealed record is immutable; insert must never silently replace one. ABORT
    // surfaces an accidental id collision instead of overwriting evidence.
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(record: EsignRecordEntity)

    /** Used only by the pull/refresh path to mirror server rows into the cache. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertFromServer(records: List<EsignRecordEntity>)
}

/**
 * Local access to the APPEND-ONLY [EsignAuditEventEntity] log. Deliberately has NO
 * update-of-content and NO delete — the only mutations are inserts and sync-status
 * flips. That's what makes it a trustworthy trail.
 */
@Dao
interface EsignAuditEventDao {

    @Query("SELECT * FROM esign_audit_events WHERE recordId = :recordId ORDER BY atMillis ASC")
    suspend fun forRecord(recordId: String): List<EsignAuditEventEntity>

    @Query("SELECT * FROM esign_audit_events WHERE recordId = :recordId ORDER BY atMillis ASC")
    fun observeForRecord(recordId: String): Flow<List<EsignAuditEventEntity>>

    @Query("SELECT * FROM esign_audit_events WHERE syncStatus != '${SyncStatus.SYNCED}' ORDER BY atMillis ASC")
    suspend fun pending(): List<EsignAuditEventEntity>

    @Query("UPDATE esign_audit_events SET syncStatus = '${SyncStatus.SYNCED}', syncAttempts = 0 WHERE id = :id")
    suspend fun markSynced(id: String)

    @Query("UPDATE esign_audit_events SET syncStatus = '${SyncStatus.ERROR}', syncAttempts = syncAttempts + 1 WHERE id = :id")
    suspend fun markError(id: String)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(event: EsignAuditEventEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertFromServer(events: List<EsignAuditEventEntity>)
}
