package com.wirewaypro.app.domain.pricing

/**
 * Resolves the hourly rate a NEW quote should open with — offline, no AI, no
 * network. Priority:
 *   1. the contractor's saved personal rate, if they've set one;
 *   2. otherwise the typical billed rate for their region (from their address);
 *   3. otherwise a national starting point.
 *
 * It is always a starting point the contractor edits — never a fixed or
 * guaranteed price. [Resolved.hint] carries the "approximate, edit me" framing
 * the quote builder shows next to the rate.
 */
object DefaultRate {

    data class Resolved(val rate: Double, val hint: String)

    fun resolve(
        personalRate: Double?,
        regionalTypical: Double,
        national: Double,
    ): Resolved = when {
        personalRate != null && personalRate > 0 ->
            Resolved(personalRate, "Your default rate — edit any time")
        regionalTypical > 0 ->
            Resolved(regionalTypical, "Typical rate for your area — approximate, edit to fit your shop")
        else ->
            Resolved(national, "Starting point — approximate, edit to fit your shop")
    }
}
