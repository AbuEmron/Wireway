package com.wirewaypro.app.data.profile

import com.wirewaypro.app.domain.model.UserProfile
import com.wirewaypro.app.domain.repository.ProfileRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Count
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
                ?: error("No profile found for the current user.")
            dto.toDomain()
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
}
