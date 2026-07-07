package com.wirewaypro.app.domain.model

/**
 * True job costing — the Elite layer on top of the Pro-level [JobProfitability]
 * "Did I make money?" card. Lines the ESTIMATE up against the ACTUALS the
 * contractor recorded, so they know where the money actually went before the
 * next bid.
 *
 * Everything here is DETERMINISTIC (no AI, no fabricated numbers) and, crucially,
 * compares like with like so every figure is defensible:
 *
 *  - **Labor is compared in HOURS.** The estimate's labor dollars are a BILL rate
 *    (what the client is charged), while actual crew labor is a COST rate (what
 *    the contractor pays) — comparing those dollar figures would be misleading.
 *    So labor variance is estimated hours ([estimatedLaborHours], from the quote's
 *    total_hours) vs actual logged hours ([actualLaborHours]). The actual labor
 *    COST ([actualLaborCost] = Σ hours × crew cost rate) is shown alongside and
 *    feeds true profit.
 *  - **Materials are compared in COST.** [estimatedMaterialCost] (the quote's
 *    material total, a cost) vs [actualMaterialCost] (receipts/expenses on the
 *    job) — both are costs, so the variance is honest.
 *  - **True profit = collected − actual costs** ([collected] − [actualTotalCost]).
 *
 * When no estimate is linked ([hasEstimate] = false) the card shows only actuals
 * and true profit — never a fabricated $0 estimate.
 *
 * Variance sign convention: variance = actual − estimate.
 *  - > 0 → OVER the estimate (more hours / more cost) — the risk.
 *  - < 0 → UNDER the estimate (came in leaner) — money/time kept.
 */
data class JobCosting(
    val estimatedLaborHours: Double,
    val actualLaborHours: Double,
    val actualLaborCost: Double,
    val estimatedMaterialCost: Double,
    val actualMaterialCost: Double,
    val collected: Double,
    val hasEstimate: Boolean,
) {
    /** Total real cost recorded so far: actual labor cost + actual materials. */
    val actualTotalCost: Double get() = MoneyMath.round2(actualLaborCost + actualMaterialCost)

    /** True profit = collected − actual costs. The honest bottom line. */
    val trueProfit: Double get() = MoneyMath.round2(collected - actualTotalCost)

    /** Labor hours over/under the estimate (actual − estimate; + = over). */
    val laborHoursVariance: Double get() = MoneyMath.round2(actualLaborHours - estimatedLaborHours)

    /** Material cost over/under the estimate (actual − estimate; + = over budget). */
    val materialVariance: Double get() = MoneyMath.round2(actualMaterialCost - estimatedMaterialCost)

    /** True profit as a fraction of collected; null until money has come in. */
    val margin: Double? get() = if (collected > 0.0) trueProfit / collected else null

    /** Nothing recorded on either side yet — the card shows its "log hours" nudge. */
    val isEmpty: Boolean
        get() = actualLaborCost == 0.0 && actualMaterialCost == 0.0 &&
            collected == 0.0 && actualLaborHours == 0.0
}
