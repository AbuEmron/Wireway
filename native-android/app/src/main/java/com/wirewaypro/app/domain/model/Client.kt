package com.wirewaypro.app.domain.model

/**
 * A row from the `clients` table. Columns match the web app's upsertClient
 * payload (src/lib/supabase.js): name + optional contact, plus running
 * job_count / total_billed tallies.
 */
data class Client(
    val id: String,
    val name: String,
    val email: String?,
    val phone: String?,
    val jobCount: Int?,
    val totalBilled: Double?,
    val createdAt: String?,
)
