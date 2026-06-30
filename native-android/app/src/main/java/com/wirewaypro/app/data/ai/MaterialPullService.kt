package com.wirewaypro.app.data.ai

import com.wirewaypro.app.domain.catalog.SupplyHouses
import com.wirewaypro.app.domain.model.PullItem
import com.wirewaypro.app.domain.model.PullListResult
import com.wirewaypro.app.domain.model.PullSection
import com.wirewaypro.app.domain.model.StorePrice
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
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Builds the Material Pull List: an itemized shopping list grouped by service,
 * with LIVE big-box prices web-searched near the job's location and a per-store
 * comparison. Ports the web app's MaterialsListView prompt, generalized to all
 * public supply houses + the local area. Uses /api/claude with web_search=true,
 * so a build takes ~45-60s — hence the long HTTP timeout.
 */
@Singleton
class MaterialPullService @Inject constructor(
    private val client: SupabaseClient,
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val http = HttpClient(Android) {
        install(HttpTimeout) {
            requestTimeoutMillis = 120_000
            socketTimeoutMillis = 120_000
            connectTimeoutMillis = 30_000
        }
    }

    suspend fun build(jobName: String?, lines: List<String>, area: String): Result<PullListResult> = runCatching {
        val token = client.auth.currentSessionOrNull()?.accessToken ?: error("Not signed in.")

        val userText = buildString {
            append("Job: ").append(jobName?.ifBlank { null } ?: "Residential electrical").append("\n")
            append("Job location / area: ").append(area.ifBlank { "(US location not specified)" }).append("\n")
            append("Services:\n").append(lines.joinToString("\n"))
        }

        val body = buildJsonObject {
            put("max_tokens", 4096)
            put("web_search", true)
            put("system", systemPrompt())
            putJsonArray("messages") {
                add(
                    buildJsonObject {
                        put("role", "user")
                        put("content", userText)
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
        parse(text) ?: error("Couldn't read the pull list. Try again.")
    }

    private fun parse(text: String): PullListResult? {
        val cleaned = text.replace("```json", "").replace("```", "")
        val objText = balancedBlock(cleaned, '{', '}') ?: return null
        val root = runCatching { json.parseToJsonElement(objText).jsonObject }.getOrNull() ?: return null
        val sectionsArr = root["sections"] as? JsonArray ?: return null

        val sections = sectionsArr.mapNotNull { el ->
            val obj = el as? JsonObject ?: return@mapNotNull null
            val service = (obj["service"] as? JsonPrimitive)?.content ?: "Materials"
            val items = (obj["items"] as? JsonArray).orEmptyArray().mapNotNull { parseItem(it as? JsonObject) }
            if (items.isEmpty()) null else PullSection(service, items)
        }
        if (sections.isEmpty()) return null
        val notes = (root["notes"] as? JsonPrimitive)?.content
        return PullListResult(sections, notes)
    }

    private fun parseItem(obj: JsonObject?): PullItem? {
        if (obj == null) return null
        val name = (obj["name"] as? JsonPrimitive)?.content ?: return null
        val qty = (obj["qty"] as? JsonPrimitive)?.doubleOrNull ?: 1.0

        val prices = mutableListOf<StorePrice>()
        (obj["prices"] as? JsonArray)?.forEach { el ->
            val po = el as? JsonObject ?: return@forEach
            val store = (po["store"] as? JsonPrimitive)?.content
            val p = (po["price"] as? JsonPrimitive)?.doubleOrNull
            if (store != null && p != null) prices += StorePrice(store, p)
        }
        // Tolerate the web prompt's hd/lw keys too.
        (obj["hd"] as? JsonPrimitive)?.doubleOrNull?.let { prices += StorePrice("Home Depot", it) }
        (obj["lw"] as? JsonPrimitive)?.doubleOrNull?.let { prices += StorePrice("Lowe's", it) }
        val deduped = prices.distinctBy { it.store }

        val explicitPrice = (obj["price"] as? JsonPrimitive)?.doubleOrNull
        val cheapest = deduped.minByOrNull { it.price }
        return PullItem(
            name = name,
            spec = (obj["spec"] as? JsonPrimitive)?.content,
            qty = qty,
            unit = (obj["unit"] as? JsonPrimitive)?.content,
            price = explicitPrice ?: cheapest?.price,
            bestStore = (obj["bestStore"] as? JsonPrimitive)?.content ?: cheapest?.store,
            prices = deduped,
            live = (obj["live"] as? JsonPrimitive)?.booleanOrNull ?: false,
        )
    }

    private fun JsonArray?.orEmptyArray(): JsonArray = this ?: JsonArray(emptyList())

    /** Joins the Anthropic content blocks' text from the /api/claude envelope. */
    private fun extractText(raw: String): String = runCatching {
        json.parseToJsonElement(raw).jsonObject["content"]?.jsonArray
            ?.joinToString("") { block ->
                runCatching { block.jsonObject["text"]?.jsonPrimitive?.content.orEmpty() }.getOrDefault("")
            }
            .orEmpty()
    }.getOrDefault("")

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
        You are a master electrician with 30 years of residential experience writing a material pull list for a supply run anywhere in the United States. You know exactly what each job needs, including the consumables everyone forgets (staples, wire nuts, connectors, straps, tape).

        TRUTHFULNESS — this matters more than anything else:
        - Only include a store in an item's "prices" and set "live": true if you ACTUALLY found that price in your web search. Never invent store names, SKUs, product names, or prices.
        - If you cannot find a real current price for an item, give a clearly-typical ESTIMATE, leave its "prices" empty, and set "live": false — never attribute an estimate to a specific store as if it were a real quote.
        - Every price is a live-searched estimate to confirm in the cart, NOT a guaranteed quote. Do not overstate certainty.

        RULES:
        1. Group materials BY SERVICE — one section per service line given.
        2. For each material: name (short, searchable — what you'd type into a store's search), spec (gauge/amperage/size detail), qty (number), unit (ea, ft, box, roll).
        3. MANDATORY LIVE PRICING via web search, localized to the job's area. Your memorized prices for copper wire, cable, panels, and breakers are YEARS out of date and too LOW — NEVER price those from memory. Before answering you MUST web-search CURRENT prices at the local PUBLIC big-box stores for that area — Home Depot AND Lowe's at minimum, plus Menards / Grainger / Ferguson where they serve that area — for: (a) every wire/cable coil or spool (NM-B/Romex, THHN, MC, UF); (b) every panel/load center; (c) every breaker, especially AFCI/GFCI/dual-function; (d) EV chargers, disconnects, and any item worth roughly ${'$'}25 or more. Prioritize the most expensive items first.
        4. For each item you actually found prices for, return a "prices" array of {"store": name, "price": number} for ONLY the stores you really searched, set "price" to the LOWEST, "bestStore" to that store, and "live": true. Commodity smalls (wire nuts, staples, straps, plates, boxes under ~${'$'}20) may use typical current prices biased slightly HIGH — leave their "prices" empty and "live": false. Do not fill in prices you did not find.
        5. Wire quantities in feet with 10-15% slack, rounded to purchasable amounts (25/50/100/250 ft).
        6. Skip materials for services marked [client supplies materials] but mention them in "notes".
        7. Consolidate shared consumables (wire nuts, staples, tape) into a final section "Consumables & Rough-In".
        8. In "notes" (1-3 sentences): state that these prices are live-searched estimates to confirm in the cart and roughly how current they are; flag anything client-supplied; name items better bought at a local electrical distributor than big-box (CED, Graybar, Rexel, Platt, WESCO, Border States, City Electric, etc. — they often beat big-box on wire and breakers but need a trade account); and any bulk-buy savings. If live prices were hard to find for this area, say so plainly.

        Respond ONLY with JSON, no markdown fences:
        {"sections":[{"service":"...","items":[{"name":"...","spec":"...","qty":1,"unit":"ea","price":0.00,"bestStore":"Home Depot","prices":[{"store":"Home Depot","price":0.00},{"store":"Lowe's","price":0.00}],"live":true}]}],"notes":"..."}
    """.trimIndent()

    companion object {
        private const val BASE_URL = "https://www.wirewaypro.com"
    }
}
