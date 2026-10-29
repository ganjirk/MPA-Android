package com.grk.mpa.app.core

/**
 * Decimal <-> binary conversion, including fractional values.
 * Ported from the legacy Kivy `dec2bin.py`. Invalid input returns null instead of
 * raising, so the UI can show a validation message.
 */
object BinaryConverter {
    private val binaryPattern = Regex("^-?[01]+(\\.[01]+)?$")
    private val decimalPattern = Regex("^-?\\d+(\\.\\d+)?$")

    /** Maximum number of binary digits produced after the point. */
    const val MAX_FRACTION_DIGITS = 10

    fun binaryToDecimal(input: String): Double? {
        val text = input.trim()
        if (!binaryPattern.matches(text)) return null
        val negative = text.startsWith("-")
        val unsigned = text.removePrefix("-")
        val whole = unsigned.substringBefore('.')
        val fraction = unsigned.substringAfter('.', "")

        var result = whole.fold(0.0) { acc, bit -> acc * 2 + (bit - '0') }
        var weight = 0.5
        for (bit in fraction) {
            result += (bit - '0') * weight
            weight /= 2
        }
        return if (negative) -result else result
    }

    fun decimalToBinary(input: String, maxFractionDigits: Int = MAX_FRACTION_DIGITS): String? {
        val text = input.trim()
        if (!decimalPattern.matches(text)) return null
        val negative = text.startsWith("-")
        val unsigned = text.removePrefix("-")
        val wholePart = unsigned.substringBefore('.').toBigInteger()
        val fractionDigits = unsigned.substringAfter('.', "")

        val whole = wholePart.toString(2)
        val sign = if (negative) "-" else ""
        if (fractionDigits.isEmpty()) return sign + whole

        var fraction = "0.$fractionDigits".toDouble()
        val bits = StringBuilder()
        while (fraction > 0.0 && bits.length < maxFractionDigits) {
            fraction *= 2
            if (fraction >= 1.0) {
                bits.append('1')
                fraction -= 1.0
            } else {
                bits.append('0')
            }
        }
        return if (bits.isEmpty()) sign + whole else "$sign$whole.$bits"
    }
}
