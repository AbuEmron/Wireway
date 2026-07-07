package com.wirewaypro.app.domain.model

import kotlin.math.ceil

/** One store's price for a material line. */
data class StorePrice(val store: String, val price: Double)

/**
 * What a vendor price is quoted per — the unit the line math multiplies by.
 * Doctrine: totals are computed deterministically from an explicit basis; a price
 * whose basis doesn't match the quantity's unit is NEVER multiplied into a total.
 */
enum class PriceBasis {
    /** Price is for one counted unit (ea, box, roll) and qty counts those units. */
    PER_UNIT,

    /** True cut-by-the-foot price; qty is in feet. */
    PER_FOOT,

    /** Price is for one purchasable package (coil/spool/carton) covering [PullItem.packageSize] of the item's unit. */
    PER_PACKAGE,

    /** Basis couldn't be determined — show the unit price, never a fabricated total. */
    UNKNOWN,
}

/** A single material on the pull list. */
data class PullItem(
    val name: String,
    val spec: String?,
    val qty: Double,
    val unit: String?,
    val price: Double?,        // lowest price found, quoted per [basis]
    val bestStore: String?,    // cheapest store name
    val prices: List<StorePrice>,
    val live: Boolean,         // true when the price came from a live web search
    val basis: PriceBasis = PriceBasis.PER_UNIT,
    val packageSize: Double? = null, // e.g. 25.0 for a 25-ft coil, in the same unit as qty
) {
    /** True when qty is a length (feet) rather than a count of purchasable units. */
    val isLengthQty: Boolean get() = isLengthUnit(unit)

    /**
     * How many times the quoted price gets paid at the register, or null when the
     * basis and the quantity's unit don't line up. The critical guard: a per-coil
     * price is paid once per coil — ceil(feet needed / coil length) — never once
     * per foot. 25 ft of NM-B at $75/25-ft coil is one coil ($75), not $1,875.
     */
    val priceMultiplier: Double? get() = when (basis) {
        PriceBasis.PER_FOOT -> if (isLengthQty) qty else null
        PriceBasis.PER_PACKAGE -> packageSize?.takeIf { it > 0 }?.let { ceil(qty / it) }
        PriceBasis.PER_UNIT -> if (isLengthQty) null else qty
        PriceBasis.UNKNOWN -> null
    }

    /** Null means "confirm at the store" — a missing total is fine, a wrong one is not. */
    val lineTotal: Double? get() {
        val p = price ?: return null
        return priceMultiplier?.let { it * p }
    }

    companion object {
        private val LENGTH_UNITS = setOf("ft", "ft.", "feet", "foot", "lf", "lin ft", "linear ft", "'")

        fun isLengthUnit(unit: String?): Boolean = unit?.trim()?.lowercase() in LENGTH_UNITS
    }
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

    /** Estimated total at each item's cheapest store — unit-consistent lines only. */
    val estTotal: Double get() = allItems.sumOf { it.lineTotal ?: 0.0 }

    /** Lines that have a vendor price but no trustworthy total — confirm in store. */
    val unconfirmedCount: Int get() = allItems.count { it.price != null && it.lineTotal == null }

    /** Savings from buying each item at its cheapest store vs. its priciest, in purchasable units. */
    val savings: Double get() = allItems.sumOf { item ->
        val ps = item.prices.map { it.price }
        val mult = item.priceMultiplier
        if (ps.size >= 2 && mult != null) (ps.max() - ps.min()) * mult else 0.0
    }
}
