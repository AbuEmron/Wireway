package com.wirewaypro.app.domain.repository

import com.wirewaypro.app.domain.model.AuthState
import kotlinx.coroutines.flow.Flow

/**
 * Auth surface the UI depends on. Backed by Supabase gotrue in the data layer,
 * but the contract is SDK-agnostic.
 */
interface AuthRepository {

    /** Emits the current auth state and every change (sign-in, sign-out, refresh). */
    val authState: Flow<AuthState>

    /** Email/password sign-in. Returns failure with a user-safe message. */
    suspend fun signIn(email: String, password: String): Result<Unit>

    /** Clears the session locally and on the server. */
    suspend fun signOut(): Result<Unit>

    /** Convenience accessor for the signed-in user id, or null. */
    fun currentUserId(): String?
}
