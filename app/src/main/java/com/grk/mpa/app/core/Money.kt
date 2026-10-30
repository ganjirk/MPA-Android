package com.grk.mpa.app.core

import kotlin.math.abs

/**
 * Rupee amounts are stored as whole paise (a Long) so that sums are exact.
 * Limits match the website: at most 10 digits, 2 of them after the point.
 */
object Money {
    const val MAX_PAISE = 9_999_999_999L
    const val SYMBOL = "₹"

    private val amountPattern = Regex("^(\\d{1,8})?(?:\\.(\\d{1,2}))?$")

    /** Parses what a person typed, such as `12`, `12.5` or `12,50`. Null if empty, invalid or not above zero. */
    fun parsePaise(text: String): Long? {
        val cleaned = text.trim().replace(',', '.')
        if (cleaned.isEmpty() || cleaned == ".") return null
        val match = amountPattern.matchEntire(cleaned) ?: return null
        val whole = match.groupValues[1].ifEmpty { "0" }.toLong()
        val fraction = match.groupValues[2].padEnd(2, '0').toLong()
        val paise = whole * 100 + fraction
        return paise.takeIf { it in 1..MAX_PAISE }
    }

    /** `1250` becomes `12.50`. No thousands separators, as on the website. */
    fun formatAmount(paise: Long): String {
        val sign = if (paise < 0) "-" else ""
        val absolute = abs(paise)
        return "$sign${absolute / 100}.${(absolute % 100).toString().padStart(2, '0')}"
    }

    /** `1250` becomes `₹12.50`. */
    fun format(paise: Long): String = SYMBOL + formatAmount(paise)
}
