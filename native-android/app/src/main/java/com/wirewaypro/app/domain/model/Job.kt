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
    /** The estimate this job was won from, if any — the basis for job costing. */
    val quoteId: String? = null,
    val syncState: SyncState = SyncState.SYNCED,
)

/** Everything the job editor can write. Mirrors the `jobs` columns. */
data class JobInput(
    val id: String?,            // null = create
    val title: String,
    val clientName: String?,
    val clientPhone: String?,
    val clientEmail: String?,
    val jobAddress: String?,
    val notes: String?,
    val scheduledDate: String?, // "yyyy-MM-dd"
    val scheduledTime: String?, // "HH:mm" / "HH:mm:ss"
    val durationHours: Double?,
    val status: String,         // scheduled | in_progress | complete | cancelled
    val total: Double?,
    val quoteId: String?,
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
) {
    /** Retainage withheld from this draw: round2(amount × pct/100). */
    val retainage: Double get() = MoneyMath.round2(amount * retainagePct / 100.0)

    /** Net billed now: amount − retainage. */
    val net: Double get() = MoneyMath.round2(amount - retainage)
}

/** Everything the draw editor can write. Mirrors `job_draws` columns. */
data class JobDrawInput(
    val id: String?,            // null = create
    val jobId: String,
    val label: String,
    val amount: Double,
    val retainagePct: Double,
    val status: String,         // pending | invoiced | paid
    val dueDate: String?,
    val sortOrder: Int,
)
