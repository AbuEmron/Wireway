package com.wirewaypro.app.domain.model

/**
 * One catalog service the AI takeoff proposed for a job. [serviceId] maps to
 * [com.wirewaypro.app.domain.catalog.Catalog]; the UI resolves label/amount.
 */
data class TakeoffSuggestion(
    val serviceId: String,
    val qty: Double,
    val variantIdx: Int,
    val clientBuys: Boolean,
    val reason: String?,
)

/** The parsed result of an AI takeoff analysis. */
data class TakeoffResult(
    val suggestions: List<TakeoffSuggestion>,
    val summary: String,
    val assumptions: List<String>,
)
