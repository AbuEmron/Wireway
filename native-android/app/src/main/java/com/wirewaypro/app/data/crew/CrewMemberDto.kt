package com.wirewaypro.app.data.crew

import com.wirewaypro.app.domain.model.CrewMember
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Wire shape of a `crew_members` row (columns per supabase/migration_crew.sql). */
@Serializable
data class CrewMemberDto(
    val id: String,
    // user_id is carried so the local cache round-trips the full row (an offline
    // upsert replays every column), matching JobDrawDto.
    @SerialName("user_id") val userId: String? = null,
    val name: String = "",
    val role: String? = null,
    @SerialName("hourly_cost_rate") val hourlyCostRate: Double = 0.0,
    val active: Boolean = true,
    @SerialName("created_at") val createdAt: String? = null,
) {
    fun toDomain(): CrewMember = CrewMember(
        id = id,
        name = name.ifBlank { "Crew member" },
        role = role,
        hourlyCostRate = hourlyCostRate,
        active = active,
        createdAt = createdAt,
    )
}
