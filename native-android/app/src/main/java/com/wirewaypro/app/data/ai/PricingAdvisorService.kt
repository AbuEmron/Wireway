package com.wirewaypro.app.data.ai

import com.wirewaypro.app.domain.model.PricingRecommendation
import com.wirewaypro.app.domain.model.RateMode
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
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AI pricing advisor: given a job description, the customer's location, and the
 * contractor's baseline rates, it suggests hourly-vs-flat and a recommended
 * rate/total grounded in that LOCATION's economic reality — nationwide, not tied
 * to any one region. Uses the same /api/claude proxy (Bearer token, no AI keys in
 * the client). The result is only a suggestion; the UI lets the user override.
 */
@Singleton
class PricingAdvisorService @Inject constructor(
    private val client: SupabaseClient,
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val http = HttpClient(Android)

    suspend fun recommend(
        jobDescription: String,
        area: String,
        baselineHourly: Double,
        baselineFlat: Double?,
    ): Result<PricingRecommendation> = runCatching {
        val token = client.auth.currentSessionOrNull()?.accessToken ?: error("Not signed in.")

        val userText = buildString {
            append("Job: ").append(jobDescription.ifBlank { "general residential electrical work" }).append("\n")
            append("Job location / area: ").append(area.ifBlank { "(US location not specified)" }).append("\n")
            append("My baseline hourly rate: $").append(num(baselineHourly)).append("/hr\n")
            if (baselineFlat != null && baselineFlat > 0) {
                append("My typical flat-rate baseline: $").append(num(baselineFlat)).append("\n")
            }
            append("Recommend how I should price this job in this area.")
        }

        val body = buildJsonObject {
            put("max_tokens", 1200)
            put("system", systemPrompt())
            putJsonArray("messages") {
                add(
                    buildJsonObject {
                        put("role", "user")
                        putJsonArray("content") {
                            add(
                                buildJsonObject {
                                    put("type", "text")
                                    put("text", userText)
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
        val raw = response.bodyAsText()
        val text = extractText(raw).ifBlank { raw }
        parse(text) ?: error("Couldn't read the suggestion. Try again.")
    }

    private fun parse(text: String): PricingRecommendation? {
        val cleaned = text.replace("```json", "").replace("```", "")
        val objText = balancedBlock(cleaned, '{', '}') ?: return null
        val root = runCatching { json.parseToJsonElement(objText).jsonObject }.getOrNull() ?: return null
        fun d(key: String) = (root[key] as? JsonPrimitive)?.doubleOrNull
        fun s(key: String) = (root[key] as? JsonPrimitive)?.content
        return PricingRecommendation(
            mode = RateMode.from(s("mode")),
            recommendedRate = d("recommendedRate"),
            recommendedTotal = d("recommendedTotal"),
            lowTotal = d("lowTotal"),
            highTotal = d("highTotal"),
            areaContext = s("areaContext").orEmpty(),
            reasoning = s("reasoning").orEmpty(),
        )
    }

    /** Joins the Anthropic-style content blocks' text from the /api/claude envelope. */
    private fun extractText(raw: String): String = runCatching {
        json.parseToJsonElement(raw).jsonObject["content"]?.jsonArray
            ?.joinToString("") { block ->
                runCatching { block.jsonObject["text"]?.jsonPrimitive?.content.orEmpty() }.getOrDefault("")
            }
            .orEmpty()
    }.getOrDefault("")

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

    private fun systemPrompt(): String = """
        You are a pricing advisor for a licensed electrical contractor working anywhere in the United States. Given a job description, the job's location, and the contractor's baseline rates, recommend how to price the job.

        Account for the REGIONAL ECONOMIC REALITY of the SPECIFIC location named:
        - Local cost of living and prevailing electrician wages.
        - Urban vs. rural and metro density — dense metros and high-cost-of-living/coastal areas support higher rates; rural and lower-cost areas support lower rates.
        - What residential electrical customers in THAT area realistically pay.
        This varies widely across the country, so be specific to the named location rather than a national average. If the location is vague, reason from whatever is given and say so.

        You only SUGGEST a starting point. The contractor always sets the final price based on what their work is worth — keep the tone encouraging and on their side.

        Return ONLY a single JSON object — no markdown fences, no text outside the JSON:
        {
          "mode": "hourly" | "flat",
          "recommendedRate": number or null,
          "recommendedTotal": number,
          "lowTotal": number,
          "highTotal": number,
          "areaContext": "1-2 sentences on the local market for this location",
          "reasoning": "1-2 sentences on why this mode and price fit"
        }
    """.trimIndent()

    private fun num(v: Double): String = if (v % 1.0 == 0.0) v.toLong().toString() else v.toString()

    companion object {
        private const val BASE_URL = "https://www.wirewaypro.com"
    }
}
