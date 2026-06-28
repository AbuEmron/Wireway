package com.wirewaypro.app.domain.model

import kotlin.math.roundToLong

/**
 * Money rounding shared by the quote/draw math. Mirrors the web app's
 * `round2 = (n) => Math.round((Number(n)||0) * 100) / 100` so native totals match
 * the React app to the cent.
 */
object MoneyMath {
    fun round2(value: Double): Double {
        if (value.isNaN() || value.isInfinite()) return 0.0
        return (value * 100).roundToLong() / 100.0
    }
}
