package com.wirewaypro.app.domain.repository

import com.wirewaypro.app.domain.model.Trip
import com.wirewaypro.app.domain.model.TripInput

interface TripRepository {
    /** The user's mileage trips for a calendar year, newest first. */
    suspend fun getTrips(userId: String, year: Int): Result<List<Trip>>

    /** Logs a trip. */
    suspend fun addTrip(userId: String, input: TripInput): Result<Trip>

    suspend fun deleteTrip(userId: String, tripId: String): Result<Unit>
}
