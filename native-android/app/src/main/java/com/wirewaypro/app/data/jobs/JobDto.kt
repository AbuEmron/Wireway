package com.wirewaypro.app.data.jobs

import com.wirewaypro.app.domain.model.Job
import com.wirewaypro.app.domain.model.JobDraw
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Wire shape of a `jobs` row. Column names per supabase/migration_jobs.sql. */
@Serializable
data class JobDto(
    val id: String,
    @SerialName("quote_id") val quoteId: String? = null,
    val title: String = "",
    @SerialName("client_name") val clientName: String? = null,
    @SerialName("client_phone") val clientPhone: String? = null,
    @SerialName("client_email") val clientEmail: String? = null,
    @SerialName("job_address") val jobAddress: String? = null,
    val notes: String? = null,
    @SerialName("scheduled_date") val scheduledDate: String? = null,
    @SerialName("scheduled_time") val scheduledTime: String? = null,
    @SerialName("duration_hours") val durationHours: Double? = null,
    val status: String? = null,
    val total: Double? = null,
    @SerialName("created_at") val createdAt: String? = null,
) {
    fun toDomain(): Job = Job(
        id = id,
        title = title.ifBlank { "Untitled job" },
        clientName = clientName,
        clientPhone = clientPhone,
        clientEmail = clientEmail,
        jobAddress = jobAddress,
        notes = notes,
        scheduledDate = scheduledDate,
        scheduledTime = scheduledTime,
        durationHours = durationHours,
        status = status,
        total = total,
        createdAt = createdAt,
    )
}

/** Wire shape of a `job_draws` row (supabase/migration_billing.sql). */
@Serializable
data class JobDrawDto(
    val id: String,
    // user_id / job_id / invoiced_at aren't on the domain model, but are carried
    // so the local offline cache round-trips the full row without loss (an upsert
    // replays every column).
    @SerialName("user_id") val userId: String? = null,
    @SerialName("job_id") val jobId: String? = null,
    val label: String = "",
    val amount: Double = 0.0,
    @SerialName("retainage_pct") val retainagePct: Double = 0.0,
    val status: String = "pending",
    @SerialName("due_date") val dueDate: String? = null,
    @SerialName("invoiced_at") val invoicedAt: String? = null,
    @SerialName("paid_at") val paidAt: String? = null,
    @SerialName("sort_order") val sortOrder: Int = 0,
) {
    fun toDomain(): JobDraw = JobDraw(
        id = id,
        label = label.ifBlank { "Draw" },
        amount = amount,
        retainagePct = retainagePct,
        status = status,
        dueDate = dueDate,
        paidAt = paidAt,
        sortOrder = sortOrder,
    )
}
