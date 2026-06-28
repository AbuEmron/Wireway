package com.wirewaypro.app.data.jobs

import com.wirewaypro.app.domain.model.Job
import com.wirewaypro.app.domain.model.JobDraw
import com.wirewaypro.app.domain.repository.JobRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JobRepositoryImpl @Inject constructor(
    private val client: SupabaseClient,
) : JobRepository {

    override suspend fun getJobs(userId: String): Result<List<Job>> = runCatching {
        client.postgrest.from("jobs")
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
        client.postgrest.from("jobs")
            .select { filter { eq("id", jobId) } }
            .decodeSingleOrNull<JobDto>()
            ?.toDomain()
            ?: error("Job not found.")
    }

    override suspend fun getJobDraws(jobId: String): Result<List<JobDraw>> = runCatching {
        client.postgrest.from("job_draws")
            .select {
                filter { eq("job_id", jobId) }
                order("sort_order", Order.ASCENDING)
            }
            .decodeList<JobDrawDto>()
            .map { it.toDomain() }
    }
}
