package com.wirewaypro.app.data.expenses

import com.wirewaypro.app.data.offline.NetworkMonitor
import com.wirewaypro.app.data.offline.OfflineQueue
import com.wirewaypro.app.data.offline.QueuedSave
import com.wirewaypro.app.data.offline.isConnectivityError
import com.wirewaypro.app.domain.model.Expense
import com.wirewaypro.app.domain.model.ExpenseInput
import com.wirewaypro.app.domain.repository.ExpenseRepository
import com.wirewaypro.app.domain.util.IsoDate
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.storage.storage
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExpenseRepositoryImpl @Inject constructor(
    private val client: SupabaseClient,
    private val queue: OfflineQueue,
    private val network: NetworkMonitor,
) : ExpenseRepository {

    private fun expenses() = client.postgrest.from("expenses")

    override suspend fun getExpenses(userId: String, year: Int): Result<List<Expense>> = runCatching {
        expenses()
            .select {
                filter {
                    eq("user_id", userId)
                    gte("expense_date", "$year-01-01")
                    lte("expense_date", "$year-12-31")
                }
                order("expense_date", Order.DESCENDING)
            }
            .decodeList<ExpenseDto>()
            .map { it.toDomain() }
    }

    override suspend fun addExpense(
        userId: String,
        input: ExpenseInput,
        imageBytes: ByteArray?,
    ): Result<Expense> = runCatching {
        val rowId = UUID.randomUUID().toString()

        if (network.isOnline()) {
            try {
                var receiptUrl: String? = null
                if (imageBytes != null) {
                    runCatching {
                        val path = "$userId/${Instant.now().toEpochMilli()}-${UUID.randomUUID().toString().take(6)}.jpg"
                        val bucket = client.storage.from("receipts")
                        bucket.upload(path, imageBytes) { upsert = false }
                        receiptUrl = bucket.publicUrl(path)
                    }
                }
                val payload = payload(rowId, userId, input, receiptUrl, hasImage = imageBytes != null)
                return@runCatching expenses().insert(payload) { select() }.decodeSingle<ExpenseDto>().toDomain()
            } catch (e: Exception) {
                if (!isConnectivityError(e)) throw e
            }
        }

        // Offline — queue the expense (without the image; receipt photos aren't queued).
        val payload = payload(rowId, userId, input, receiptUrl = null, hasImage = false)
        queue.enqueue(
            QueuedSave(
                id = rowId,
                table = "expenses",
                mode = "insert",
                payload = payload.toString(),
                userId = userId,
                createdAt = System.currentTimeMillis(),
            )
        )
        Expense(
            id = rowId,
            expenseDate = input.expenseDate,
            amount = input.amount,
            category = input.category,
            vendor = input.vendor,
            description = input.description,
            receiptUrl = null,
            jobId = input.jobId,
            source = "manual",
            createdAt = null,
        )
    }

    override suspend fun deleteExpense(userId: String, expenseId: String): Result<Unit> = runCatching {
        expenses().delete { filter { eq("id", expenseId); eq("user_id", userId) } }
    }

    private fun payload(
        rowId: String,
        userId: String,
        input: ExpenseInput,
        receiptUrl: String?,
        hasImage: Boolean,
    ): JsonObject = buildJsonObject {
        put("id", rowId)
        put("user_id", userId)
        // expense_date is NOT NULL — normalize to valid ISO, falling back to today
        // if the value (e.g. an OCR-extracted date) can't be parsed.
        put("expense_date", IsoDate.normalizeOrNull(input.expenseDate) ?: LocalDate.now().toString())
        put("amount", input.amount)
        put("category", input.category)
        put("vendor", input.vendor)
        put("description", input.description)
        put("receipt_url", receiptUrl)
        put("job_id", input.jobId)
        put("source", if (hasImage) "receipt" else "manual")
    }
}
