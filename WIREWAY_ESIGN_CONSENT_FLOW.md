# Wireway Pro — Electronic Signature Consent Flow (copy + spec)

> **LEGAL REVIEW REQUIRED — NOT LEGAL ADVICE.** This is original UX copy drafted to align with the U.S. ESIGN Act (15 U.S.C. §7001) and UETA to the fullest practical extent. It is **not** legal advice and does not guarantee enforceability. Every block marked `[COUNSEL]` and the flow as a whole must be reviewed and approved by a licensed attorney for the states you operate in before production use. State rules vary (e.g. New York uses ESRA, not UETA; some transactions are excluded from e-sign entirely).

_Author: drafted 2026-07-06 for the Wireway own-e-signature module. Written in plain English a homeowner can understand, professional, evidentiary-first. Original wording — not copied from any provider._

---

## The flow (order matters for enforceability)

1. **Electronic Signature Consent** — shown BEFORE anything is signed. Affirmative, unchecked-by-default consent + disclosures.
2. **Review the document** — the signer can read/scroll/download the full proposal first.
3. **Final Signature Confirmation** — the last screen before the pen touches; restates what they're signing and the amount.
4. **Sign** — draw or type.
5. **Completion Certificate** — appended to the sealed PDF; both parties get a copy.

The signer must be able to **decline and get a paper copy at every step** before signing. Consent is captured as an affirmative act (tapping "I agree"), never pre-checked.

---

## Screen 1 — Electronic Signature Consent

**Header:** Before you sign
**Sub:** A few things to know about signing electronically. Please read these — they protect you too.

### Your signature is legally binding
When you sign this document electronically, it carries the same legal weight as signing with a pen on paper. U.S. law (the federal ESIGN Act and your state's electronic-transactions law) treats a valid electronic signature as a real signature. `[COUNSEL: confirm state-law citation for each operating state.]`

### Signing electronically is your choice
You don't have to sign electronically. You're agreeing to do it this way because it's faster and gives you an instant copy. You can decline and ask for a paper document instead — see below.

### You can get a paper copy
Before you sign, you may ask [Contractor Name] for a paper copy of this document to review or keep. `[COUNSEL: state whether any fee applies; ESIGN requires disclosing any fee for paper copies.]` After signing, you can also download or print your own copy anytime (see the FAQ).

### You can change your mind about electronic delivery
You can withdraw your consent to receive documents electronically at any time by contacting [Contractor Name] at [email/phone]. Withdrawing consent applies going forward and won't undo a document you've already signed. `[COUNSEL: describe consequences/fees, if any, of withdrawal per ESIGN §101(c).]`

### What you'll need to read and keep these documents
To view, download, and keep your documents electronically you'll need: a device with internet access, a current web browser or the Wireway app, an email account, and the ability to open PDF files. If you can read this screen and receive email, you're set. `[COUNSEL: confirm hardware/software statement satisfies ESIGN §101(c)(1)(C)(i).]`

### How your signed document is kept
Once you sign, your document is sealed so it can't be quietly altered, and a unique fingerprint (a secure hash) is recorded so any later change would be detectable. Your signed copy and its record are stored securely, encrypted, and retained so you and [Contractor Name] can retrieve them later.

### What we record when you sign (and why)
To make your signature trustworthy and hold up if it's ever questioned, we record: the date and time you signed, your name and email, your device and browser type, your network (IP) address, the document's secure fingerprint, and a step-by-step log of the signing. We record this **only to prove the signature is genuine and the document unchanged** — not to track you elsewhere. See our Privacy Notice: [link]. `[COUNSEL + PRIVACY: confirm against your privacy policy and applicable state privacy law.]`

**Consent control (unchecked by default):**
☐ I have read the above. I agree to sign and receive this document electronically, and I understand my electronic signature is legally binding.

**Buttons:** `Agree and continue` (disabled until checked) · `I'd rather use paper`

---

## Screen 2 — Final Signature Confirmation

**Header:** You're about to sign
**Sub:** This is the last step. Take a second to confirm.

- **Document:** [Proposal #1042 — Panel upgrade + kitchen circuits]
- **From:** [Contractor / company name]
- **Total:** [$4,850.00]
- **You're signing as:** [Signer name] · [email]
- **Date:** [auto, shown]

> By tapping "Sign now," you're placing your legal signature on this document.

**Buttons:** `Sign now` · `Go back and review`

(Then: signature pad — "Draw your signature" with a "Type it instead" toggle.)

---

## Screen 3 — Completion Certificate (appended to the sealed PDF)

**Title:** Certificate of Electronic Signature
**Sub:** This page is part of the signed document and records how it was signed.

| Field | Value |
|---|---|
| Document | [Proposal #1042 — title] |
| Document fingerprint (SHA-256) | [64-char hash] |
| Signer | [Name] · [email] |
| Consent to sign electronically | Given [date/time, timezone] |
| Signed | [date/time, timezone] |
| Signature method | Drawn on device / Typed |
| Identity check | Email on file [+ one-time code, when used] |
| Signed on | [device], Wireway [app version] |
| Network address | [IP] |
| Sealed by | Wireway electronic signature |

**Footer:** This certificate and the document's fingerprint let anyone confirm the document hasn't changed since it was signed. To verify, open it in Wireway and choose "Verify integrity." Questions? Contact [Contractor Name] at [contact]. `[COUNSEL: approve certificate wording + retention statement.]`

---

## Customer-facing FAQ

**Is this signature legally binding?**
Yes. Under U.S. law an electronic signature you make on purpose, and agree to, is as binding as a pen-and-paper one. This flow is built to meet those requirements. `[COUNSEL: confirm phrasing.]`

**Can this document be changed after I sign?**
No — not without it being obvious. When you sign, the document is sealed and given a unique fingerprint. If even one character changed afterward, the fingerprint wouldn't match and the change would show. Your signed copy is locked in.

**How is my signature protected?**
Your signature image is encrypted where it's stored, the document travels over a secure connection, and the sealed file plus its record are kept so they can be produced later. Only you and [Contractor Name] get the signed copy.

**What information is recorded?**
The date and time, your name and email, your device and browser, your network (IP) address, the document's fingerprint, and a log of the signing steps. It's recorded to prove the signature is real and the document unchanged — nothing more.

**Can I receive a paper copy?**
Yes. You can ask for a paper copy before you sign, and you can download or print your own copy anytime after. `[COUNSEL: note any fee.]`

**How do I get another copy later?**
Open the document in the Wireway app or ask [Contractor Name] to resend it — your signed copy and its certificate are stored and can be retrieved at any time.

---

## Design notes (for the build)
- Match the S+ design language: calm, high-contrast, one primary action per screen, generous spacing, trust cues (lock/shield/seal icons used sparingly and honestly).
- Consent checkbox is **never pre-checked**; the primary button stays disabled until it's checked (affirmative act = evidence of intent).
- "I'd rather use paper" is always visible pre-signature — the voluntary choice must be real, not buried.
- Capture the consent event (timestamp + which disclosure version) into the audit trail, separate from the signature event.
- Version this disclosure text; store the version shown on each signed record so you can always show exactly what a given signer saw.
- Nothing here claims "certified," "notarized," or "guaranteed" — honest language only.
