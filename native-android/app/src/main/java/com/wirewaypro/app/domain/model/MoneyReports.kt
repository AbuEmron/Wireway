package com.wirewaypro.app.domain.model

/** Accounts-receivable aging buckets (mirrors ar.js agingBuckets). */
data class AgingBuckets(
    val current: Double = 0.0,
    val d1_30: Double = 0.0,
    val d31_60: Double = 0.0,
    val d61_90: Double = 0.0,
    val d90: Double = 0.0,
    val total: Double = 0.0,
) {
    val overdue: Double get() = d1_30 + d31_60 + d61_90 + d90
}

/** A single unpaid item (invoice-quote or invoiced draw). */
data class Receivable(
    val id: String,
    val title: String,
    val client: String,
    val amount: Double,
    val dueDate: String?,
    val daysOverdue: Int,
)

data class AgingReport(
    val buckets: AgingBuckets,
    val items: List<Receivable>,
)

/** Per-job profit/loss (mirrors jobCosting.js decorate + dashboard.js winners/losers). */
data class JobPnl(
    val id: String,
    val title: String,
    val clientName: String?,
    val bid: Double,
    val spend: Double,
    val collected: Double,
    val margin: Double,
    val settled: Boolean,
)

data class JobPnlReport(
    val winners: List<JobPnl>,
    val losers: List<JobPnl>,
)
