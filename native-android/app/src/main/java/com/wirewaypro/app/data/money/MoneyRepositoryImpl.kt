package com.wirewaypro.app.data.money

import com.wirewaypro.app.domain.model.AgingBuckets
import com.wirewaypro.app.domain.model.AgingReport
import com.wirewaypro.app.domain.model.JobPnl
import com.wirewaypro.app.domain.model.JobPnlReport
import com.wirewaypro.app.domain.model.MoneyMath
import com.wirewaypro.app.domain.model.MoneySnapshot
import com.wirewaypro.app.domain.model.Receivable
import com.wirewaypro.app.domain.repository.MoneyRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Aggregates the headline money numbers for a month, mirroring dashboard.js
 * getMoneySnapshot. Each sub-query is independently fault-tolerant so an empty or
 * unmigrated optional table (Plaid, time tracking) degrades to 0 rather than
 * failing the whole snapshot.
 */
@Singleton
class MoneyRepositoryImpl @Inject constructor(
    private val client: SupabaseClient,
) : MoneyRepository {

    private val wonStatuses = setOf("accepted", "deposit_paid", "paid", "completed")

    private val irsRates = mapOf(
        2022 to 0.585, 2023 to 0.655, 2024 to 0.67, 2025 to 0.70, 2026 to 0.725,
    )

    private fun irsRate(year: Int): Double = irsRates[year] ?: 0.725

    override suspend fun getSnapshot(userId: String, year: Int, month: Int): Result<MoneySnapshot> = runCatching {
        val start = "%04d-%02d-01".format(year, month)
        val lastDay = YearMonth.of(year, month).lengthOfMonth()
        val end = "%04d-%02d-%02d".format(year, month, lastDay)
        fun inRange(d: String?): Boolean = d != null && d.take(10) >= start && d.take(10) <= end

        // Quotes: bid + won by created date; collected by paid date (fetch all, split locally).
        var bid = 0.0; var won = 0.0; var collected = 0.0
        runCatching {
            client.postgrest.from("quotes")
                .select(io.github.jan.supabase.postgrest.query.Columns.list("total", "status", "created_at", "paid_at")) {
                    filter { eq("user_id", userId) }
                }
                .decodeList<QuoteMoneyDto>()
        }.getOrDefault(emptyList()).forEach { q ->
            if (inRange(q.createdAt)) {
                bid += q.total ?: 0.0
                if (q.status in wonStatuses) won += q.total ?: 0.0
            }
            if (inRange(q.paidAt)) collected += q.total ?: 0.0
        }

        // Collected also includes paid progress draws.
        runCatching {
            client.postgrest.from("job_draws")
                .select(io.github.jan.supabase.postgrest.query.Columns.list("amount", "retainage_pct", "paid_at")) {
                    filter { eq("user_id", userId); eq("status", "paid") }
                }
                .decodeList<DrawMoneyDto>()
        }.getOrDefault(emptyList()).forEach { d ->
            if (inRange(d.paidAt)) {
                val retainage = MoneyMath.round2(d.amount * d.retainagePct / 100.0)
                collected += MoneyMath.round2(d.amount - retainage)
            }
        }

        val expenseSum = sumAmount("expenses", userId, "expense_date", start, end)
        val plaidSum = sumAmount("plaid_transactions", userId, "txn_date", start, end, positiveOnly = true)
        val subsSum = sumAmount("sub_payments", userId, "payment_date", start, end)
        val mileage = runCatching {
            client.postgrest.from("trips")
                .select(io.github.jan.supabase.postgrest.query.Columns.list("miles")) {
                    filter { eq("user_id", userId); gte("trip_date", start); lte("trip_date", end) }
                }
                .decodeList<MilesDto>()
                .sumOf { it.miles }
        }.getOrDefault(0.0) * irsRate(year)
        val labor = runCatching {
            client.postgrest.from("time_entries")
                .select(io.github.jan.supabase.postgrest.query.Columns.list("hours", "rate", "clock_in")) {
                    filter {
                        eq("user_id", userId)
                        eq("is_running", false)
                        gte("clock_in", "${start}T00:00:00")
                        lte("clock_in", "${end}T23:59:59")
                    }
                }
                .decodeList<TimeDto>()
                .sumOf { it.hours * it.rate }
        }.getOrDefault(0.0)

        val materials = expenseSum + plaidSum
        val spent = materials + mileage + subsSum + labor

        MoneySnapshot(
            year = year, month = month,
            bid = bid, won = won, collected = collected,
            spent = spent,
            materials = materials, mileage = mileage, subs = subsSum, labor = labor,
        )
    }

    // ── Accounts receivable (ar.js) ─────────────────────────────────────────────
    override suspend fun getReceivables(userId: String): Result<AgingReport> = runCatching {
        val quotes = decodeOrEmpty {
            client.postgrest.from("quotes")
                .select(Columns.list("id", "quote_number", "client_name", "total", "status", "invoice_mode", "invoice_due_date", "invoice_paid", "created_at")) {
                    filter { eq("user_id", userId) }
                    limit(500)
                }.decodeList<QuoteArDto>()
        }
        val jobs = decodeOrEmpty {
            client.postgrest.from("jobs")
                .select(Columns.list("id", "title", "client_name", "quote_id")) { filter { eq("user_id", userId) } }
                .decodeList<JobArDto>()
        }
        val draws = decodeOrEmpty {
            client.postgrest.from("job_draws")
                .select { filter { eq("user_id", userId); eq("status", "invoiced") } }
                .decodeList<DrawArDto>()
        }
        val jobById = jobs.associateBy { it.id }

        val paidLike = setOf("paid", "completed", "declined", "draft")
        val owed = setOf("sent", "accepted", "deposit_paid", "invoiced")
        val items = mutableListOf<Receivable>()

        for (q in quotes) {
            val isPaid = q.invoicePaid == true || q.status == "paid" || q.status == "completed"
            val receivable = !isPaid && q.status !in paidLike && (q.invoiceMode == true || q.status in owed)
            if (!receivable) continue
            val due = q.invoiceDueDate ?: q.createdAt?.take(10) ?: todayStr()
            items += Receivable(
                id = q.id,
                title = q.quoteNumber?.let { "Invoice $it" } ?: "Invoice",
                client = q.clientName ?: "—",
                amount = q.total ?: 0.0,
                dueDate = due,
                daysOverdue = daysOverdue(due),
            )
        }
        for (d in draws) {
            val job = jobById[d.jobId]
            val due = d.dueDate ?: d.invoicedAt?.take(10) ?: todayStr()
            items += Receivable(
                id = d.id,
                title = "${job?.title ?: "Job"} — ${d.label}",
                client = job?.clientName ?: "—",
                amount = drawNet(d.amount, d.retainagePct),
                dueDate = due,
                daysOverdue = daysOverdue(due),
            )
        }
        items.sortByDescending { it.daysOverdue }
        AgingReport(buckets = aging(items), items = items)
    }

    private fun aging(items: List<Receivable>): AgingBuckets {
        var current = 0.0; var d1 = 0.0; var d31 = 0.0; var d61 = 0.0; var d90 = 0.0; var total = 0.0
        for (it in items) {
            total += it.amount
            when {
                it.daysOverdue <= 0 -> current += it.amount
                it.daysOverdue <= 30 -> d1 += it.amount
                it.daysOverdue <= 60 -> d31 += it.amount
                it.daysOverdue <= 90 -> d61 += it.amount
                else -> d90 += it.amount
            }
        }
        return AgingBuckets(current, d1, d31, d61, d90, total)
    }

    // ── Per-job P&L (jobCosting.js) ─────────────────────────────────────────────
    override suspend fun getJobsPnl(userId: String): Result<JobPnlReport> = runCatching {
        val jobs = decodeOrEmpty {
            client.postgrest.from("jobs").select { filter { eq("user_id", userId) } }.decodeList<JobCostingDto>()
        }
        // [bank, mileage, subs, labor] per job id.
        val agg = HashMap<String, DoubleArray>()
        jobs.forEach { agg[it.id] = DoubleArray(4) }
        fun add(jobId: String?, idx: Int, v: Double) { if (jobId != null) agg[jobId]?.let { it[idx] += v } }

        decodeOrEmpty { client.postgrest.from("expenses").select(Columns.list("amount", "job_id")) { filter { eq("user_id", userId) } }.decodeList<CostAmountDto>() }
            .forEach { add(it.jobId, 0, it.amount) }
        decodeOrEmpty { client.postgrest.from("plaid_transactions").select(Columns.list("amount", "job_id")) { filter { eq("user_id", userId) } }.decodeList<CostAmountDto>() }
            .forEach { add(it.jobId, 0, it.amount) }
        decodeOrEmpty { client.postgrest.from("trips").select(Columns.list("miles", "trip_date", "job_id")) { filter { eq("user_id", userId) } }.decodeList<CostTripDto>() }
            .forEach { add(it.jobId, 1, it.miles * irsRate(yearOf(it.tripDate))) }
        decodeOrEmpty { client.postgrest.from("sub_payments").select(Columns.list("amount", "job_id")) { filter { eq("user_id", userId) } }.decodeList<CostAmountDto>() }
            .forEach { add(it.jobId, 2, it.amount) }
        decodeOrEmpty { client.postgrest.from("time_entries").select(Columns.list("hours", "rate", "job_id")) { filter { eq("user_id", userId); eq("is_running", false) } }.decodeList<CostTimeDto>() }
            .forEach { add(it.jobId, 3, it.hours * it.rate) }

        val pnls = jobs.mapNotNull { j ->
            val a = agg[j.id] ?: DoubleArray(4)
            val spend = a.sum()
            val bid = j.bidAmount ?: 0.0
            val collected = j.collected ?: 0.0
            if (bid <= 0.0 && spend <= 0.0) return@mapNotNull null
            val margin = if (collected > 0) collected - spend else bid - spend
            JobPnl(j.id, j.title ?: "Job", j.clientName, bid, spend, collected, margin, collected > 0)
        }
        JobPnlReport(
            winners = pnls.filter { it.margin >= 0 }.sortedByDescending { it.margin },
            losers = pnls.filter { it.margin < 0 }.sortedBy { it.margin },
        )
    }

    // ── Accountant CSV (dashboard.js buildAccountantCsv) ────────────────────────
    override suspend fun buildAccountantCsv(userId: String, year: Int): Result<String> = runCatching {
        val rows = gatherLedgerRows(userId, year)
        val header = listOf("Date", "Type", "Name", "Category", "Job", "Amount", "Memo")
        (listOf(header) + rows).joinToString("\n") { r -> r.joinToString(",") { csvCell(it) } }
    }

    /**
     * QuickBooks Online "import bank transactions" CSV: Date, Description, Amount
     * (signed — expenses negative, income positive). Derived from the same ledger
     * rows as the accountant CSV.
     */
    override suspend fun buildQuickBooksCsv(userId: String, year: Int): Result<String> = runCatching {
        val rows = gatherLedgerRows(userId, year)
        val header = listOf("Date", "Description", "Amount")
        val qbRows = rows.map { r ->
            // r = [Date, Type, Name, Category, Job, Amount, Memo]
            val description = listOf(r.getOrElse(2) { "" }, r.getOrElse(1) { "" }, r.getOrElse(4) { "" }, r.getOrElse(6) { "" })
                .filter { it.isNotBlank() }
                .joinToString(" — ")
                .ifBlank { "Transaction" }
            listOf(r.getOrElse(0) { "" }, description, r.getOrElse(5) { "0" })
        }
        (listOf(header) + qbRows).joinToString("\n") { r -> r.joinToString(",") { csvCell(it) } }
    }

    /** Gathers every money movement for the year as [Date, Type, Name, Category, Job, Amount, Memo] rows, sorted by date. */
    private suspend fun gatherLedgerRows(userId: String, year: Int): List<List<String>> {
        val ys = "$year-01-01"; val ye = "$year-12-31"
        val rate = irsRate(year)

        val jobName = decodeOrEmpty {
            client.postgrest.from("jobs").select(Columns.list("id", "title")) { filter { eq("user_id", userId) } }.decodeList<JobArDto>()
        }.associate { it.id to (it.title ?: "") }

        val rows = mutableListOf<List<String>>()

        decodeOrEmpty {
            client.postgrest.from("expenses").select { filter { eq("user_id", userId); gte("expense_date", ys); lte("expense_date", ye) } }.decodeList<CsvExpenseDto>()
        }.forEach { rows += listOf(it.expenseDate.orEmpty(), "Expense", it.vendor.orEmpty(), it.category.orEmpty(), jobName[it.jobId].orEmpty(), (-it.amount).toString(), it.description.orEmpty()) }

        decodeOrEmpty {
            client.postgrest.from("plaid_transactions").select { filter { eq("user_id", userId); gt("amount", 0); gte("txn_date", ys); lte("txn_date", ye) } }.decodeList<CsvPlaidDto>()
        }.forEach { rows += listOf(it.txnDate.orEmpty(), "Expense (bank)", it.merchantName ?: it.rawName.orEmpty(), it.userCategory ?: it.mappedCategory.orEmpty(), jobName[it.jobId].orEmpty(), (-it.amount).toString(), "") }

        decodeOrEmpty {
            client.postgrest.from("trips").select { filter { eq("user_id", userId); gte("trip_date", ys); lte("trip_date", ye) } }.decodeList<CsvTripDto>()
        }.forEach { rows += listOf(it.tripDate.orEmpty(), "Mileage", "", "vehicle", jobName[it.jobId].orEmpty(), (-(it.miles * rate)).toString(), "${it.miles} mi @ $rate/mi") }

        decodeOrEmpty {
            client.postgrest.from("sub_payments").select { filter { eq("user_id", userId); gte("payment_date", ys); lte("payment_date", ye) } }.decodeList<CsvSubDto>()
        }.forEach { rows += listOf(it.paymentDate.orEmpty(), "Subcontractor", "", "subcontractors", jobName[it.jobId].orEmpty(), (-it.amount).toString(), it.memo.orEmpty()) }

        decodeOrEmpty {
            client.postgrest.from("quotes").select(Columns.list("quote_number", "client_name", "total", "paid_at")) { filter { eq("user_id", userId); gte("paid_at", "${ys}T00:00:00"); lte("paid_at", "${ye}T23:59:59") } }.decodeList<CsvQuoteDto>()
        }.forEach { rows += listOf(it.paidAt?.take(10).orEmpty(), "Income (invoice)", it.clientName.orEmpty(), "income", "", (it.total ?: 0.0).toString(), it.quoteNumber.orEmpty()) }

        decodeOrEmpty {
            client.postgrest.from("job_draws").select { filter { eq("user_id", userId); eq("status", "paid"); gte("paid_at", "${ys}T00:00:00"); lte("paid_at", "${ye}T23:59:59") } }.decodeList<CsvDrawDto>()
        }.forEach { rows += listOf(it.paidAt?.take(10).orEmpty(), "Income (draw)", "", "income", jobName[it.jobId].orEmpty(), drawNet(it.amount, it.retainagePct).toString(), it.label.orEmpty()) }

        rows.sortBy { it.firstOrNull().orEmpty() }
        return rows
    }

    private fun csvCell(v: String): String =
        if (v.contains(',') || v.contains('"') || v.contains('\n')) "\"${v.replace("\"", "\"\"")}\"" else v

    private fun todayStr(): String = LocalDate.now().toString()

    private fun yearOf(date: String?): Int =
        date?.take(4)?.toIntOrNull() ?: YearMonth.now().year

    private fun drawNet(amount: Double, retainagePct: Double): Double {
        val retainage = MoneyMath.round2(amount * retainagePct / 100.0)
        return MoneyMath.round2(amount - retainage)
    }

    private fun daysOverdue(due: String?): Int {
        if (due.isNullOrBlank()) return 0
        val dueMillis = runCatching {
            LocalDate.parse(due.take(10)).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        }.getOrNull() ?: return 0
        return ((System.currentTimeMillis() - dueMillis) / 86_400_000L).toInt()
    }

    private inline fun <T> decodeOrEmpty(block: () -> List<T>): List<T> =
        runCatching { block() }.getOrDefault(emptyList())

    private suspend fun sumAmount(
        table: String,
        userId: String,
        dateColumn: String,
        start: String,
        end: String,
        positiveOnly: Boolean = false,
    ): Double = runCatching {
        client.postgrest.from(table)
            .select(io.github.jan.supabase.postgrest.query.Columns.list("amount")) {
                filter {
                    eq("user_id", userId)
                    gte(dateColumn, start)
                    lte(dateColumn, end)
                    if (positiveOnly) gt("amount", 0)
                }
            }
            .decodeList<AmountDto>()
            .sumOf { it.amount }
    }.getOrDefault(0.0)
}

@Serializable
private data class QuoteMoneyDto(
    val total: Double? = null,
    val status: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("paid_at") val paidAt: String? = null,
)

@Serializable
private data class DrawMoneyDto(
    val amount: Double = 0.0,
    @SerialName("retainage_pct") val retainagePct: Double = 0.0,
    @SerialName("paid_at") val paidAt: String? = null,
)

@Serializable
private data class AmountDto(val amount: Double = 0.0)

@Serializable
private data class MilesDto(val miles: Double = 0.0)

@Serializable
private data class TimeDto(val hours: Double = 0.0, val rate: Double = 0.0)

// ── AR / P&L / CSV DTOs ─────────────────────────────────────────────────────────
@Serializable
private data class QuoteArDto(
    val id: String,
    @SerialName("quote_number") val quoteNumber: String? = null,
    @SerialName("client_name") val clientName: String? = null,
    val total: Double? = null,
    val status: String? = null,
    @SerialName("invoice_mode") val invoiceMode: Boolean? = null,
    @SerialName("invoice_due_date") val invoiceDueDate: String? = null,
    @SerialName("invoice_paid") val invoicePaid: Boolean? = null,
    @SerialName("created_at") val createdAt: String? = null,
)

@Serializable
private data class JobArDto(
    val id: String,
    val title: String? = null,
    @SerialName("client_name") val clientName: String? = null,
    @SerialName("quote_id") val quoteId: String? = null,
)

@Serializable
private data class DrawArDto(
    val id: String,
    @SerialName("job_id") val jobId: String? = null,
    val label: String = "",
    val amount: Double = 0.0,
    @SerialName("retainage_pct") val retainagePct: Double = 0.0,
    @SerialName("due_date") val dueDate: String? = null,
    @SerialName("invoiced_at") val invoicedAt: String? = null,
)

@Serializable
private data class JobCostingDto(
    val id: String,
    val title: String? = null,
    @SerialName("client_name") val clientName: String? = null,
    @SerialName("bid_amount") val bidAmount: Double? = null,
    val collected: Double? = null,
)

@Serializable
private data class CostAmountDto(val amount: Double = 0.0, @SerialName("job_id") val jobId: String? = null)

@Serializable
private data class CostTripDto(
    val miles: Double = 0.0,
    @SerialName("trip_date") val tripDate: String? = null,
    @SerialName("job_id") val jobId: String? = null,
)

@Serializable
private data class CostTimeDto(
    val hours: Double = 0.0,
    val rate: Double = 0.0,
    @SerialName("job_id") val jobId: String? = null,
)

@Serializable
private data class CsvExpenseDto(
    @SerialName("expense_date") val expenseDate: String? = null,
    val amount: Double = 0.0,
    val category: String? = null,
    val vendor: String? = null,
    val description: String? = null,
    @SerialName("job_id") val jobId: String? = null,
)

@Serializable
private data class CsvPlaidDto(
    @SerialName("txn_date") val txnDate: String? = null,
    val amount: Double = 0.0,
    @SerialName("merchant_name") val merchantName: String? = null,
    @SerialName("raw_name") val rawName: String? = null,
    @SerialName("user_category") val userCategory: String? = null,
    @SerialName("mapped_category") val mappedCategory: String? = null,
    @SerialName("job_id") val jobId: String? = null,
)

@Serializable
private data class CsvTripDto(
    @SerialName("trip_date") val tripDate: String? = null,
    val miles: Double = 0.0,
    @SerialName("job_id") val jobId: String? = null,
)

@Serializable
private data class CsvSubDto(
    @SerialName("payment_date") val paymentDate: String? = null,
    val amount: Double = 0.0,
    val memo: String? = null,
    @SerialName("job_id") val jobId: String? = null,
)

@Serializable
private data class CsvQuoteDto(
    @SerialName("quote_number") val quoteNumber: String? = null,
    @SerialName("client_name") val clientName: String? = null,
    val total: Double? = null,
    @SerialName("paid_at") val paidAt: String? = null,
)

@Serializable
private data class CsvDrawDto(
    val amount: Double = 0.0,
    @SerialName("retainage_pct") val retainagePct: Double = 0.0,
    @SerialName("paid_at") val paidAt: String? = null,
    val label: String = "",
    @SerialName("job_id") val jobId: String? = null,
)
