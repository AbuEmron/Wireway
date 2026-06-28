package com.wirewaypro.app.data.money

import com.wirewaypro.app.domain.model.MoneyMath
import com.wirewaypro.app.domain.model.MoneySnapshot
import com.wirewaypro.app.domain.repository.MoneyRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.YearMonth
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
