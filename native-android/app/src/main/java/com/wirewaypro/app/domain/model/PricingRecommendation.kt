package com.wirewaypro.app.domain.model

/**
 * The AI's location-aware pricing suggestion for a job. It is ONLY a starting
 * point — the contractor always confirms or overrides with their own number.
 */
data class PricingRecommendation(
    val mode: RateMode,              // recommended approach: hourly or flat
    val recommendedRate: Double?,    // suggested hourly $/hr (null when flat)
    val recommendedTotal: Double?,   // suggested all-in total
    val lowTotal: Double?,           // sensible low end of the range
    val highTotal: Double?,          // sensible high end of the range
    val areaContext: String,         // 1-2 sentences on the local market + the basis for the figures
    val reasoning: String,           // 1-2 sentences on why this fits
    val confidence: String? = null,  // "high" | "medium" | "low" — how solid the local data was
)
