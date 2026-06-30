package com.wirewaypro.app.ui.takeoff

/**
 * The two headline AI estimating entry points. Both run the same
 * [com.wirewaypro.app.data.ai.AiTakeoffService]; they differ only in framing so
 * each is a distinct, clearly-labeled destination:
 *  - [QUOTE_BUILDER] leads with a plain-English job description.
 *  - [TAKEOFF] leads with snapping/uploading a photo or PDF of the plan.
 */
enum class AiEstimateMode(
    val title: String,
    val cardTitle: String,
    val cardSubtitle: String,
    val promptLabel: String,
    val promptPlaceholder: String,
    val attachHint: String,
) {
    QUOTE_BUILDER(
        title = "AI Quote Builder",
        cardTitle = "Describe the job",
        cardSubtitle = "Tell me what the job is in plain English — I'll build the estimate.",
        promptLabel = "Scope of work",
        promptPlaceholder = "e.g. Install 12 recessed lights, 3 dimmers, and a dedicated 20A office circuit",
        attachHint = "Have a plan? Add a photo or PDF (optional)",
    ),
    TAKEOFF(
        title = "AI Takeoff",
        cardTitle = "Snap or upload the plan",
        cardSubtitle = "Add a photo or PDF of the plan/scope — I'll read it and build the estimate.",
        promptLabel = "Notes (optional)",
        promptPlaceholder = "Anything the plan doesn't show — panel location, finishes, access…",
        attachHint = "Attach a photo or PDF of the plan",
    ),
}
