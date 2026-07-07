package com.wirewaypro.app.domain.model

/**
 * Dead-simple "did I make money on this job?" — real collected cash vs the
 * real costs the contractor recorded. Sources:
 *  - [collected]: PAID progress-billing draws, net of retainage.
 *  - [materials]: expenses/receipts tagged to this job.
 *  - [laborHours]/[laborCost]: completed time entries tagged to this job
 *    (hours × the entry's cost rate).
 *
 * Honesty contract: this counts ONLY what was recorded on the job — the card
 * says so instead of pretending to know untracked costs.
 */
data class JobProfitability(
    val collected: Double,
    val materials: Double,
    val laborHours: Double,
    val laborCost: Double,
) {
    val profit: Double get() = MoneyMath.round2(collected - materials - laborCost)

    /** Profit as a fraction of collected; null until money has come in. */
    val margin: Double? get() = if (collected > 0.0) profit / collected else null

    val isEmpty: Boolean
        get() = collected == 0.0 && materials == 0.0 && laborCost == 0.0 && laborHours == 0.0
}
