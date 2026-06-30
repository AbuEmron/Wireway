package com.wirewaypro.app.domain.model

/** A logged business-mileage trip (`trips` table). */
data class Trip(
    val id: String,
    val tripDate: String?,
    val miles: Double,
    val purpose: String?,
    val startLoc: String?,
    val endLoc: String?,
    val notes: String?,
    val jobId: String?,
    val billedAt: String?,
    val createdAt: String?,
) {
    /** True once the trip's miles have been pulled onto a quote as a billable line. */
    val isBilled: Boolean get() = !billedAt.isNullOrBlank()
}

/** Editable mileage fields written to `trips`. */
data class TripInput(
    val tripDate: String,
    val miles: Double,
    val purpose: String,
    val startLoc: String? = null,
    val endLoc: String? = null,
    val notes: String? = null,
    val jobId: String? = null,
)

/**
 * IRS standard mileage deduction. Mirrors the web app's configurable per-mile rate;
 * default is the 2025 IRS business rate. Used to show the running tax deduction.
 */
object Mileage {
    const val IRS_RATE_PER_MILE = 0.70

    fun deduction(miles: Double, rate: Double = IRS_RATE_PER_MILE): Double = miles * rate
}
