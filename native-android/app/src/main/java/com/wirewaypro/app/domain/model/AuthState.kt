package com.wirewaypro.app.domain.model

/**
 * App-level auth status, derived from Supabase's session. Kept free of any SDK
 * types so the UI layer can switch on it without importing supabase-kt.
 */
sealed interface AuthState {
    /** Still loading the persisted session on cold start. */
    data object Loading : AuthState

    /** No valid session — show the login screen. */
    data object Unauthenticated : AuthState

    /** Signed in. */
    data class Authenticated(val userId: String, val email: String?) : AuthState
}
