package com.grk.mpa.app.core

import java.math.BigDecimal
import java.math.MathContext
import kotlin.math.abs

/** Formats numbers for display without trailing zeros or floating-point noise. */
object NumberFormatter {
    const val DEFAULT_SIGNIFICANT_DIGITS = 8

    fun format(value: Double, significantDigits: Int = DEFAULT_SIGNIFICANT_DIGITS): String {
        if (value.isNaN() || value.isInfinite()) return "-"
        if (value == 0.0) return "0"
        val rounded = BigDecimal(value).round(MathContext(significantDigits)).stripTrailingZeros()
        val magnitude = abs(value)
        return if (magnitude >= 1e-9 && magnitude < 1e15) rounded.toPlainString() else rounded.toString()
    }
}
