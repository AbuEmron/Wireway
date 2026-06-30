package com.wirewaypro.app.data.stripe

import com.wirewaypro.app.domain.model.ConnectStatus
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Stripe (Connect + Checkout) via the SAME server routes the web app uses
 * (/api/connect-onboarding, /api/create-checkout, /api/pay-draw), gated by the
 * Supabase access token — identical pattern to [com.wirewaypro.app.data.plaid.PlaidService].
 * Stripe-hosted URLs (onboarding, Checkout) are opened in a browser/Custom Tab by
 * the UI; this layer only fetches them and reads Connect status.
 */
@Singleton
class StripeService @Inject constructor(
    private val client: SupabaseClient,
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val http = HttpClient(Android) {
        install(HttpTimeout) {
            requestTimeoutMillis = 60_000
            socketTimeoutMillis = 60_000
            connectTimeoutMillis = 30_000
        }
    }

    /** Start (or resume) Connect onboarding → a Stripe-hosted onboarding URL. */
    suspend fun createConnectOnboardingLink(): Result<String> = runCatching {
        val obj = apiPost("/api/connect-onboarding", buildJsonObject { put("action", "link") })
        (obj["url"] as? JsonPrimitive)?.content?.takeIf { it.isNotBlank() }
            ?: error("No onboarding URL returned")
    }

    /** Whether the contractor's connected account can accept payments yet. */
    suspend fun connectStatus(): Result<ConnectStatus> = runCatching {
        val obj = apiPost("/api/connect-onboarding", buildJsonObject { put("action", "status") })
        ConnectStatus(
            connected = (obj["connected"] as? JsonPrimitive)?.booleanOrNull ?: false,
            chargesEnabled = (obj["charges_enabled"] as? JsonPrimitive)?.booleanOrNull ?: false,
        )
    }

    private suspend fun apiPost(path: String, body: JsonObject?): JsonObject {
        val token = client.auth.currentSessionOrNull()?.accessToken ?: error("Not signed in.")
        val response = http.post("$BASE_URL$path") {
            header("Authorization", "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody((body ?: buildJsonObject {}).toString())
        }
        val text = response.bodyAsText()
        // Surface the REAL backend failure (401 / 500 "Could not start Stripe Connect."
        // / 503) instead of a generic message.
        if (response.status.value !in 200..299) {
            val parsed = runCatching { json.parseToJsonElement(text) as? JsonObject }.getOrNull()
            val msg = (parsed?.get("error") as? JsonPrimitive)?.content
            val snippet = (msg ?: text.take(200)).ifBlank { "(empty body)" }
            error("Payments service error ${response.status.value}: $snippet")
        }
        return runCatching { json.parseToJsonElement(text) as? JsonObject }.getOrNull() ?: buildJsonObject {}
    }

    companion object {
        private const val BASE_URL = "https://www.wireway.cc"
    }
}
