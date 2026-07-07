package com.wirewaypro.app.esign.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.wirewaypro.app.data.local.SyncStatus

/**
 * Local offline-first copy of a signed e-signature record — the durable proof.
 * Same shape/discipline as the other offline entities (crew, jurisdiction):
 * [payloadJson] holds the full [com.wirewaypro.app.esign.data.EsignRecordDto] JSON
 * (the exact Supabase upsert body and the source the domain model rebuilds from),
 * with the evidentiary fields also surfaced as columns for querying/display and so
 * the tamper check can read the hash without parsing JSON. Sync bookkeeping mirrors
 * the other entities ([syncStatus], [deleted] tombstone, [updatedAt] = LWW key).
 *
 * A signed record is IMMUTABLE once sealed — it's written exactly once. It carries
 * the [deleted] column only so it round-trips through the identical sync path as
 * everything else; the repository never edits a sealed record's evidentiary fields.
 *
 * The migration DDL that creates this table lives in [EsignSchema.CREATE_RECORDS]
 * and must stay column-for-column in sync with this entity (Room validates on open).
 */
@Entity(
    tableName = "esign_records",
    indices = [Index("userId"), Index("syncStatus"), Index("quoteId")],
)
data class EsignRecordEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val quoteId: String,
    val documentTitle: String,
    val signerName: String,
    val signerEmail: String? = null,
    val method: String,
    val consentVersion: String,
    val consentGivenAt: Long,
    val signedAt: Long,
    val contentSha256: String,
    val sealedSha256: String,
    val sealedPdfPath: String,
    val encryptedSignaturePath: String? = null,
    val deviceModel: String,
    val appVersion: String,
    val ipAddress: String? = null,
    val payloadJson: String,
    val syncStatus: String = SyncStatus.SYNCED,
    val deleted: Boolean = false,
    val updatedAt: Long = 0L,
    val syncAttempts: Int = 0,
    val createdAt: String? = null,
)

/**
 * One row of the APPEND-ONLY audit trail. Events are never updated or deleted at
 * the app layer (the DAO exposes insert + sync-status flips only) — the log is the
 * evidence. The consent event is recorded here SEPARATELY from the signature event
 * (spec requirement), and every event carries the [consentVersion] in effect so we
 * can always reproduce exactly what disclosures the signer saw.
 *
 * Flat by design: the columns ARE the payload (no JSON blob) so the log stays
 * trivially queryable and the Supabase mapping is a direct 1:1.
 *
 * DDL lives in [EsignSchema.CREATE_AUDIT] — keep column-for-column in sync.
 */
@Entity(
    tableName = "esign_audit_events",
    indices = [Index("userId"), Index("syncStatus"), Index("recordId")],
)
data class EsignAuditEventEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val recordId: String,
    val eventType: String,
    val atMillis: Long,
    val consentVersion: String,
    val detailJson: String? = null,
    val syncStatus: String = SyncStatus.SYNCED,
    val updatedAt: Long = 0L,
    val syncAttempts: Int = 0,
    val createdAt: String? = null,
)
