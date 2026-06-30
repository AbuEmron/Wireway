package com.wirewaypro.app.data.ocr

import android.util.Base64
import com.wirewaypro.app.domain.model.ExpenseCategories
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import javax.inject.Inject
import javax.inject.Singleton

/** Best-effort fields extracted from a receipt image. */
data class OcrResult(
    val vendor: String?,
    val date: String?,
    val amount: Double?,
    val category: String?,
    val summary: String?,
)

/**
 * Receipt OCR via the SAME server proxy the web app uses (src/lib/receipts.js):
 * POST {base}/api/claude with the image + a strict extraction prompt, gated by
 * the user's Supabase access token. The model returns a compact JSON object.
 */
@Singleton
class OcrService @Inject constructor(
    private val client: SupabaseClient,
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val http = HttpClient(Android)

    suspend fun ocr(imageBytes: ByteArray): Result<OcrResult> = runCatching {
        val token = client.auth.currentSessionOrNull()?.accessToken
            ?: error("Not signed in.")
        val base64 = Base64.encodeToString(imageBytes, Base64.NO_WRAP)
        val categoryIds = ExpenseCategories.ALL.joinToString(", ") { it.id }

        val body = buildJsonObject {
            put("max_tokens", 400)
            put(
                "system",
                "You are a precise receipt OCR extractor for a contractor's bookkeeping app. " +
                    "Return ONLY a single compact JSON object, no prose, no code fences.",
            )
            putJsonArray("messages") {
                add(
                    buildJsonObject {
                        put("role", "user")
                        putJsonArray("content") {
                            add(
                                buildJsonObject {
                                    put("type", "image")
                                    putJsonObject("source") {
                                        put("type", "base64")
                                        put("media_type", "image/jpeg")
                                        put("data", base64)
                                    }
                                },
                            )
                            add(
                                buildJsonObject {
                                    put("type", "text")
                                    put(
                                        "text",
                                        "Extract these fields from the receipt:\n" +
                                            "- vendor: the store/merchant name\n" +
                                            "- date: the purchase date as YYYY-MM-DD (null if not visible)\n" +
                                            "- amount: the grand TOTAL actually paid, as a number (no currency symbol)\n" +
                                            "- category: classify into exactly one of: $categoryIds\n" +
                                            "- summary: a 3-6 word description of what was bought\n" +
                                            "Respond as JSON: {\"vendor\":\"\",\"date\":\"YYYY-MM-DD\",\"amount\":0,\"category\":\"\",\"summary\":\"\"}. " +
                                            "Use null for any field you cannot read. Do not guess the date.",
                                    )
                                },
                            )
                        }
                    },
                )
            }
        }

        val response = http.post("$BASE_URL/api/claude") {
            header("Authorization", "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(body.toString())
        }
        parseResponse(response.bodyAsText())
    }

    private fun parseResponse(raw: String): OcrResult {
        val root = json.parseToJsonElement(raw).jsonObject
        val text = root["content"]?.jsonArray
            ?.joinToString("") { it.jsonObject["text"]?.jsonPrimitive?.content.orEmpty() }
            .orEmpty()

        var inner = text.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
        val start = inner.indexOf('{')
        val end = inner.lastIndexOf('}')
        if (start >= 0 && end > start) inner = inner.substring(start, end + 1)

        val obj = runCatching { json.parseToJsonElement(inner).jsonObject }.getOrNull()
        fun str(key: String): String? =
            obj?.get(key)?.let { runCatching { it.jsonPrimitive.content }.getOrNull() }
                ?.takeIf { it.isNotBlank() && it != "null" }

        val amount = obj?.get("amount")?.jsonPrimitive?.doubleOrNull?.takeIf { it > 0 }
        val date = str("date")?.takeIf { Regex("""\d{4}-\d{2}-\d{2}""").matches(it) }
        val category = str("category")?.takeIf { id -> ExpenseCategories.ALL.any { it.id == id } }

        return OcrResult(
            vendor = str("vendor"),
            date = date,
            amount = amount,
            category = category,
            summary = str("summary"),
        )
    }

    companion object {
        // Canonical app origin for /api/* — same Vercel deployment as wirewaypro.com,
        // but wireway.cc is the domain the web app actually runs on.
        private const val BASE_URL = "https://www.wireway.cc"
    }
}
