package com.wirewaypro.app.domain.repository

import com.wirewaypro.app.domain.model.ProfileInput
import com.wirewaypro.app.domain.model.UserProfile

/**
 * Reads the signed-in user's data from the shared Supabase backend.
 */
interface ProfileRepository {

    /** The user's row from `profiles`. */
    suspend fun getProfile(userId: String): Result<UserProfile>

    /** Exact count of the user's rows in `jobs` — used to prove the data layer end-to-end. */
    suspend fun getJobCount(userId: String): Result<Long>

    /** Updates the user's name + business info on `profiles`. */
    suspend fun saveProfile(userId: String, input: ProfileInput): Result<UserProfile>
}
