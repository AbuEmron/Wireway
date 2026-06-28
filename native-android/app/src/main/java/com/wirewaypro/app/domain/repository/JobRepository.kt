package com.wirewaypro.app.domain.repository

import com.wirewaypro.app.domain.model.Job
import com.wirewaypro.app.domain.model.JobDraw

interface JobRepository {
    /** The user's jobs, newest scheduled first. */
    suspend fun getJobs(userId: String): Result<List<Job>>

    /** A single job by id (RLS scopes it to the owner). */
    suspend fun getJob(jobId: String): Result<Job>

    /** The job's progress-billing draws, in sort order. */
    suspend fun getJobDraws(jobId: String): Result<List<JobDraw>>
}
