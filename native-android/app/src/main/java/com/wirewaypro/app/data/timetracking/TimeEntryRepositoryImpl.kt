package com.wirewaypro.app.data.timetracking

import com.wirewaypro.app.domain.model.TimeEntry
import com.wirewaypro.app.domain.repository.TimeEntryRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max

@Singleton
class TimeEntryRepositoryImpl @Inject constructor(
    private val client: SupabaseClient,
) : TimeEntryRepository {

    private fun entries() = client.postgrest.from("time_entries")

    override suspend fun getRunning(userId: String): Result<TimeEntry?> = runCatching {
        entries()
            .select { filter { eq("user_id", userId); eq("is_running", true) }; limit(1) }
            .decodeList<TimeEntryDto>()
            .firstOrNull()
            ?.toDomain()
    }

    override suspend fun getRecent(userId: String, limit: Int): Result<List<TimeEntry>> = runCatching {
        entries()
            .select {
                filter { eq("user_id", userId) }
                order("created_at", Order.DESCENDING)
                limit(limit.toLong())
            }
            .decodeList<TimeEntryDto>()
            .map { it.toDomain() }
    }

    override suspend fun getForJob(userId: String, jobId: String): Result<List<TimeEntry>> = runCatching {
        entries()
            .select {
                filter {
                    eq("user_id", userId)
                    eq("job_id", jobId)
                }
                order("created_at", Order.DESCENDING)
            }
            .decodeList<TimeEntryDto>()
            .map { it.toDomain() }
    }

    override suspend fun start(
        userId: String,
        jobId: String?,
        rate: Double,
        crewMemberId: String?,
        workerName: String?,
    ): Result<TimeEntry> = runCatching {
        val payload = buildJsonObject {
            put("id", UUID.randomUUID().toString())
            put("user_id", userId)
            put("job_id", jobId)
            put("clock_in", Instant.now().toString())
            put("rate", rate)
            put("is_running", true)
            put("crew_member_id", crewMemberId)
            put("worker_name", workerName)
        }
        entries().insert(payload) { select() }.decodeSingle<TimeEntryDto>().toDomain()
    }

    override suspend fun stop(userId: String, entry: TimeEntry): Result<TimeEntry> = runCatching {
        val now = Instant.now()
        val start = parseInstant(entry.clockIn) ?: now
        val hours = round2(max(0.0, Duration.between(start, now).toMillis() / 3_600_000.0))
        val payload = buildJsonObject {
            put("clock_out", now.toString())
            put("hours", hours)
            put("is_running", false)
        }
        entries().update(payload) {
            filter { eq("id", entry.id); eq("user_id", userId) }
            select()
        }.decodeSingle<TimeEntryDto>().toDomain()
    }

    override suspend fun addManual(
        userId: String,
        jobId: String?,
        hours: Double,
        rate: Double,
        notes: String?,
        crewMemberId: String?,
        workerName: String?,
    ): Result<TimeEntry> = runCatching {
        val payload = buildJsonObject {
            put("id", UUID.randomUUID().toString())
            put("user_id", userId)
            put("job_id", jobId)
            put("hours", hours)
            put("rate", rate)
            put("is_running", false)
            put("notes", notes)
            put("crew_member_id", crewMemberId)
            put("worker_name", workerName)
        }
        entries().insert(payload) { select() }.decodeSingle<TimeEntryDto>().toDomain()
    }

    override suspend fun delete(userId: String, entryId: String): Result<Unit> = runCatching {
        entries().delete { filter { eq("id", entryId); eq("user_id", userId) } }
    }

    private fun round2(v: Double): Double = Math.round(v * 100.0) / 100.0

    /** Parse a Postgres timestamptz string robustly (handles 'Z', '+00:00', or bare). */
    private fun parseInstant(s: String?): Instant? {
        if (s.isNullOrBlank()) return null
        return runCatching { OffsetDateTime.parse(s).toInstant() }.getOrNull()
            ?: runCatching { Instant.parse(s) }.getOrNull()
            ?: runCatching { LocalDateTime.parse(s).toInstant(ZoneOffset.UTC) }.getOrNull()
    }
}
