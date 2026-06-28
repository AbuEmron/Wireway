package com.wirewaypro.app.data.auth

import com.wirewaypro.app.domain.model.AuthState
import com.wirewaypro.app.domain.repository.AuthRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.exceptions.RestException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Supabase-backed [AuthRepository]. gotrue persists and auto-refreshes the
 * session, so [authState] simply mirrors the SDK's session flow into our
 * domain model.
 */
@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val client: SupabaseClient,
) : AuthRepository {

    override val authState: Flow<AuthState> =
        client.auth.sessionStatus.map { status ->
            when (status) {
                is SessionStatus.Authenticated -> AuthState.Authenticated(
                    userId = status.session.user?.id.orEmpty(),
                    email = status.session.user?.email,
                )
                is SessionStatus.NotAuthenticated -> AuthState.Unauthenticated
                is SessionStatus.RefreshFailure -> AuthState.Unauthenticated
                SessionStatus.Initializing -> AuthState.Loading
            }
        }

    override suspend fun signIn(email: String, password: String): Result<Unit> =
        runCatching {
            client.auth.signInWith(Email) {
                this.email = email.trim()
                this.password = password
            }
        }.recoverCatching { t ->
            // Surface a clean message instead of a raw stack trace to the UI.
            throw AuthException(t.userMessage(), t)
        }

    override suspend fun signOut(): Result<Unit> =
        runCatching { client.auth.signOut() }

    override fun currentUserId(): String? =
        client.auth.currentUserOrNull()?.id
}

/** Auth error with a message safe to show the user. */
class AuthException(message: String, cause: Throwable?) : Exception(message, cause)

private fun Throwable.userMessage(): String = when (this) {
    is RestException -> when {
        message?.contains("Invalid login", ignoreCase = true) == true ->
            "Incorrect email or password."
        message?.contains("Email not confirmed", ignoreCase = true) == true ->
            "Please confirm your email before signing in."
        else -> message ?: "Sign-in failed. Please try again."
    }
    else -> "Couldn't reach Wireway. Check your connection and try again."
}
