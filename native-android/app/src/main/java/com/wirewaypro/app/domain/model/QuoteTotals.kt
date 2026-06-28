package com.wirewaypro.app.domain.model

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
)

/**
 * Reproduces the web app's quote math EXACTLY (electrical-estimator.jsx):
 *
 *   per custom item:  mat = materialCost·qty, lab = laborCost·qty, hrs = laborHours·qty
 *   totMat = Σ mat   totLab = Σ lab   totHrs = Σ hrs   (plus carried catalog `entries`)
 *   subtotal  = totMat + totLab
 *   markupAmt = subtotal · markup
 *   taxAmt    = taxEnabled ? totMat · taxRate : 0     (tax is on MATERIALS only)
 *   total     = subtotal + markupAmt + taxAmt
 *
 * [carried] is the contribution of any catalog `entries` the native builder does
 * not edit (material/labor/hours), so editing custom items never drops them.
 */
object QuoteCalculator {

    data class Carried(
        val material: Double = 0.0,
        val labor: Double = 0.0,
        val hours: Double = 0.0,
    )

    fun compute(
        customItems: List<QuoteCustomItem>,
        markup: Double,
        taxEnabled: Boolean,
        taxRate: Double,
        carried: Carried = Carried(),
    ): QuoteTotals {
        val customMat = customItems.sumOf { it.materialCost * it.qty }
        val customLab = customItems.sumOf { it.laborCost * it.qty }
        val customHrs = customItems.sumOf { it.laborHours * it.qty }

        val totMat = MoneyMath.round2(carried.material + customMat)
        val totLab = MoneyMath.round2(carried.labor + customLab)
        val totHrs = MoneyMath.round2(carried.hours + customHrs)

        val subtotal = totMat + totLab
        val markupAmt = MoneyMath.round2(subtotal * markup)
        val taxAmt = MoneyMath.round2(if (taxEnabled) totMat * taxRate else 0.0)
        val total = MoneyMath.round2(subtotal + markupAmt + taxAmt)

        return QuoteTotals(totMat, totLab, totHrs, markupAmt, taxAmt, total)
    }
}
