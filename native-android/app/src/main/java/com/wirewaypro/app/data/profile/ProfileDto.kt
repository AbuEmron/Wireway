package com.wirewaypro.app.data.profile

import com.wirewaypro.app.domain.model.UserProfile
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Wire shape of a `profiles` row. Column names match the web app's schema; nulls
 * are tolerated so partially-filled rows still decode.
 */
@Serializable
data class ProfileDto(
    val id: String,
    @SerialName("full_name") val fullName: String? = null,
    val email: String? = null,
    val plan: String? = null,
    @SerialName("subscription_status") val subscriptionStatus: String? = null,
) {
    fun toDomain(): UserProfile = UserProfile(
        id = id,
        fullName = fullName,
        email = email,
        plan = plan,
        subscriptionStatus = subscriptionStatus,
    )
}
