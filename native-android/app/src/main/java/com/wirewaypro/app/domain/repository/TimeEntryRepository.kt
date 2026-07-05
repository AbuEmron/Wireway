package com.wirewaypro.app.domain.repository

import com.wirewaypro.app.domain.model.TimeEntry

interface TimeEntryRepository {
    /** The currently-running timer for this user, if any. */
    suspend fun getRunning(userId: String): Result<TimeEntry?>

    /** Recent time entries (running + completed), newest first. */
    suspend fun getRecent(userId: String, limit: Int = 50): Result<List<TimeEntry>>

    /** All time entries tagged to one job, newest first (job profitability). */
    suspend fun getForJob(userId: String, jobId: String): Result<List<TimeEntry>>

    /**
     * Clocks in: starts a running timer (optionally tied to a job and a crew
     * member). When a crew member is supplied, [workerName]/[rate] are their
     * snapshot at clock-in so the labor cost is defensible later.
     */
    suspend fun start(
        userId: String,
        jobId: String?,
        rate: Double,
        crewMemberId: String? = null,
        workerName: String? = null,
    ): Result<TimeEntry>

    /** Clocks out: stamps clock_out, computes hours from clock_in, clears is_running. */
    suspend fun stop(userId: String, entry: TimeEntry): Result<TimeEntry>

    /**
     * Adds a completed entry manually (hours entered directly). When a crew
     * member is supplied, [workerName]/[rate] are their snapshot so labor cost =
     * hours × [rate] is deterministic and stays correct if the crew later changes.
     */
    suspend fun addManual(
        userId: String,
        jobId: String?,
        hours: Double,
        rate: Double,
        notes: String?,
        crewMemberId: String? = null,
        workerName: String? = null,
    ): Result<TimeEntry>

    suspend fun delete(userId: String, entryId: String): Result<Unit>
}
