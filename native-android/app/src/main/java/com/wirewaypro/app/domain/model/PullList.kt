package com.wirewaypro.app.domain.model

/** One store's price for a material line. */
data class StorePrice(val store: String, val price: Double)

/** A single material on the pull list. */
data class PullItem(
    val name: String,
    val spec: String?,
    val qty: Double,
    val unit: String?,
    val price: Double?,        // lowest price found
    val bestStore: String?,    // cheapest store name
    val prices: List<StorePrice>,
    val live: Boolean,         // true when the price came from a live web search
) {
    val lineTotal: Double get() = (price ?: 0.0) * qty
}

/** Materials for one service line, grouped. */
data class PullSection(val service: String, val items: List<PullItem>)

/**
 * The AI-built material pull list: an itemized shopping list grouped by service,
 * with live big-box pricing and per-store comparison. Mirrors the web app's
 * MaterialsListView shape, generalized to multiple stores.
 */
data class PullListResult(
    val sections: List<PullSection>,
    val notes: String?,
) {
    val allItems: List<PullItem> get() = sections.flatMap { it.items }

    /** Estimated total at each item's cheapest store. */
    val estTotal: Double get() = allItems.sumOf { it.lineTotal }

    /** Savings from buying each item at its cheapest store vs. a single store. */
    val savings: Double get() = allItems.sumOf { item ->
        val ps = item.prices.map { it.price }
        if (ps.size >= 2) (ps.max() - ps.min()) * item.qty else 0.0
    }
}
