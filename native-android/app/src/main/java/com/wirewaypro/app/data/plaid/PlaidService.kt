package com.wirewaypro.app.data.plaid

import com.wirewaypro.app.domain.model.PlaidTxn
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.put
import java.time.Year
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Plaid backend integration via the SAME server routes the web app uses
 * (/api/plaid-*), gated by the Supabase access token. The secure bank-login UI
 * itself is the Plaid Link SDK (wired separately); this handles tokens, sync, and
 * reading the synced rows from `plaid_transactions`.
 */
@Singleton
class PlaidService @Inject constructor(
    private val client: SupabaseClient,
) {
    private val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
    private val http = HttpClient(Android)

    suspend fun createLinkToken(): Result<String> = runCatching {
        // The Plaid Link Android SDK requires the link_token to be created with this
        // app's android_package_name (and it must be allowlisted in the Plaid
        // dashboard). We send it so the backend can set it; harmless if ignored.
        val obj = apiPost(
            "/api/plaid-create-link-token",
            buildJsonObject {
                put("android_package_name", com.wirewaypro.app.BuildConfig.APPLICATION_ID)
            },
        )
        (obj["link_token"] as? JsonPrimitive)?.content?.takeIf { it.isNotBlank() }
            ?: error("No link token returned")
    }

    suspend fun exchangeToken(
        publicToken: String,
        institutionId: String?,
        institutionName: String?,
    ): Result<Unit> = runCatching {
        apiPost(
            "/api/plaid-exchange-token",
            buildJsonObject {
                put("public_token", publicToken)
                put("institution_id", institutionId)
                put("institution_name", institutionName)
            },
        )
        Unit
    }

    suspend fun sync(): Result<Int> = runCatching {
        val obj = apiPost("/api/plaid-sync", null)
        (obj["synced"] as? JsonPrimitive)?.doubleOrNull?.toInt() ?: 0
    }

    suspend fun getTransactions(userId: String, year: Int = Year.now().value): Result<List<PlaidTxn>> = runCatching {
        client.postgrest.from("plaid_transactions")
            .select {
                filter {
                    eq("user_id", userId)
                    gt("amount", 0)
                    gte("txn_date", "$year-01-01")
                    lte("txn_date", "$year-12-31")
                }
                order("txn_date", Order.DESCENDING)
                limit(200)
            }
            .decodeList<PlaidTxnDto>()
            .map { it.toDomain() }
    }

    /** Institutions the user has linked (drives the Bank screen's connected state). */
    suspend fun getLinkedInstitutions(userId: String): Result<List<String>> = runCatching {
        client.postgrest.from("plaid_items")
            .select { filter { eq("user_id", userId) } }
            .decodeList<LinkedItemDto>()
            .map { it.institutionName?.takeIf { n -> n.isNotBlank() } ?: "Bank" }
    }

    /**
     * Returns a valid access token, briefly waiting for the persisted session to
     * finish restoring. Right after the app returns from an external activity
     * (e.g. the Plaid Link flow) the process may have been recreated and the
     * Supabase session may still be loading — without this wait the token
     * exchange would abort as "not signed in" and the bank link would never save.
     */
    private suspend fun awaitAccessToken(): String {
        repeat(20) {
            client.auth.currentSessionOrNull()?.accessToken?.let { return it }
            kotlinx.coroutines.delay(250)
        }
        error("Not signed in.")
    }

    private suspend fun apiPost(path: String, body: JsonObject?): JsonObject {
        val token = awaitAccessToken()
        val response = http.post("$BASE_URL$path") {
            header("Authorization", "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody((body ?: buildJsonObject {}).toString())
        }
        val text = response.bodyAsText()
        // Surface the REAL backend failure (401 sign-in / 503 Plaid-not-configured /
        // 500 + Plaid detail) instead of letting it fall through as "no link token".
        if (response.status.value !in 200..299) {
            val parsed = runCatching { json.parseToJsonElement(text) as? JsonObject }.getOrNull()
            val msg = (parsed?.get("error") as? JsonPrimitive)?.content
            val detail = (parsed?.get("detail") as? JsonPrimitive)?.content
            val snippet = (msg ?: text.take(200)).ifBlank { "(empty body)" }
            error("Bank service error ${response.status.value}: $snippet${detail?.let { " — $it" } ?: ""}")
        }
        return runCatching { json.parseToJsonElement(text) as? JsonObject }.getOrNull() ?: buildJsonObject {}
    }

    companion object {
        private const val BASE_URL = "https://www.wireway.cc"
    }
}

@Serializable
private data class LinkedItemDto(
    @SerialName("institution_name") val institutionName: String? = null,
)

@Serializable
private data class PlaidTxnDto(
    val id: String,
    @SerialName("txn_date") val txnDate: String? = null,
    val amount: Double = 0.0,
    @SerialName("merchant_name") val merchantName: String? = null,
    @SerialName("raw_name") val rawName: String? = null,
    @SerialName("user_category") val userCategory: String? = null,
    @SerialName("mapped_category") val mappedCategory: String? = null,
) {
    fun toDomain(): PlaidTxn = PlaidTxn(
        id = id,
        date = txnDate,
        name = merchantName ?: rawName ?: "Bank charge",
        category = userCategory ?: mappedCategory,
        amount = amount,
    )
}
