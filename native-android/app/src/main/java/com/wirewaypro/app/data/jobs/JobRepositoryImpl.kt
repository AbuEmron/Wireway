package com.wirewaypro.app.data.jobs

import com.wirewaypro.app.domain.model.Job
import com.wirewaypro.app.domain.model.JobDraw
import com.wirewaypro.app.domain.model.JobDrawInput
import com.wirewaypro.app.domain.model.JobInput
import com.wirewaypro.app.domain.repository.JobRepository
import com.wirewaypro.app.domain.util.IsoDate
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JobRepositoryImpl @Inject constructor(
    private val client: SupabaseClient,
) : JobRepository {

    private fun jobs() = client.postgrest.from("jobs")
    private fun draws() = client.postgrest.from("job_draws")

    override suspend fun getJobs(userId: String): Result<List<Job>> = runCatching {
        jobs()
            .select {
                filter { eq("user_id", userId) }
                order("scheduled_date", Order.DESCENDING)
                order("created_at", Order.DESCENDING)
                limit(200)
            }
            .decodeList<JobDto>()
            .map { it.toDomain() }
    }

    override suspend fun getJob(jobId: String): Result<Job> = runCatching {
        jobs()
            .select { filter { eq("id", jobId) } }
            .decodeSingleOrNull<JobDto>()
            ?.toDomain()
            ?: error("Job not found.")
    }

    override suspend fun getJobDraws(jobId: String): Result<List<JobDraw>> = runCatching {
        draws()
            .select {
                filter { eq("job_id", jobId) }
                order("sort_order", Order.ASCENDING)
            }
            .decodeList<JobDrawDto>()
            .map { it.toDomain() }
    }

    override suspend fun getDuePendingDraws(userId: String, onOrBeforeDate: String): Result<List<JobDraw>> =
        runCatching {
            draws()
                .select {
                    filter {
                        eq("user_id", userId)
                        eq("status", "pending")
                        lte("due_date", onOrBeforeDate)
                    }
                    order("due_date", Order.ASCENDING)
                }
                .decodeList<JobDrawDto>()
                .map { it.toDomain() }
        }

    override suspend fun saveJob(userId: String, input: JobInput): Result<Job> = runCatching {
        val payload = buildJsonObject {
            if (input.id == null) put("user_id", userId)
            put("title", input.title)
            put("client_name", input.clientName)
            put("client_phone", input.clientPhone)
            put("client_email", input.clientEmail)
            put("job_address", input.jobAddress)
            put("notes", input.notes)
            put("scheduled_date", IsoDate.normalizeOrNull(input.scheduledDate))
            put("scheduled_time", input.scheduledTime)
            put("duration_hours", input.durationHours)
            put("status", input.status)
            put("total", input.total)
            put("quote_id", input.quoteId)
        }
        val saved = if (input.id == null) {
            jobs().insert(payload) { select() }.decodeSingle<JobDto>()
        } else {
            jobs().update(payload) {
                filter { eq("id", input.id); eq("user_id", userId) }
                select()
            }.decodeSingle<JobDto>()
        }
        saved.toDomain()
    }

    override suspend fun deleteJob(userId: String, jobId: String): Result<Unit> = runCatching {
        jobs().delete { filter { eq("id", jobId); eq("user_id", userId) } }
    }

    override suspend fun saveDraw(userId: String, input: JobDrawInput): Result<JobDraw> = runCatching {
        val payload = buildJsonObject {
            put("user_id", userId)
            put("job_id", input.jobId)
            put("label", input.label)
            put("amount", input.amount)
            put("retainage_pct", input.retainagePct)
            put("status", input.status)
            put("due_date", IsoDate.normalizeOrNull(input.dueDate))
            put("sort_order", input.sortOrder)
        }
        val saved = if (input.id == null) {
            draws().insert(payload) { select() }.decodeSingle<JobDrawDto>()
        } else {
            draws().update(payload) {
                filter { eq("id", input.id); eq("user_id", userId) }
                select()
            }.decodeSingle<JobDrawDto>()
        }
        saved.toDomain()
    }

    override suspend fun deleteDraw(userId: String, drawId: String): Result<Unit> = runCatching {
        draws().delete { filter { eq("id", drawId); eq("user_id", userId) } }
    }

    override suspend fun setDrawStatus(
        userId: String,
        drawId: String,
        status: String,
    ): Result<JobDraw> = runCatching {
        val payload = buildJsonObject {
            put("status", status)
            if (status == "invoiced") put("invoiced_at", Instant.now().toString())
            if (status == "paid") put("paid_at", Instant.now().toString())
        }
        draws().update(payload) {
            filter { eq("id", drawId); eq("user_id", userId) }
            select()
        }.decodeSingle<JobDrawDto>().toDomain()
    }
}
