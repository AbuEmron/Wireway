package com.wirewaypro.app.data.clients

import com.wirewaypro.app.domain.model.Client
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Wire shape of a `clients` row (columns per src/lib/supabase.js upsertClient). */
@Serializable
data class ClientDto(
    val id: String,
    val name: String = "",
    val email: String? = null,
    val phone: String? = null,
    @SerialName("job_count") val jobCount: Int? = null,
    @SerialName("total_billed") val totalBilled: Double? = null,
    @SerialName("created_at") val createdAt: String? = null,
) {
    fun toDomain(): Client = Client(
        id = id,
        name = name.ifBlank { "Unnamed client" },
        email = email,
        phone = phone,
        jobCount = jobCount,
        totalBilled = totalBilled,
        createdAt = createdAt,
    )
}
