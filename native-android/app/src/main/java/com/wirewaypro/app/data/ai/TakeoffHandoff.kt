package com.wirewaypro.app.data.ai

import com.wirewaypro.app.domain.model.QuoteCatalogEntry
import javax.inject.Inject
import javax.inject.Singleton

/**
 * One-shot, in-memory handoff of AI-takeoff catalog entries from the takeoff
 * screen to a freshly-opened quote builder (avoids serializing a list through
 * nav args). Consumed exactly once by the builder on a new quote.
 */
@Singleton
class TakeoffHandoff @Inject constructor() {
    @Volatile
    private var pending: List<QuoteCatalogEntry>? = null

    fun put(entries: List<QuoteCatalogEntry>) {
        pending = entries
    }

    fun take(): List<QuoteCatalogEntry>? {
        val p = pending
        pending = null
        return p
    }
}
