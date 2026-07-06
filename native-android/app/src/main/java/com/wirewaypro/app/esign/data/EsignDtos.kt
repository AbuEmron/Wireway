package com.wirewaypro.app.esign.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Supabase row shapes (snake_case) for the two e-signature tables. These are the
 * exact JSON bodies pushed to Postgrest and decoded back on refresh, mirroring the
 * DTO convention used by crew/jurisdiction. Column names here MUST match the SQL
 * migration in `supabase/` (see the reported migration file).
 *
 * Local file paths (sealed PDF, encrypted signature) are intentionally NOT sent to
 * the server — they're device-local. The server holds the evidentiary record + the
 * hashes; server-side blob storage is a documented follow-up (see the report).
 */
@Serializable
data class EsignRecordDto(
    val id: String,
    @SerialName("user_id") val userId: String? = null,
    @SerialName("quote_id") val quoteId: String,
    @SerialName("document_title") val documentTitle: String,
    @SerialName("signer_name") val signerName: String,
    @SerialName("signer_email") val signerEmail: String? = null,
    val method: String,
    @SerialName("consent_version") val consentVersion: String,
    @SerialName("consent_given_at") val consentGivenAt: String,
    @SerialName("signed_at") val signedAt: String,
    @SerialName("content_sha256") val contentSha256: String,
    @SerialName("sealed_sha256") val sealedSha256: String,
    @SerialName("device_model") val deviceModel: String,
    @SerialName("app_version") val appVersion: String,
    @SerialName("ip_address") val ipAddress: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
)

@Serializable
data class EsignAuditEventDto(
    val id: String,
    @SerialName("user_id") val userId: String? = null,
    @SerialName("record_id") val recordId: String,
    @SerialName("event_type") val eventType: String,
    @SerialName("at") val at: String,
    @SerialName("consent_version") val consentVersion: String,
    @SerialName("detail_json") val detailJson: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
)
