package com.wirewaypro.app.domain.model

/**
 * A scheduled job from the `jobs` table (the Job Calendar's row). Read-only this
 * phase. Fields mirror supabase/migration_jobs.sql exactly; everything past the
 * identity columns is nullable because rows are often partially filled.
 */
data class Job(
    val id: String,
    val title: String,
    val clientName: String?,
    val clientPhone: String?,
    val clientEmail: String?,
    val jobAddress: String?,
    val notes: String?,
    val scheduledDate: String?, // ISO date "yyyy-MM-dd"
    val scheduledTime: String?, // "HH:mm:ss"
    val durationHours: Double?,
    val status: String?,        // scheduled | in_progress | complete | cancelled
    val total: Double?,
    val createdAt: String?,
)

/**
 * A progress-billing draw for a job (`job_draws`), used as the job detail's line
 * items: a billing schedule of deposit → draws → final.
 */
data class JobDraw(
    val id: String,
    val label: String,
    val amount: Double,
    val retainagePct: Double,
    val status: String,   // pending | invoiced | paid
    val dueDate: String?,
    val paidAt: String?,
    val sortOrder: Int,
)
