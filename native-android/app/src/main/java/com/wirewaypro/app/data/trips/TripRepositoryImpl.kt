package com.wirewaypro.app.data.trips

import com.wirewaypro.app.domain.model.Trip
import com.wirewaypro.app.domain.model.TripInput
import com.wirewaypro.app.domain.repository.TripRepository
import com.wirewaypro.app.domain.util.IsoDate
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TripRepositoryImpl @Inject constructor(
    private val client: SupabaseClient,
) : TripRepository {

    private fun trips() = client.postgrest.from("trips")

    override suspend fun getTrips(userId: String, year: Int): Result<List<Trip>> = runCatching {
        trips()
            .select {
                filter {
                    eq("user_id", userId)
                    gte("trip_date", "$year-01-01")
                    lte("trip_date", "$year-12-31")
                }
                order("trip_date", Order.DESCENDING)
            }
            .decodeList<TripDto>()
            .map { it.toDomain() }
    }

    override suspend fun addTrip(userId: String, input: TripInput): Result<Trip> = runCatching {
        trips().insert(payload(UUID.randomUUID().toString(), userId, input)) { select() }
            .decodeSingle<TripDto>()
            .toDomain()
    }

    override suspend fun deleteTrip(userId: String, tripId: String): Result<Unit> = runCatching {
        trips().delete { filter { eq("id", tripId); eq("user_id", userId) } }
    }

    private fun payload(rowId: String, userId: String, input: TripInput): JsonObject = buildJsonObject {
        put("id", rowId)
        put("user_id", userId)
        // trip_date + purpose are NOT NULL; normalize the date, fall back to today.
        put("trip_date", IsoDate.normalizeOrNull(input.tripDate) ?: LocalDate.now().toString())
        put("miles", input.miles)
        put("purpose", input.purpose)
        put("start_loc", input.startLoc)
        put("end_loc", input.endLoc)
        put("notes", input.notes)
        put("job_id", input.jobId)
    }
}
