package com.wirewaypro.app.esign.consent

/**
 * The versioned Electronic Signature Consent copy (Screen 1 of the flow) —
 * verbatim from WIREWAY_ESIGN_CONSENT_FLOW.md, which is drafted to align with the
 * U.S. ESIGN Act (15 U.S.C. §7001) and UETA.
 *
 * IMPORTANT — this text is NOT legal advice and does not guarantee enforceability.
 * Blocks marked `[COUNSEL]` and the flow as a whole MUST be reviewed and approved
 * by a licensed attorney for each operating state before production use. State
 * rules vary (e.g. New York uses ESRA, not UETA; some transactions are excluded).
 *
 * The disclosure text is VERSIONED. Every signed record stores the [version] it
 * saw ([current] here), so we can always reproduce exactly what a given signer was
 * shown. Bump [VERSION] (and add a dated changelog note) whenever the copy changes;
 * never edit a shipped version's text in place.
 */
object ConsentDisclosures {

    /**
     * Bump on ANY change to the disclosure copy below. Format: date + revision.
     *  - 2026-07-06.1 — initial disclosures per WIREWAY_ESIGN_CONSENT_FLOW.md.
     */
    const val VERSION = "2026-07-06.1"

    val current: Disclosure = Disclosure(
        version = VERSION,
        header = "Before you sign",
        subhead = "A few things to know about signing electronically. Please read these — they protect you too.",
        sections = listOf(
            DisclosureSection(
                title = "Your signature is legally binding",
                body = "When you sign this document electronically, it carries the same legal weight as " +
                    "signing with a pen on paper. U.S. law (the federal ESIGN Act and your state's " +
                    "electronic-transactions law) treats a valid electronic signature as a real signature.",
            ),
            DisclosureSection(
                title = "Signing electronically is your choice",
                body = "You don't have to sign electronically. You're agreeing to do it this way because " +
                    "it's faster and gives you an instant copy. You can decline and ask for a paper " +
                    "document instead — see below.",
            ),
            DisclosureSection(
                title = "You can get a paper copy",
                body = "Before you sign, you may ask the contractor for a paper copy of this document to " +
                    "review or keep. After signing, you can also download or print your own copy anytime.",
            ),
            DisclosureSection(
                title = "You can change your mind about electronic delivery",
                body = "You can withdraw your consent to receive documents electronically at any time by " +
                    "contacting the contractor. Withdrawing consent applies going forward and won't undo " +
                    "a document you've already signed.",
            ),
            DisclosureSection(
                title = "What you'll need to read and keep these documents",
                body = "To view, download, and keep your documents electronically you'll need: a device " +
                    "with internet access, a current web browser or the Wireway app, an email account, " +
                    "and the ability to open PDF files. If you can read this screen and receive email, " +
                    "you're set.",
            ),
            DisclosureSection(
                title = "How your signed document is kept",
                body = "Once you sign, your document is sealed so it can't be quietly altered, and a unique " +
                    "fingerprint (a secure hash) is recorded so any later change would be detectable. Your " +
                    "signed copy and its record are stored securely, encrypted, and retained so you and the " +
                    "contractor can retrieve them later.",
            ),
            DisclosureSection(
                title = "What we record when you sign (and why)",
                body = "To make your signature trustworthy and hold up if it's ever questioned, we record: " +
                    "the date and time you signed, your name and email, your device and app version, your " +
                    "network (IP) address, the document's secure fingerprint, and a step-by-step log of the " +
                    "signing. We record this only to prove the signature is genuine and the document " +
                    "unchanged — not to track you elsewhere.",
            ),
        ),
        // Consent control — never pre-checked (affirmative act = evidence of intent).
        checkboxLabel = "I have read the above. I agree to sign and receive this document " +
            "electronically, and I understand my electronic signature is legally binding.",
        agreeButton = "Agree and continue",
        paperButton = "I'd rather use paper",
        // [COUNSEL] Confirm state-law citations, any paper-copy fee, withdrawal
        // consequences (ESIGN §101(c)), and the hardware/software statement
        // (ESIGN §101(c)(1)(C)(i)) before production use.
        counselNote = "Legal review required — not legal advice.",
    )

    data class Disclosure(
        val version: String,
        val header: String,
        val subhead: String,
        val sections: List<DisclosureSection>,
        val checkboxLabel: String,
        val agreeButton: String,
        val paperButton: String,
        val counselNote: String,
    )

    data class DisclosureSection(val title: String, val body: String)
}
