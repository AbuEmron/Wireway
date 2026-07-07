package com.wirewaypro.app.data.quotes

import com.wirewaypro.app.data.local.QuoteDraftDao
import com.wirewaypro.app.data.local.QuoteDraftEntity
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Persists and restores autosaved quote-builder drafts. Thin wrapper over
 * [QuoteDraftDao] that (de)serializes [QuoteDraft] to JSON, so the ViewModel
 * deals in plain form data and never touches Room directly.
 */
@Singleton
class QuoteDraftStore @Inject constructor(
    private val dao: QuoteDraftDao,
) {
    private val json = Json { ignoreUnknownKeys = true }

    /** Draft key for a builder session: the quote id when editing, else a fresh sentinel. */
    fun keyFor(quoteId: String?, isInvoice: Boolean): String =
        quoteId ?: if (isInvoice) NEW_INVOICE else NEW_ESTIMATE

    suspend fun load(key: String): QuoteDraft? =
        dao.get(key)?.let {
            runCatching { json.decodeFromString(QuoteDraft.serializer(), it.contentJson) }.getOrNull()
        }

    suspend fun save(key: String, draft: QuoteDraft, updatedAt: Long) {
        dao.upsert(
            QuoteDraftEntity(
                draftKey = key,
                contentJson = json.encodeToString(QuoteDraft.serializer(), draft),
                updatedAt = updatedAt,
            ),
        )
    }

    suspend fun clear(key: String) = dao.delete(key)

    private companion object {
        const val NEW_ESTIMATE = "new-estimate"
        const val NEW_INVOICE = "new-invoice"
    }
}
