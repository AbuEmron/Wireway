package com.wirewaypro.app.domain.repository

import com.wirewaypro.app.domain.model.Job
import com.wirewaypro.app.domain.model.JobDraw
import com.wirewaypro.app.domain.model.JobDrawInput
import com.wirewaypro.app.domain.model.JobInput
import kotlinx.coroutines.flow.Flow

interface JobRepository {
    /** Live count of jobs with local changes still waiting to sync. */
    fun pendingSyncCount(): Flow<Int>

    /** The user's jobs, newest scheduled first. */
    suspend fun getJobs(userId: String): Result<List<Job>>

    /** A single job by id (RLS scopes it to the owner). */
    suspend fun getJob(jobId: String): Result<Job>

    /** The job's progress-billing draws, in sort order. */
    suspend fun getJobDraws(jobId: String): Result<List<JobDraw>>

    /** Pending draws whose due date is on or before [onOrBeforeDate] (yyyy-MM-dd). */
    suspend fun getDuePendingDraws(userId: String, onOrBeforeDate: String): Result<List<JobDraw>>

    /** Creates (id == null) or updates a job. Returns the saved record. */
    suspend fun saveJob(userId: String, input: JobInput): Result<Job>

    /** Deletes a job (cascades its draws server-side). */
    suspend fun deleteJob(userId: String, jobId: String): Result<Unit>

    /** Creates (id == null) or updates a progress-billing draw. */
    suspend fun saveDraw(userId: String, input: JobDrawInput): Result<JobDraw>

    /** Deletes a draw. */
    suspend fun deleteDraw(userId: String, drawId: String): Result<Unit>

    /** Sets a draw's status, stamping invoiced_at / paid_at like the web app. */
    suspend fun setDrawStatus(userId: String, drawId: String, status: String): Result<JobDraw>
}
