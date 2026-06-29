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
        val obj = apiPost("/api/plaid-create-link-token", null)
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

    private suspend fun apiPost(path: String, body: JsonObject?): JsonObject {
        val token = client.auth.currentSessionOrNull()?.accessToken ?: error("Not signed in.")
        val response = http.post("$BASE_URL$path") {
            header("Authorization", "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody((body ?: buildJsonObject {}).toString())
        }
        val text = response.bodyAsText()
        return runCatching { json.parseToJsonElement(text) as? JsonObject }.getOrNull() ?: buildJsonObject {}
    }

    companion object {
        private const val BASE_URL = "https://www.wirewaypro.com"
    }
}

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
