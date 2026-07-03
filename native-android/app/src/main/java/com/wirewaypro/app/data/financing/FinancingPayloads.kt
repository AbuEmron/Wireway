package com.wirewaypro.app.data.financing

import com.wirewaypro.app.domain.financing.FinancingOffer
import com.wirewaypro.app.domain.financing.FinancingOfferStatus
import com.wirewaypro.app.domain.financing.FinancingSetup
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.longOrNull

/**
 * Pure parsing of the backend financing responses — split from the HTTP adapter
 * so the "never fabricate a number" contract is unit-testable: a missing
 * `asLowAsMonthly` stays null (the proposal shows nothing), it is never derived.
 * Field access uses `as? JsonPrimitive` throughout so nulls/odd shapes from the
 * backend can't crash the app (same hardening rule as QuoteDto).
 */
object FinancingPayloads {

    private val json = Json { ignoreUnknownKeys = true }

    fun parseSetup(raw: String): FinancingSetup {
        val o = json.parseToJsonElement(raw).jsonObject
        return FinancingSetup(
            connected = (o["connected"] as? JsonPrimitive)?.booleanOrNull ?: false,
            provider = (o["provider"] as? JsonPrimitive)?.contentOrNull,
            merchantName = (o["merchantName"] as? JsonPrimitive)?.contentOrNull,
            connectUrl = (o["connectUrl"] as? JsonPrimitive)?.contentOrNull,
        )
    }

    /** Throws when the application link is missing — an offer without a link is unusable. */
    fun parseOffer(estimateId: String, raw: String): FinancingOffer {
        val o = json.parseToJsonElement(raw).jsonObject
        val url = (o["applicationUrl"] as? JsonPrimitive)?.contentOrNull?.takeIf { it.isNotBlank() }
            ?: error("Financing response is missing the application link.")
        return FinancingOffer(
            estimateId = (o["estimateId"] as? JsonPrimitive)?.contentOrNull ?: estimateId,
            applicationUrl = url,
            status = FinancingOfferStatus.from((o["status"] as? JsonPrimitive)?.contentOrNull),
            asLowAsMonthly = (o["asLowAsMonthly"] as? JsonPrimitive)?.doubleOrNull?.takeIf { it > 0 },
            termMonths = (o["termMonths"] as? JsonPrimitive)?.intOrNull?.takeIf { it > 0 },
            updatedAt = (o["updatedAt"] as? JsonPrimitive)?.longOrNull,
        )
    }
}
