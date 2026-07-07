package com.wirewaypro.app.data.ahj

import com.wirewaypro.app.domain.model.Jurisdiction
import com.wirewaypro.app.domain.model.JurisdictionSource
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Wire shape of a `user_jurisdictions` row (columns per supabase/migration_jurisdiction.sql). */
@Serializable
data class JurisdictionDto(
    val id: String,
    // user_id is carried so the local cache round-trips the full row (an offline
    // upsert replays every column), matching CrewMemberDto.
    @SerialName("user_id") val userId: String? = null,
    @SerialName("state_code") val stateCode: String = "",
    val county: String? = null,
    val city: String? = null,
    val source: String = "manual",
    @SerialName("created_at") val createdAt: String? = null,
) {
    fun toDomain(): Jurisdiction = Jurisdiction(
        id = id,
        stateCode = stateCode,
        county = county,
        city = city,
        source = when (source.lowercase()) {
            "gps_confirmed", "gps" -> JurisdictionSource.GPS_CONFIRMED
            else -> JurisdictionSource.MANUAL
        },
        createdAt = createdAt,
    )
}
