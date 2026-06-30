package com.wirewaypro.app.data.ai

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
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Plain-text AI completions via the SAME server proxy the web app uses
 * (`/api/claude`), gated by the user's Supabase access token. Self-contained
 * helper for the "draft with AI" button — no AI keys ever live in the client.
 */
@Singleton
class AiService @Inject constructor(
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

    suspend fun complete(system: String, userText: String, maxTokens: Int = 600): Result<String> = runCatching {
        val token = client.auth.currentSessionOrNull()?.accessToken ?: error("Not signed in.")
        val body = buildJsonObject {
            put("max_tokens", maxTokens)
            put("system", system)
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
        val text = extractText(response.claudeBodyOrThrow(json))
        if (text.isBlank()) error("The AI returned an empty response. Try again.") else text.trim()
    }

    /** Joins the Anthropic-style content blocks' text; tolerant of odd shapes. */
    private fun extractText(raw: String): String = runCatching {
        json.parseToJsonElement(raw).jsonObject["content"]?.jsonArray
            ?.joinToString("") { block ->
                runCatching { block.jsonObject["text"]?.jsonPrimitive?.content.orEmpty() }.getOrDefault("")
            }
            .orEmpty()
    }.getOrDefault("")

    companion object {
        private const val BASE_URL = "https://www.wirewaypro.com"
    }
}
