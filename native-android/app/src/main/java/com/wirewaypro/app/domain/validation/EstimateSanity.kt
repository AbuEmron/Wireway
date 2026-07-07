package com.wirewaypro.app.domain.validation

import com.wirewaypro.app.domain.catalog.Assemblies
import com.wirewaypro.app.domain.catalog.Catalog
import com.wirewaypro.app.domain.model.QuoteCatalogEntry
import com.wirewaypro.app.domain.model.QuoteCustomItem
import com.wirewaypro.app.domain.model.QuoteTotals

/**
 * One deterministic heads-up on an estimate: something missing or suspicious,
 * caught by a RULE (doctrine: rule-based checks off the assemblies/templates,
 * never an AI hunch). Flags are advisory — the contractor is in control; they
 * never block a save.
 */
data class SanityFlag(
    /** Stable rule id (rule name, plus the offending item where relevant). */
    val id: String,
    /** Field-friendly, one-line heads-up. */
    val message: String,
    /** The deterministic "why this fired" — shown so the tool shows its work. */
    val rule: String,
)

/**
 * Deterministic sanity-check validators for an estimate's line items and
 * totals. Pure domain — same inputs, same flags, offline, no AI anywhere.
 *
 * The permit rule is derived from the template library itself: the anchor
 * (first) line of every permit-carrying assembly marks work that ships with a
 * permit in every template — if that work appears with no permit line, flag it.
 */
object EstimateSanity {

    private val PERMIT_IDS = setOf("permit_service", "permit_general")

    /**
     * Work that NEVER ships without a permit in the template library (derived,
     * not hardcoded): the anchor (first) service of each permit-carrying
     * template, minus anything that also appears in a permit-free template —
     * if the library itself quotes a service without a permit somewhere, a bare
     * line of it isn't proof a permit is missing.
     */
    internal val permitAnchors: Set<String> = run {
        val permitFree = Assemblies.all
            .filter { asm -> asm.items.isNotEmpty() && asm.items.none { it.serviceId in PERMIT_IDS } }
            .flatMap { asm -> asm.items.map { it.serviceId } }
            .toSet()
        Assemblies.all
            .filter { asm -> asm.items.any { it.serviceId in PERMIT_IDS } }
            .mapNotNull { asm -> asm.items.firstOrNull { it.serviceId !in PERMIT_IDS }?.serviceId }
            .filterNot { it in permitFree }
            .toSet()
    }

    /** Single-service quantity at/above this is a probable fat-finger. */
    const val QTY_SUSPECT = 50.0

    /** Deposits above this many percent get a "check your state's cap" note. */
    const val DEPOSIT_SUSPECT_PCT = 50

    fun check(
        catalogEntries: List<QuoteCatalogEntry>,
        customItems: List<QuoteCustomItem>,
        totals: QuoteTotals,
        clientBuysAll: Boolean,
        depositPercent: Int? = null,
    ): List<SanityFlag> {
        val flags = mutableListOf<SanityFlag>()
        val ids = catalogEntries.filter { it.qty > 0.0 }.mapTo(HashSet()) { it.serviceId }

        // Permit-worthy work with no permit line ("200A panel with no permit").
        val anchorsPresent = ids.intersect(permitAnchors)
        if (anchorsPresent.isNotEmpty() && ids.intersect(PERMIT_IDS).isEmpty()) {
            val label = Catalog.service(anchorsPresent.first())?.label ?: anchorsPresent.first()
            flags += SanityFlag(
                id = "permit-missing",
                message = "No permit line — jobs with “$label” normally carry one.",
                rule = "Every job template that includes this work also includes an electrical permit line; this estimate has none.",
            )
        }

        // Probable fat-fingered quantity on a single service.
        catalogEntries.filter { it.qty >= QTY_SUSPECT }.forEach { e ->
            val label = Catalog.service(e.serviceId)?.label ?: e.serviceId
            flags += SanityFlag(
                id = "qty-suspect:${e.serviceId}",
                message = "${trim(e.qty)}× “$label” — double-check that quantity.",
                rule = "A single service at ${QTY_SUSPECT.toInt()}+ units is flagged for review.",
            )
        }

        // You're supplying materials, labor is priced, but materials are $0.
        if (!clientBuysAll && totals.totalLabor > 0.0 && totals.totalMaterial == 0.0) {
            flags += SanityFlag(
                id = "materials-zero",
                message = "Materials are \$0 but you're supplying them — supplier quote still pending?",
                rule = "Labor above \$0 with materials at \$0 while “client supplies materials” is off.",
            )
        }

        // A line with real hours but unpriced labor (template line never priced).
        customItems.filter { it.qty > 0.0 && it.laborHours > 0.0 && it.laborCost == 0.0 }.forEach { item ->
            flags += SanityFlag(
                id = "labor-unpriced:${item.label.trim().lowercase()}",
                message = "“${item.label}” carries ${trim(item.laborHours)} hrs but \$0 labor — price it before sending.",
                rule = "Custom line with laborHours > 0 and laborCost = 0.",
            )
        }

        // Deposit above half — several states cap advance deposits.
        if ((depositPercent ?: 0) > DEPOSIT_SUSPECT_PCT) {
            flags += SanityFlag(
                id = "deposit-high",
                message = "Deposit is $depositPercent% — some states cap deposits; confirm yours allows it.",
                rule = "Deposit percent above $DEPOSIT_SUSPECT_PCT.",
            )
        }

        // The same custom label twice — usually a duplicate, sometimes intended.
        customItems
            .filter { it.label.isNotBlank() }
            .groupBy { it.label.trim().lowercase() }
            .filterValues { it.size > 1 }
            .forEach { (key, dupes) ->
                flags += SanityFlag(
                    id = "duplicate-line:$key",
                    message = "“${dupes.first().label}” appears ${dupes.size}× — merge, or confirm they're separate scopes.",
                    rule = "Identical custom-line labels.",
                )
            }

        return flags
    }

    private fun trim(v: Double): String =
        if (v % 1.0 == 0.0) v.toLong().toString() else v.toString()
}
