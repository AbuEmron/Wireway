package com.wirewaypro.app.domain.model

/** A business expense row (`expenses` table). */
data class Expense(
    val id: String,
    val expenseDate: String?,
    val amount: Double,
    val category: String?,
    val vendor: String?,
    val description: String?,
    val receiptUrl: String?,
    val jobId: String?,
    val source: String?,
    val createdAt: String?,
)

/** Editable expense fields. Image bytes (if any) are passed separately. */
data class ExpenseInput(
    val expenseDate: String,
    val amount: Double,
    val category: String,
    val vendor: String?,
    val description: String?,
    val jobId: String?,
)

/** Schedule-C expense categories, ported from financeApi.js EXPENSE_CATEGORIES. */
data class ExpenseCategory(val id: String, val label: String)

object ExpenseCategories {
    val ALL: List<ExpenseCategory> = listOf(
        ExpenseCategory("materials", "Materials & Parts"),
        ExpenseCategory("fuel", "Gas & Fuel"),
        ExpenseCategory("tools", "Tools & Equipment"),
        ExpenseCategory("vehicle", "Vehicle (Other)"),
        ExpenseCategory("insurance", "Insurance"),
        ExpenseCategory("advertising", "Advertising"),
        ExpenseCategory("meals", "Meals (50% ded.)"),
        ExpenseCategory("phone", "Phone & Internet"),
        ExpenseCategory("permits", "Licenses & Permits"),
        ExpenseCategory("subcontractors", "Subcontractors"),
        ExpenseCategory("office", "Office Supplies"),
        ExpenseCategory("other", "Other"),
    )

    fun label(id: String?): String =
        ALL.firstOrNull { it.id == id }?.label ?: "Other"
}
