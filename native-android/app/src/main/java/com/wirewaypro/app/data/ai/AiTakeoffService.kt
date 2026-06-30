package com.wirewaypro.app.data.ai

import android.util.Base64
import com.wirewaypro.app.domain.catalog.Catalog
import com.wirewaypro.app.domain.model.TakeoffResult
import com.wirewaypro.app.domain.model.TakeoffSuggestion
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
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import kotlin.math.max
import kotlin.math.roundToInt
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AI takeoff: turn a plain-English scope (and optionally a plan photo/PDF page)
 * into catalog-mapped line items, via the SAME /api/claude proxy + system prompt
 * the web app uses (AIQuoteBuilder.jsx). Bearer token from Supabase; no AI keys
 * in the client.
 */
@Singleton
class AiTakeoffService @Inject constructor(
    private val client: SupabaseClient,
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val http = HttpClient(Android) {
        install(io.ktor.client.plugins.HttpTimeout) {
            requestTimeoutMillis = 90_000
            socketTimeoutMillis = 90_000
            connectTimeoutMillis = 30_000
        }
    }

    suspend fun analyze(prompt: String, imageBytes: ByteArray?): Result<TakeoffResult> = runCatching {
        val token = client.auth.currentSessionOrNull()?.accessToken ?: error("Not signed in.")

        val body = buildJsonObject {
            put("max_tokens", 3000)
            put("system", buildSystemPrompt())
            putJsonArray("messages") {
                add(
                    buildJsonObject {
                        put("role", "user")
                        putJsonArray("content") {
                            if (imageBytes != null) {
                                add(
                                    buildJsonObject {
                                        put("type", "image")
                                        putJsonObject("source") {
                                            put("type", "base64")
                                            put("media_type", "image/jpeg")
                                            put("data", Base64.encodeToString(imageBytes, Base64.NO_WRAP))
                                        }
                                    },
                                )
                            }
                            add(
                                buildJsonObject {
                                    put("type", "text")
                                    put(
                                        "text",
                                        if (imageBytes != null) {
                                            "Analyze this plan/scope-of-work image and produce the estimate JSON. " +
                                                "Additional notes: ${prompt.ifBlank { "(none)" }}"
                                        } else {
                                            prompt
                                        },
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
        // Surface the real HTTP failure (auth/rate-limit/server) instead of letting
        // a non-2xx body fall through to a misleading parse error.
        val raw = response.claudeBodyOrThrow(json)
        // /api/claude returns the full Anthropic envelope; pull the model's text
        // (content[].text) out first, then extract its JSON object.
        val text = extractText(raw).ifBlank { raw }
        parse(text) ?: error("The AI returned an unexpected response. [${text.take(180)}]")
    }

    /** Joins the Anthropic-style content blocks' text from the /api/claude envelope. */
    private fun extractText(raw: String): String = runCatching {
        json.parseToJsonElement(raw).jsonObject["content"]?.jsonArray
            ?.joinToString("") { block ->
                runCatching { block.jsonObject["text"]?.jsonPrimitive?.content.orEmpty() }.getOrDefault("")
            }
            .orEmpty()
    }.getOrDefault("")

    private fun parse(raw: String): TakeoffResult? {
        val cleaned = raw.replace("```json", "").replace("```", "")
        val objText = balancedBlock(cleaned, '{', '}') ?: return null
        val root = runCatching { json.parseToJsonElement(objText).jsonObject }.getOrNull() ?: return null
        val services = (root["services"] as? JsonArray) ?: return null

        val suggestions = services.mapNotNull { el ->
            val obj = el as? JsonObject ?: return@mapNotNull null
            val id = (obj["id"] as? JsonPrimitive)?.content ?: return@mapNotNull null
            val service = Catalog.service(id) ?: return@mapNotNull null
            val rawQty = (obj["qty"] as? JsonPrimitive)?.doubleOrNull ?: 1.0
            val qty = max(1, rawQty.roundToInt()).toDouble()
            val variantIdx = ((obj["variantIdx"] as? JsonPrimitive)?.doubleOrNull?.toInt() ?: 0)
                .coerceIn(0, (service.variants.size - 1).coerceAtLeast(0))
            TakeoffSuggestion(
                serviceId = id,
                qty = qty,
                variantIdx = variantIdx,
                clientBuys = (obj["clientBuys"] as? JsonPrimitive)?.booleanOrNull ?: false,
                reason = (obj["reason"] as? JsonPrimitive)?.content,
            )
        }

        val summary = (root["summary"] as? JsonPrimitive)?.content.orEmpty()
        val assumptions = (root["assumptions"] as? JsonArray)
            ?.mapNotNull { (it as? JsonPrimitive)?.content }
            .orEmpty()
        return TakeoffResult(suggestions, summary, assumptions)
    }

    /** First balanced [open]…[close] block, skipping braces inside strings. */
    private fun balancedBlock(s: String, open: Char, close: Char): String? {
        val start = s.indexOf(open)
        if (start == -1) return null
        var depth = 0
        var inStr = false
        var esc = false
        for (i in start until s.length) {
            val ch = s[i]
            when {
                inStr -> when {
                    esc -> esc = false
                    ch == '\\' -> esc = true
                    ch == '"' -> inStr = false
                }
                ch == '"' -> inStr = true
                ch == open -> depth++
                ch == close -> {
                    depth--
                    if (depth == 0) return s.substring(start, i + 1)
                }
            }
        }
        return null
    }

    private fun buildSystemPrompt(): String {
        val catalog = Catalog.categories.joinToString("\n\n") { cat ->
            cat.label + ":\n" + cat.services.joinToString("\n") { s ->
                "  - id:\"${s.id}\" | \"${s.label}\" | ${s.nec} | " +
                    "mat:$${num(s.materialCost)} lab:$${num(s.laborCost)} per ${s.unit} | " +
                    "variants: ${s.variants.joinToString(", ") { it.label }}"
            }
        }
        return """
            You are an expert residential electrical estimating AI for Wireway, built by a licensed electrician with NEC 2023 expertise.

            Your job: analyze a job description (and any attached plan image) and return a JSON object matching services from the Wireway catalog.

            WIREWAY SERVICE CATALOG:
            $catalog

            RULES:
            1. Return ONLY a single valid JSON object — no markdown fences, no preamble, no text outside the JSON.
            2. Match services from the catalog by their exact "id" field.
            3. Choose the most appropriate variant index (0 = first variant).
            4. Set realistic quantities based on the job.
            5. Only use catalog items; skip anything not in the catalog.
            6. Include NEC-required companions (GFCI with pool circuits, surge with panel upgrades, etc.).
            7. Be thorough but disciplined — include what the job needs, don't pad.
            8. Set "clientBuys": true only when the description says the customer is supplying that material.

            RETURN FORMAT — a single JSON object, nothing else:
            {
              "services": [
                { "id": "service_id_from_catalog", "qty": 2, "variantIdx": 0, "variantLabel": "variant name", "clientBuys": false, "reason": "one sentence why" }
              ],
              "summary": "2-sentence plain-English summary of the scope.",
              "assumptions": ["short note on anything inferred or to confirm"]
            }
        """.trimIndent()
    }

    private fun num(v: Double): String = if (v % 1.0 == 0.0) v.toLong().toString() else v.toString()

    companion object {
        private const val BASE_URL = "https://www.wirewaypro.com"
    }
}
