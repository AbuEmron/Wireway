package com.wirewaypro.app.data.quotes

import com.wirewaypro.app.data.local.QuoteEntity
import com.wirewaypro.app.data.local.SyncStatus
import com.wirewaypro.app.domain.model.QuoteDetail
import com.wirewaypro.app.domain.model.QuoteSummary
import com.wirewaypro.app.domain.model.SyncState
import kotlinx.serialization.json.Json

/**
 * Bridges the Supabase wire shape ([QuoteDto]) and the local [QuoteEntity].
 *
 * [QuoteEntity.payloadJson] holds the full row as JSON so the detail screen and
 * the sync push can both be rebuilt from it without a second network read. On a
 * server refresh we store the DTO's own JSON; on a local write the repository
 * stores the exact upsert body it built (same column names either way, so
 * [toDetail] parses both).
 */
private val entityJson = Json { ignoreUnknownKeys = true }

/** Server row → local entity. Freshly fetched rows are, by definition, [SyncStatus.SYNCED]. */
fun QuoteDto.toEntity(
    userId: String,
    updatedAt: Long,
    syncStatus: String = SyncStatus.SYNCED,
): QuoteEntity = QuoteEntity(
    id = id,
    userId = userId,
    quoteNumber = quoteNumber,
    clientName = clientName,
    jobName = jobName,
    total = total,
    status = status,
    createdAt = createdAt,
    invoiceMode = invoiceMode == true,
    invoiceDueDate = invoiceDueDate,
    invoicePaid = invoicePaid == true,
    paidAt = paidAt,
    payloadJson = entityJson.encodeToString(QuoteDto.serializer(), this),
    syncStatus = syncStatus,
    deleted = false,
    updatedAt = updatedAt,
)

/** Local entity → full detail, by parsing the stored payload through the DTO. */
fun QuoteEntity.toDetail(): QuoteDetail =
    entityJson.decodeFromString(QuoteDto.serializer(), payloadJson).toDetail()

/** Local entity → list-row summary, straight from the indexed columns (no JSON parse). */
fun QuoteEntity.toSummary(): QuoteSummary = QuoteSummary(
    id = id,
    quoteNumber = quoteNumber,
    clientName = clientName,
    jobName = jobName,
    total = total,
    status = status,
    createdAt = createdAt,
    isInvoice = invoiceMode,
    invoiceDueDate = invoiceDueDate,
    invoicePaid = invoicePaid,
    paidAt = paidAt,
    syncState = when (syncStatus) {
        SyncStatus.PENDING -> SyncState.PENDING
        SyncStatus.ERROR -> SyncState.ERROR
        else -> SyncState.SYNCED
    },
)
