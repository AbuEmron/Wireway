package com.wirewaypro.app.domain.voice

/**
 * The pure, framework-free model of a live dictation. The Android
 * [android.speech.SpeechRecognizer] streams two kinds of text — interim
 * *partial* hypotheses that keep changing, and *final* segments that lock in
 * once it hears a pause. This holds the locked-in text plus the current partial
 * so the UI can render "what's been said so far" without any Android types,
 * which makes the accumulation logic unit-testable.
 *
 * There is no AI in this path — it's deterministic capture. The electrician
 * always edits the result before it becomes scope text.
 */
data class VoiceTranscript(
    /** Segments the recognizer has committed, joined with single spaces. */
    val finalized: String = "",
    /** The in-flight hypothesis for the current utterance (not yet committed). */
    val partial: String = "",
) {
    /** What the user reads live: finalized text with the partial trailing it. */
    val display: String
        get() = listOf(finalized, partial)
            .filter { it.isNotBlank() }
            .joinToString(" ")
            .trim()

    val isEmpty: Boolean get() = display.isEmpty()

    /** Replace the in-flight hypothesis (idempotent — partials keep arriving). */
    fun withPartial(text: String): VoiceTranscript = copy(partial = text.trim())

    /**
     * Commit a recognized segment: append it to [finalized] and clear the
     * partial. Blank segments are ignored so a dropped result never adds space.
     */
    fun commitFinal(text: String): VoiceTranscript {
        val seg = text.trim()
        if (seg.isEmpty()) return copy(partial = "")
        val joined = if (finalized.isBlank()) seg else "$finalized $seg"
        return VoiceTranscript(finalized = joined, partial = "")
    }

    companion object {
        /**
         * Merge dictated text into an existing field. Voice is *additive* — it
         * appends to whatever the electrician already typed rather than
         * clobbering it. An empty field takes the dictation as-is; a non-empty
         * one gets it on a new line so the typed path is never lost.
         */
        fun append(existing: String, dictated: String): String {
            val add = dictated.trim()
            if (add.isEmpty()) return existing
            val base = existing.trimEnd()
            return if (base.isEmpty()) add else "$base\n$add"
        }
    }
}
