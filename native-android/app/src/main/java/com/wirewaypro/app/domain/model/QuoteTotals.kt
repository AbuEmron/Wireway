package com.wirewaypro.app.domain.model

import com.wirewaypro.app.domain.catalog.Catalog

/**
 * The computed money breakdown of a quote. Field names mirror the columns the
 * web app stores (total_material, total_labor, …).
 */
data class QuoteTotals(
    val totalMaterial: Double,
    val totalLabor: Double,
    val totalHours: Double,
    val markupAmount: Double,
    val taxAmount: Double,
    val total: Double,
) {
    /**
     * The headline total for the quote's pricing mode:
     *  - [RateMode.FLAT]   → the catalog/itemized [total].
     *  - [RateMode.HOURLY] → a time bid: estimated hours × hourly rate.
     */
    fun headlineTotal(rateMode: RateMode, hourlyRate: Double): Double = when (rateMode) {
        RateMode.FLAT -> total
        RateMode.HOURLY -> totalHours * hourlyRate
    }
}

/**
 * Reproduces the web app's quote math EXACTLY (electrical-estimator.jsx), for both
 * catalog `entries` and `custom_items`:
 *
 *   catalog line (service s, variant v, qty):
 *     mat = s.materialCost · v.m · qty
 *     lab = round(s.laborCost · v.m · qty · (hourlyRate / 85))   // BASE_HOURLY = 85
 *     hrs = s.laborHours · v.m · qty
 *   custom line: mat = materialCost·qty, lab = laborCost·qty, hrs = laborHours·qty
 *
 *   totMat = Σ (clientBuys ? 0 : mat)   totLab = Σ lab   totHrs = Σ hrs
 *   subtotal  = totMat + totLab
 *   markupAmt = subtotal · markup
 *   taxAmt    = taxEnabled ? totMat · taxRate : 0     (tax on MATERIALS only)
 *   total     = subtotal + markupAmt + taxAmt
 *
 * Aggregates are NOT rounded (the web stores raw JS numbers); only per-line labor
 * is rounded to the nearest dollar, exactly as the web does.
 */
object QuoteCalculator {

    private const val BASE_HOURLY = 85.0

    fun compute(
        catalogEntries: List<QuoteCatalogEntry>,
        customItems: List<QuoteCustomItem>,
        markup: Double,
        taxEnabled: Boolean,
        taxRate: Double,
        hourlyRate: Double,
    ): QuoteTotals {
        var totMat = 0.0
        var totLab = 0.0
        var totHrs = 0.0

        for (e in catalogEntries) {
            if (e.qty <= 0.0) continue
            val s = Catalog.service(e.serviceId) ?: continue
            val vm = s.variants.getOrNull(e.variantIdx)?.m ?: 1.0
            val mat = s.materialCost * vm * e.qty
            val lab = Math.round(s.laborCost * vm * e.qty * (hourlyRate / BASE_HOURLY)).toDouble()
            val hrs = s.laborHours * vm * e.qty
            if (!e.clientBuys) totMat += mat
            totLab += lab
            totHrs += hrs
        }

        for (i in customItems) {
            totMat += i.materialCost * i.qty
            totLab += i.laborCost * i.qty
            totHrs += i.laborHours * i.qty
        }

        val subtotal = totMat + totLab
        val markupAmt = subtotal * markup
        val taxAmt = if (taxEnabled) totMat * taxRate else 0.0
        val total = subtotal + markupAmt + taxAmt

        return QuoteTotals(totMat, totLab, totHrs, markupAmt, taxAmt, total)
    }

    /** The single-line money for a catalog entry, for display in detail screens. */
    fun catalogLineAmount(entry: QuoteCatalogEntry, hourlyRate: Double): Double? {
        val s = Catalog.service(entry.serviceId) ?: return null
        val vm = s.variants.getOrNull(entry.variantIdx)?.m ?: 1.0
        val mat = s.materialCost * vm * entry.qty
        val lab = Math.round(s.laborCost * vm * entry.qty * (hourlyRate / BASE_HOURLY)).toDouble()
        return if (entry.clientBuys) lab else mat + lab
    }
}
