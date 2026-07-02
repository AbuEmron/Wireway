package com.wirewaypro.app.domain.model

/**
 * Result of a successful sign-up. Supabase either signs the new user in
 * immediately (email confirmation disabled) or creates the account pending an
 * emailed confirmation link. The web app requires confirmation, so the app
 * mirrors that "check your email" flow when [ConfirmationRequired] comes back.
 */
enum class SignUpOutcome {
    /** Account created and a session is active — the app routes to the dashboard. */
    SignedIn,

    /** Account created; the user must confirm their email before signing in. */
    ConfirmationRequired,
}
