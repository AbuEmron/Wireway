package com.wirewaypro.app.data.quotes

import com.wirewaypro.app.data.local.OverrideDao
import com.wirewaypro.app.data.local.OverrideEntity
import com.wirewaypro.app.domain.audit.OverrideAudit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Persists the manual-override audit trail (doctrine: overrides always allowed,
 * always leave a trail). Thin wrapper over [OverrideDao] so ViewModels stay off
 * Room directly.
 */
@Singleton
class OverrideTrail @Inject constructor(
    private val dao: OverrideDao,
) {
    suspend fun record(quoteId: String, overrides: List<OverrideAudit.Override>, atMillis: Long) {
        if (overrides.isEmpty()) return
        dao.insertAll(
            overrides.map {
                OverrideEntity(
                    quoteId = quoteId,
                    field = it.label,
                    original = it.original,
                    overridden = it.overridden,
                    atMillis = atMillis,
                )
            },
        )
    }

    suspend fun forQuote(quoteId: String): List<OverrideEntity> = dao.forQuote(quoteId)

    suspend fun clear(quoteId: String) = dao.deleteFor(quoteId)
}
