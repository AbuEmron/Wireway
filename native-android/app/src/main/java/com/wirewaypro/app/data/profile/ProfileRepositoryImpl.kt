package com.wirewaypro.app.data.profile

import com.wirewaypro.app.domain.model.ProfileInput
import com.wirewaypro.app.domain.model.UserProfile
import com.wirewaypro.app.domain.repository.ProfileRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Count
import io.github.jan.supabase.storage.storage
import io.ktor.http.ContentType
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reads `profiles` and `jobs` from the shared Supabase backend. Row-Level
 * Security scopes everything to the signed-in user, so these queries return only
 * the caller's data even though the anon key is public.
 */
@Singleton
class ProfileRepositoryImpl @Inject constructor(
    private val client: SupabaseClient,
) : ProfileRepository {

    override suspend fun getProfile(userId: String): Result<UserProfile> =
        runCatching {
            val dto = client.postgrest
                .from("profiles")
                .select {
                    filter { eq("id", userId) }
                }
                .decodeSingleOrNull<ProfileDto>()
            // A missing profiles row (e.g. a brand-new user) is not an error —
            // fall back to a minimal profile so Home still renders.
            dto?.toDomain() ?: UserProfile(
                id = userId,
                fullName = null,
                email = null,
                plan = null,
                subscriptionStatus = null,
            )
        }

    override suspend fun getJobCount(userId: String): Result<Long> =
        runCatching {
            val result = client.postgrest
                .from("jobs")
                .select(Columns.list("id")) {
                    filter { eq("user_id", userId) }
                    count(Count.EXACT)
                }
            result.countOrNull() ?: 0L
        }

    override suspend fun saveProfile(userId: String, input: ProfileInput): Result<UserProfile> =
        runCatching {
            val payload = buildJsonObject {
                put("full_name", input.fullName)
                put("company_name", input.companyName)
                put("company_phone", input.companyPhone)
                put("company_email", input.companyEmail)
                put("company_license", input.companyLicense)
                put("company_address", input.companyAddress)
                put("company_website", input.companyWebsite)
            }
            client.postgrest.from("profiles")
                .update(payload) {
                    filter { eq("id", userId) }
                    select()
                }
                .decodeSingle<ProfileDto>()
                .toDomain()
        }

    override suspend fun uploadLogo(userId: String, bytes: ByteArray, mimeType: String?): Result<String> =
        runCatching {
            // The logos bucket is owner-folder RLS: writes require the user's JWT
            // and the first path segment must be their uid. Guard the session up
            // front so an expired login reads as "sign in again", not a bare 403.
            if (client.auth.currentSessionOrNull() == null) {
                error("Your session expired — sign in again, then retry the upload.")
            }
            val ext = when (mimeType?.lowercase()) {
                "image/jpeg", "image/jpg" -> "jpg"
                "image/webp" -> "webp"
                else -> "png"
            }
            val path = "$userId/logo.$ext"
            val bucket = client.storage.from("logos")
            bucket.upload(path, bytes) {
                upsert = true
                contentType = ContentType.parse(mimeType?.takeIf { it.startsWith("image/") } ?: "image/png")
            }
            // A re-upload with a different format leaves the old object behind —
            // clear stale siblings (best-effort; delete may be disallowed by policy).
            runCatching {
                val stale = listOf("png", "jpg", "webp").filter { it != ext }.map { "$userId/logo.$it" }
                bucket.delete(stale)
            }
            // Cache-bust so a replaced logo shows immediately on PDFs / customer pages.
            val url = "${bucket.publicUrl(path)}?v=${System.currentTimeMillis()}"
            client.postgrest.from("profiles")
                .update(buildJsonObject { put("logo_url", url) }) { filter { eq("id", userId) } }
            url
        }
}
