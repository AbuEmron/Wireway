package com.wirewaypro.app.data.ai

import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Carries the REAL failure from /api/claude — the HTTP status plus a short body
 * snippet — so the UI can show what actually went wrong (auth, rate-limit, server
 * error, …) instead of a generic "couldn't do it" message.
 */
class ClaudeApiException(
    val status: Int,
    val bodySnippet: String,
    message: String,
) : Exception(message)

/**
 * Reads the response body, throwing a descriptive [ClaudeApiException] on any
 * non-2xx. Ktor does NOT throw on non-2xx by default, so without this a 401/429/
 * 500 would flow into JSON parsing and surface as a misleading "parse" error.
 * The proxy returns {"error": "..."} bodies, which we lift into the message.
 */
suspend fun HttpResponse.claudeBodyOrThrow(json: Json): String {
    val body = runCatching { bodyAsText() }.getOrDefault("")
    if (status.value in 200..299) return body

    val serverError = runCatching {
        json.parseToJsonElement(body).jsonObject["error"]?.jsonPrimitive?.content
    }.getOrNull()
    val snippet = (serverError ?: body.take(200)).ifBlank { "(empty body)" }
    val message = when (status.value) {
        401 -> "Sign-in expired (401) — sign out and back in. [$snippet]"
        403 -> "Access denied (403). [$snippet]"
        429 -> "$snippet (429)"
        in 500..599 -> "AI server error ${status.value}: $snippet"
        else -> "AI request failed (${status.value}): $snippet"
    }
    throw ClaudeApiException(status.value, snippet, message)
}
