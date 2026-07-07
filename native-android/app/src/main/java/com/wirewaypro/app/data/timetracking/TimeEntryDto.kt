package com.wirewaypro.app.data.timetracking

import com.wirewaypro.app.domain.model.TimeEntry
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Wire shape of a `time_entries` row (columns per migration_timetracking.sql). */
@Serializable
data class TimeEntryDto(
    val id: String,
    @SerialName("job_id") val jobId: String? = null,
    @SerialName("worker_name") val workerName: String? = null,
    @SerialName("clock_in") val clockIn: String? = null,
    @SerialName("clock_out") val clockOut: String? = null,
    val hours: Double? = null,
    val rate: Double = 0.0,
    @SerialName("is_running") val isRunning: Boolean = false,
    val notes: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("crew_member_id") val crewMemberId: String? = null,
) {
    fun toDomain(): TimeEntry = TimeEntry(
        id = id,
        jobId = jobId,
        workerName = workerName,
        clockIn = clockIn,
        clockOut = clockOut,
        hours = hours,
        rate = rate,
        isRunning = isRunning,
        notes = notes,
        createdAt = createdAt,
        crewMemberId = crewMemberId,
    )
}
