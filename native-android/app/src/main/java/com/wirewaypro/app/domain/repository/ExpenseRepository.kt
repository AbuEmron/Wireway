package com.wirewaypro.app.domain.repository

import com.wirewaypro.app.domain.model.Expense
import com.wirewaypro.app.domain.model.ExpenseInput

interface ExpenseRepository {
    /** The user's expenses for a calendar year, newest first. */
    suspend fun getExpenses(userId: String, year: Int): Result<List<Expense>>

    /**
     * Creates an expense. If [imageBytes] is non-null it is uploaded to the
     * shared `receipts` storage bucket first and linked as the receipt image.
     */
    suspend fun addExpense(userId: String, input: ExpenseInput, imageBytes: ByteArray?): Result<Expense>

    suspend fun deleteExpense(userId: String, expenseId: String): Result<Unit>
}
