package com.wirewaypro.app.data.expenses

import com.wirewaypro.app.domain.model.Expense
import com.wirewaypro.app.domain.model.ExpenseInput
import com.wirewaypro.app.domain.repository.ExpenseRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.storage.storage
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExpenseRepositoryImpl @Inject constructor(
    private val client: SupabaseClient,
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
        // Upload the receipt image first (non-fatal — the expense still saves).
        var receiptUrl: String? = null
        if (imageBytes != null) {
            runCatching {
                val path = "$userId/${Instant.now().toEpochMilli()}-${UUID.randomUUID().toString().take(6)}.jpg"
                val bucket = client.storage.from("receipts")
                bucket.upload(path, imageBytes) { upsert = false }
                receiptUrl = bucket.publicUrl(path)
            }
        }

        val payload = buildJsonObject {
            put("user_id", userId)
            put("expense_date", input.expenseDate)
            put("amount", input.amount)
            put("category", input.category)
            put("vendor", input.vendor)
            put("description", input.description)
            put("receipt_url", receiptUrl)
            put("job_id", input.jobId)
            put("source", if (imageBytes != null) "receipt" else "manual")
        }
        expenses().insert(payload) { select() }.decodeSingle<ExpenseDto>().toDomain()
    }

    override suspend fun deleteExpense(userId: String, expenseId: String): Result<Unit> = runCatching {
        expenses().delete { filter { eq("id", expenseId); eq("user_id", userId) } }
    }
}
