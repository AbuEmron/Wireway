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
) {
    /** Labor cost contribution (hours × cost rate). */
    val laborCost: Double get() = (hours ?: 0.0) * rate
}
