package com.wirewaypro.app.data.trips

import com.wirewaypro.app.domain.model.Trip
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Wire shape of a `trips` row (columns per migration_finance.sql + migration_mileage_billing.sql). */
@Serializable
data class TripDto(
    val id: String,
    @SerialName("trip_date") val tripDate: String? = null,
    val miles: Double = 0.0,
    val purpose: String? = null,
    @SerialName("start_loc") val startLoc: String? = null,
    @SerialName("end_loc") val endLoc: String? = null,
    val notes: String? = null,
    @SerialName("job_id") val jobId: String? = null,
    @SerialName("billed_at") val billedAt: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
) {
    fun toDomain(): Trip = Trip(
        id = id,
        tripDate = tripDate,
        miles = miles,
        purpose = purpose,
        startLoc = startLoc,
        endLoc = endLoc,
        notes = notes,
        jobId = jobId,
        billedAt = billedAt,
        createdAt = createdAt,
    )
}
