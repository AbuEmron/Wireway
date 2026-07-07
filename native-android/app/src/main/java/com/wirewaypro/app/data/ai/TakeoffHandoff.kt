package com.wirewaypro.app.data.ai

import com.wirewaypro.app.domain.catalog.AssemblyCustomItem
import com.wirewaypro.app.domain.model.QuoteCatalogEntry
import javax.inject.Inject
import javax.inject.Singleton

/**
 * One-shot, in-memory handoff of seed line items from the AI takeoff or the
 * template picker to a freshly-opened quote builder (avoids serializing lists
 * through nav args). Consumed exactly once by the builder on a new quote.
 */
@Singleton
class TakeoffHandoff @Inject constructor() {
    @Volatile
    private var pending: List<QuoteCatalogEntry>? = null

    @Volatile
    private var pendingCustom: List<AssemblyCustomItem>? = null

    fun put(entries: List<QuoteCatalogEntry>) {
        pending = entries
    }

    /**
     * Commercial/industrial template lines. The builder prices their labor at
     * the contractor's own hourly rate when it seeds; material stays 0 for the
     * supplier's quote (never a fabricated number).
     */
    fun putCustom(items: List<AssemblyCustomItem>) {
        pendingCustom = items
    }

    fun take(): List<QuoteCatalogEntry>? {
        val p = pending
        pending = null
        return p
    }

    fun takeCustom(): List<AssemblyCustomItem>? {
        val p = pendingCustom
        pendingCustom = null
        return p
    }
}
