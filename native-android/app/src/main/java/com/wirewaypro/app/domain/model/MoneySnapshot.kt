package com.wirewaypro.app.domain.model

/**
 * This-month money summary, mirroring the headline outputs of the web app's
 * getMoneySnapshot (dashboard.js): money in (collected) vs money out (spent).
 */
data class MoneySnapshot(
    val year: Int,
    val month: Int, // 1-12
    val bid: Double,
    val won: Double,
    val collected: Double,
    val spent: Double,
    val materials: Double,
    val mileage: Double,
    val subs: Double,
    val labor: Double,
) {
    val realProfit: Double get() = collected - spent
}
