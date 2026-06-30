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
    @SerialName("company_name") val companyName: String? = null,
    @SerialName("company_phone") val companyPhone: String? = null,
    @SerialName("company_email") val companyEmail: String? = null,
    @SerialName("company_license") val companyLicense: String? = null,
    @SerialName("company_address") val companyAddress: String? = null,
    @SerialName("company_website") val companyWebsite: String? = null,
    @SerialName("logo_url") val logoUrl: String? = null,
) {
    fun toDomain(): UserProfile = UserProfile(
        id = id,
        fullName = fullName,
        email = email,
        plan = plan,
        subscriptionStatus = subscriptionStatus,
        companyName = companyName,
        companyPhone = companyPhone,
        companyEmail = companyEmail,
        companyLicense = companyLicense,
        companyAddress = companyAddress,
        companyWebsite = companyWebsite,
        logoUrl = logoUrl,
    )
}
