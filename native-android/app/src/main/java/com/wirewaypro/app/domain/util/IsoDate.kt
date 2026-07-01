package com.wirewaypro.app.domain.util

import java.time.YearMonth

/**
 * Normalizes date strings to a valid ISO `yyyy-MM-dd` before they are written to
 * Postgres. The `date` columns (invoice_due_date, scheduled_date, draw due_date,
 * expense_date, …) reject anything that isn't a real calendar day, so this is the
 * single choke point that guarantees every date we send the backend is valid.
 *
 * Why this exists: the live Postgres logs showed saves failing with
 * `date/time field value out of range: "2026-06-31"` (impossible days in 30-day
 * months) and `"30/06/2026"` (a dd/MM/yyyy string). Material date pickers emit
 * good ISO, but values can also arrive from AI/OCR extraction or from stale rows
 * re-saved on edit — so normalization belongs at the data-layer write boundary,
 * not in any one screen.
 *
 * Handling:
 *  - ISO `yyyy-MM-dd` (a trailing time/zone suffix is ignored — only the date is kept)
 *  - slash formats `dd/MM/yyyy`, `MM/dd/yyyy`, and `yyyy/MM/dd`
 *  - impossible days (e.g. `2026-06-31`, `2026-02-30`) are clamped to the last
 *    valid day of that month rather than rejected, so the user's save still goes
 *    through with a sensible date instead of failing.
 */
object IsoDate {

    private val ISO = Regex("""^(\d{4})-(\d{1,2})-(\d{1,2})""")
    private val SLASH = Regex("""^(\d{1,4})/(\d{1,2})/(\d{1,4})$""")

    /**
     * @return a valid `yyyy-MM-dd` string, or `null` when [raw] is blank or can't
     * be parsed as a date. Callers that write a NOT NULL column should supply a
     * fallback (e.g. today) for the null case.
     */
    fun normalizeOrNull(raw: String?): String? {
        val s = raw?.trim().orEmpty()
        if (s.isEmpty()) return null

        ISO.find(s)?.destructured?.let { (y, m, d) ->
            return build(y.toInt(), m.toInt(), d.toInt())
        }

        SLASH.matchEntire(s)?.destructured?.let { (a, b, c) ->
            val ai = a.toInt(); val bi = b.toInt(); val ci = c.toInt()
            // yyyy/MM/dd when the first group is a 4-digit year.
            if (a.length == 4) return build(ai, bi, ci)
            // Otherwise the year is last and the first two are day/month in some
            // order. A value > 12 can only be the day; if both are ambiguous we
            // assume day/month (the format the bad live data used).
            val (day, month) = when {
                ai > 12 -> ai to bi   // 30/06/2026 -> day=30, month=06
                bi > 12 -> bi to ai   // 06/30/2026 -> day=30, month=06
                else -> ai to bi      // ambiguous -> day/month
            }
            return build(ci, month, day)
        }

        return null
    }

    /** Builds `yyyy-MM-dd`, clamping [month] to 1..12 and [day] to that month's last valid day. */
    private fun build(year: Int, month: Int, day: Int): String? {
        if (year < 1) return null
        val m = month.coerceIn(1, 12)
        val lastDay = YearMonth.of(year, m).lengthOfMonth()
        val d = day.coerceIn(1, lastDay)
        return "%04d-%02d-%02d".format(year, m, d)
    }
}
