package com.wirewaypro.app.domain.model

/**
 * True job costing — the Elite layer on top of the Pro-level [JobProfitability]
 * "Did I make money?" card. Lines the ESTIMATE up against the ACTUALS the
 * contractor recorded, split into labor and materials, with variance, so they
 * know where the money actually went before the next bid.
 *
 * Everything here is DETERMINISTIC (no AI, no fabricated numbers):
 *  - [actualLaborCost] = Σ(logged crew hours × that crew member's cost rate).
 *  - [actualMaterialCost] = Σ(expenses/receipts tagged to the job).
 *  - [collected] = paid progress-billing draws, net of retainage.
 *  - Estimates come from the linked quote's labor/material totals (or the job's
 *    snapshot columns). When no estimate is available the estimate side is
 *    [hasEstimate] = false and the UI shows only actuals — never a fake $0.
 *
 * Variance sign convention: variance = actual − estimate.
 *  - variance > 0  → OVER budget (cost more than estimated) — the risk.
 *  - variance < 0  → UNDER budget (came in cheaper) — money kept.
 */
data class JobCosting(
    val estimatedLaborCost: Double,
    val actualLaborCost: Double,
    val estimatedMaterialCost: Double,
    val actualMaterialCost: Double,
    val collected: Double,
    val hasEstimate: Boolean,
) {
    /** Total real cost recorded so far: labor + materials. */
    val actualTotalCost: Double get() = MoneyMath.round2(actualLaborCost + actualMaterialCost)

    /** Total estimated cost: estimated labor + estimated materials. */
    val estimatedTotalCost: Double get() = MoneyMath.round2(estimatedLaborCost + estimatedMaterialCost)

    /** True profit = collected − actual costs. The honest bottom line. */
    val trueProfit: Double get() = MoneyMath.round2(collected - actualTotalCost)

    // ── Variances (actual − estimate; positive = over budget) ────────────────
    val laborVariance: Double get() = MoneyMath.round2(actualLaborCost - estimatedLaborCost)
    val materialVariance: Double get() = MoneyMath.round2(actualMaterialCost - estimatedMaterialCost)
    val totalVariance: Double get() = MoneyMath.round2(actualTotalCost - estimatedTotalCost)

    /** True profit as a fraction of collected; null until money has come in. */
    val margin: Double? get() = if (collected > 0.0) trueProfit / collected else null

    /** Nothing recorded yet on either side — the card shows its "log hours" nudge. */
    val isEmpty: Boolean
        get() = actualLaborCost == 0.0 && actualMaterialCost == 0.0 &&
            collected == 0.0 && estimatedTotalCost == 0.0
}
