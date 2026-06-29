package com.wirewaypro.app.domain.model

/**
 * Domain view of a row in the `profiles` table (same table the web app reads).
 * Mirrors only the fields this phase needs; grows as features land.
 */
data class UserProfile(
    val id: String,
    val fullName: String?,
    val email: String?,
    val plan: String?,
    val subscriptionStatus: String?,
    val companyName: String? = null,
    val companyPhone: String? = null,
    val companyEmail: String? = null,
    val companyLicense: String? = null,
    val companyAddress: String? = null,
    val companyWebsite: String? = null,
) {
    /** Mirrors `isPro()` in the web app's src/lib/supabase.js. */
    val isPro: Boolean
        get() = plan == "pro" ||
            plan == "teams" ||
            subscriptionStatus == "trialing" ||
            subscriptionStatus == "active"

    /** Company/business header used on proposals & PDFs. */
    fun businessInfo(): BusinessInfo = BusinessInfo(
        name = companyName?.takeIf { it.isNotBlank() } ?: fullName,
        phone = companyPhone,
        email = companyEmail ?: email,
        license = companyLicense,
        address = companyAddress,
        website = companyWebsite,
    )
}

/** Branding/contact block for proposals and PDF exports. */
data class BusinessInfo(
    val name: String?,
    val phone: String?,
    val email: String?,
    val license: String?,
    val address: String?,
    val website: String?,
)

/** Editable profile fields (name + business info), written to `profiles`. */
data class ProfileInput(
    val fullName: String?,
    val companyName: String?,
    val companyPhone: String?,
    val companyEmail: String?,
    val companyLicense: String?,
    val companyAddress: String?,
    val companyWebsite: String?,
)
