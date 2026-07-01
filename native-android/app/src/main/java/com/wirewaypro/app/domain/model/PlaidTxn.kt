package com.wirewaypro.app.domain.model

/** A synced bank transaction row (`plaid_transactions`). */
data class PlaidTxn(
    val id: String,
    val date: String?,
    val name: String,
    val category: String?,
    val amount: Double,
)
