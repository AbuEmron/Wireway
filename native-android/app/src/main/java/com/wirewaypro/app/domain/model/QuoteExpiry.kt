package com.wirewaypro.app.domain.model

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Whether an estimate is nearing (or past) the end of its validity window, so the
 * contractor can follow up before a live bid goes cold. Pure + offline — derived
 * from the quote's creation date and status, no network.
 *
 * The proposal PDF states estimates are valid for [VALID_DAYS] days; this mirrors
 * that. Only OPEN estimates are considered — invoices and already-decided quotes
 * (accepted / paid / declined) never show an expiry.
 */
object QuoteExpiry {

    /** Validity window shown on proposals ("valid for 30 days from the date above"). */
    const val VALID_DAYS = 30L

    /** Days-left threshold at or under which an estimate counts as "expiring soon". */
    const val SOON_DAYS = 7L

    enum class Level { EXPIRING, EXPIRED }

    data class Status(val level: Level, val daysLeft: Long, val label: String)

    /** Statuses that mean the estimate is settled — no follow-up needed. */
    private val CLOSED = setOf(
        "accepted", "paid", "declined", "rejected", "cancelled", "canceled", "void", "expired",
    )

    /**
     * Follow-up status for an estimate, or null when none is warranted: an invoice,
     * a settled quote, a still-fresh estimate (more than [SOON_DAYS] left), or an
     * unparseable date. [today] is injected for testability.
     */
    fun of(quote: QuoteSummary, today: LocalDate): Status? {
        if (quote.isInvoice) return null
        val status = quote.status?.lowercase()?.trim()
        if (status != null && status in CLOSED) return null
        val created = runCatching { LocalDate.parse(quote.createdAt?.take(10)) }.getOrNull() ?: return null

        val daysLeft = ChronoUnit.DAYS.between(today, created.plusDays(VALID_DAYS))
        return when {
            daysLeft < 0 -> Status(
                level = Level.EXPIRED,
                daysLeft = daysLeft,
                label = if (daysLeft == -1L) "Expired yesterday" else "Expired ${-daysLeft}d ago",
            )
            daysLeft <= SOON_DAYS -> Status(
                level = Level.EXPIRING,
                daysLeft = daysLeft,
                label = when (daysLeft) {
                    0L -> "Expires today"
                    1L -> "Expires tomorrow"
                    else -> "Expires in ${daysLeft}d"
                },
            )
            else -> null
        }
    }
}
