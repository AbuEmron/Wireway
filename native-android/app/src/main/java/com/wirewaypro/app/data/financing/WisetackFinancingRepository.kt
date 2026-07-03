package com.wirewaypro.app.data.financing

import com.wirewaypro.app.domain.financing.FinancingOffer
import com.wirewaypro.app.domain.financing.FinancingSetup
import com.wirewaypro.app.domain.repository.FinancingRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Wisetack adapter for [FinancingRepository], talking to the Wireway backend
 * proxy (which alone holds the Wisetack keys — none live in the app). Until the
 * backend routes exist, `setup()` maps their absence (404/501) to "not
 * connected", so the UI shows an honest setup state instead of an error.
 *
 * Backend contract this expects (all authenticated with the user's JWT):
 *   GET    /api/financing/status               -> {connected, provider, merchantName?, connectUrl?}
 *   POST   /api/financing/offers               -> {estimateId, amount, clientName?, clientEmail?, clientPhone?}
 *                                              <- {estimateId, applicationUrl, status, asLowAsMonthly?, termMonths?, updatedAt?}
 *   GET    /api/financing/offers/{estimateId}  <- offer JSON | 404
 *   DELETE /api/financing/offers/{estimateId}  -> 2xx | 404 (both = gone)
 *   POST   /api/financing/webhook              <- Wisetack signed webhooks (server-side only;
 *                                                 updates offer status that GET then reflects)
 */
@Singleton
class WisetackFinancingRepository @Inject constructor(
    private val client: SupabaseClient,
) : FinancingRepository {

    private val http = HttpClient(Android) {
        install(io.ktor.client.plugins.HttpTimeout) {
            requestTimeoutMillis = 30_000
            socketTimeoutMillis = 30_000
            connectTimeoutMillis = 15_000
        }
    }

    private fun HttpRequestBuilder.authorize() {
        val token = client.auth.currentSessionOrNull()?.accessToken ?: error("Not signed in.")
        header("Authorization", "Bearer $token")
    }

    override suspend fun setup(): Result<FinancingSetup> = runCatching {
        val resp = http.get("$BASE_URL/api/financing/status") { authorize() }
        when {
            resp.status == HttpStatusCode.NotFound || resp.status == HttpStatusCode.NotImplemented ->
                FinancingSetup(connected = false) // backend route not live yet
            resp.status.isSuccess() -> FinancingPayloads.parseSetup(resp.bodyAsText())
            else -> error("Couldn't check financing status (${resp.status.value}).")
        }
    }

    override suspend fun createOffer(
        estimateId: String,
        amount: Double,
        clientName: String?,
        clientEmail: String?,
        clientPhone: String?,
    ): Result<FinancingOffer> = runCatching {
        val body = buildJsonObject {
            put("estimateId", estimateId)
            put("amount", amount)
            put("clientName", clientName)
            put("clientEmail", clientEmail)
            put("clientPhone", clientPhone)
        }
        val resp = http.post("$BASE_URL/api/financing/offers") {
            authorize()
            contentType(ContentType.Application.Json)
            setBody(body.toString())
        }
        when {
            resp.status == HttpStatusCode.NotFound || resp.status == HttpStatusCode.NotImplemented ->
                error("Financing isn't live on the backend yet — no link can be created.")
            resp.status.isSuccess() -> FinancingPayloads.parseOffer(estimateId, resp.bodyAsText())
            else -> error("Couldn't create the financing link (${resp.status.value}).")
        }
    }

    override suspend fun offerFor(estimateId: String): Result<FinancingOffer?> = runCatching {
        val resp = http.get("$BASE_URL/api/financing/offers/$estimateId") { authorize() }
        when {
            resp.status == HttpStatusCode.NotFound || resp.status == HttpStatusCode.NotImplemented -> null
            resp.status.isSuccess() -> FinancingPayloads.parseOffer(estimateId, resp.bodyAsText())
            else -> error("Couldn't load the financing status (${resp.status.value}).")
        }
    }

    override suspend fun removeOffer(estimateId: String): Result<Unit> = runCatching {
        val resp = http.delete("$BASE_URL/api/financing/offers/$estimateId") { authorize() }
        if (!resp.status.isSuccess() && resp.status != HttpStatusCode.NotFound) {
            error("Couldn't withdraw the financing offer (${resp.status.value}).")
        }
    }

    private companion object {
        const val BASE_URL = "https://www.wireway.cc"
    }
}
