package com.wirewaypro.app.domain.model

/**
 * A labor time entry (`time_entries` table). Either a live in/out timer (set
 * [isRunning] while clocked in) or a completed/manual entry with [hours]. Completed
 * entries contribute hours × rate as real labor cost into Job Costing.
 */
data class TimeEntry(
    val id: String,
    val jobId: String?,
    val workerName: String?,
    val clockIn: String?,
    val clockOut: String?,
    val hours: Double?,
    val rate: Double,
    val isRunning: Boolean,
    val notes: String?,
    val createdAt: String?,
    /**
     * The crew member these hours belong to, if logged against the roster.
     * [workerName] and [rate] are snapshotted from that crew member at log time,
     * so labor cost stays correct even if the crew member is later removed or
     * their rate changes. Null for the owner's own untagged time.
     */
    val crewMemberId: String? = null,
) {
    /** Labor cost contribution (hours × cost rate). */
    val laborCost: Double get() = (hours ?: 0.0) * rate
}
