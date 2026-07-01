package com.wirewaypro.app.data.expenses

import com.wirewaypro.app.domain.model.Expense
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Wire shape of an `expenses` row (columns per financeApi.js addExpense). */
@Serializable
data class ExpenseDto(
    val id: String,
    @SerialName("expense_date") val expenseDate: String? = null,
    val amount: Double = 0.0,
    val category: String? = null,
    val vendor: String? = null,
    val description: String? = null,
    @SerialName("receipt_url") val receiptUrl: String? = null,
    @SerialName("job_id") val jobId: String? = null,
    val source: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
) {
    fun toDomain(): Expense = Expense(
        id = id,
        expenseDate = expenseDate,
        amount = amount,
        category = category,
        vendor = vendor,
        description = description,
        receiptUrl = receiptUrl,
        jobId = jobId,
        source = source,
        createdAt = createdAt,
    )
}
