package com.wirewaypro.app.ui.util

import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.text.NumberFormat
import java.util.Locale

/** Small, dependency-free formatting helpers shared across screens. */
object Format {

    private val currency: NumberFormat = NumberFormat.getCurrencyInstance(Locale.US)
    private val number1: NumberFormat = NumberFormat.getNumberInstance(Locale.US).apply { maximumFractionDigits = 1 }
    private val dateOut = DateTimeFormatter.ofPattern("MMM d, yyyy")
    private val timeOut = DateTimeFormatter.ofPattern("h:mm a")

    /** "$1,234.56", or an em-dash when null. */
    fun money(value: Double?): String =
        value?.let { currency.format(it) } ?: "—"

    /** "1,234.5" (one decimal), or "0" when null. */
    fun miles(value: Double?): String =
        value?.let { number1.format(it) } ?: "0"

    /** "2.5" (one decimal), or "0" when null. Used for logged labor hours. */
    fun hours(value: Double?): String =
        value?.let { number1.format(it) } ?: "0"

    /** Accepts "yyyy-MM-dd" or a full ISO timestamp; returns "Jun 28, 2026". */
    fun date(iso: String?): String {
        if (iso.isNullOrBlank()) return "—"
        return runCatching {
            LocalDate.parse(iso.take(10)).format(dateOut)
        }.getOrDefault(iso)
    }

    /** Accepts "HH:mm[:ss]"; returns "3:00 PM". Blank input yields null. */
    fun time(value: String?): String? {
        if (value.isNullOrBlank()) return null
        return runCatching {
            LocalTime.parse(value.take(8).removeSuffix(":")).format(timeOut)
        }.getOrNull()
    }

    /** Title-cases a snake/lower status, e.g. "in_progress" -> "In progress". */
    fun status(value: String?): String {
        if (value.isNullOrBlank()) return "—"
        return value.replace('_', ' ').replaceFirstChar { it.uppercase() }
    }
}
