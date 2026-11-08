package com.grk.mpa.app.core

import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.pow

/** How a rate is quoted. "Rs.2 interest" means Rs.2 a month on every Rs.100, which is 2% a month. */
enum class RateUnit { RS_PER_100_MONTH, PERCENT_MONTH, PERCENT_YEAR }

enum class InterestType { SIMPLE, COMPOUND }

/** How often compound interest is added to the amount. */
enum class Compounding(val perYear: Int) { MONTHLY(12), QUARTERLY(4), HALF_YEARLY(2), YEARLY(1) }

data class InterestInput(
    val principal: Double,
    val rate: Double,
    val rateUnit: RateUnit,
    val years: Int,
    val months: Int,
    val days: Int,
    val type: InterestType,
    val compounding: Compounding = Compounding.MONTHLY
)

enum class InterestError {
    PRINCIPAL_INVALID, PRINCIPAL_TOO_LARGE, RATE_INVALID, RATE_TOO_HIGH, TIME_NEGATIVE, TIME_ZERO, TIME_TOO_LONG
}

/** One row of the year-by-year table. `partial` marks a last year that is not complete. */
data class YearRow(val year: Int, val partial: Boolean, val interest: Double, val total: Double)

data class InterestResult(
    val principal: Double,
    val interest: Double,
    val total: Double,
    val annualPercent: Double,
    val monthlyPercent: Double,
    /** What the yearly rate works out to once interest is added to the amount (same as annual for simple). */
    val effectiveAnnualPercent: Double,
    val years: Double,
    val type: InterestType,
    val rows: List<YearRow>
)

sealed interface InterestOutcome {
    data class Ok(val result: InterestResult) : InterestOutcome
    data class Invalid(val error: InterestError) : InterestOutcome
}

/** Same rules and figures as the website's Interest Calculator. */
object InterestCalculator {
    const val MAX_PRINCIPAL = 1e12
    const val MAX_YEARS = 100
    const val MAX_ANNUAL_PERCENT = 1200.0

    fun round2(x: Double): Double = Math.floor((x + Math.ulp(x)) * 100 + 0.5) / 100

    /** A month counts as 1/12 of a year and a day as 1/365. */
    fun toYears(years: Int, months: Int, days: Int): Double = years + months / 12.0 + days / 365.0

    fun annualPercent(rate: Double, unit: RateUnit): Double = when (unit) {
        RateUnit.PERCENT_YEAR -> rate
        RateUnit.PERCENT_MONTH, RateUnit.RS_PER_100_MONTH -> rate * 12
    }

    fun monthlyPercent(annual: Double): Double = annual / 12

    private fun accrued(principal: Double, annual: Double, years: Double, type: InterestType, c: Compounding): Double {
        val r = annual / 100
        if (type == InterestType.SIMPLE) return principal * r * years
        val n = c.perYear
        return principal * ((1 + r / n).pow(n * years) - 1)
    }

    fun calculate(input: InterestInput): InterestOutcome {
        val principal = input.principal
        if (!principal.isFinite() || principal <= 0) return InterestOutcome.Invalid(InterestError.PRINCIPAL_INVALID)
        if (principal > MAX_PRINCIPAL) return InterestOutcome.Invalid(InterestError.PRINCIPAL_TOO_LARGE)
        if (!input.rate.isFinite() || input.rate < 0) return InterestOutcome.Invalid(InterestError.RATE_INVALID)
        if (input.years < 0 || input.months < 0 || input.days < 0) {
            return InterestOutcome.Invalid(InterestError.TIME_NEGATIVE)
        }

        val annual = annualPercent(input.rate, input.rateUnit)
        if (annual > MAX_ANNUAL_PERCENT) return InterestOutcome.Invalid(InterestError.RATE_TOO_HIGH)

        val years = toYears(input.years, input.months, input.days)
        if (years <= 0) return InterestOutcome.Invalid(InterestError.TIME_ZERO)
        if (years > MAX_YEARS) return InterestOutcome.Invalid(InterestError.TIME_TOO_LONG)

        val interest = round2(accrued(principal, annual, years, input.type, input.compounding))
        val whole = Math.ceil(years - 1e-9).toInt()
        val rows = (1..whole).map { k ->
            val t = minOf(k.toDouble(), years)
            val i = round2(accrued(principal, annual, t, input.type, input.compounding))
            YearRow(k, t < k, i, round2(principal + i))
        }
        val effective = if (input.type == InterestType.COMPOUND) {
            val n = input.compounding.perYear
            ((1 + annual / 100 / n).pow(n) - 1) * 100
        } else annual

        return InterestOutcome.Ok(
            InterestResult(
                principal = round2(principal),
                interest = interest,
                total = round2(principal + interest),
                annualPercent = annual,
                monthlyPercent = monthlyPercent(annual),
                effectiveAnnualPercent = effective,
                years = years,
                type = input.type,
                rows = rows
            )
        )
    }
}

/** Rupee text with Indian digit grouping, for example ₹12,34,567.00. */
object IndianMoney {
    fun format(amount: Double): String {
        val fixed = BigDecimal(amount).setScale(2, RoundingMode.HALF_UP).abs().toPlainString()
        val whole = fixed.substringBefore('.')
        val fraction = fixed.substringAfter('.')
        val grouped = if (whole.length <= 3) whole else {
            val head = whole.dropLast(3).reversed().chunked(2).joinToString(",").reversed()
            "$head,${whole.takeLast(3)}"
        }
        val sign = if (amount < 0 && fixed != "0.00") "-" else ""
        return "$sign${Money.SYMBOL}$grouped.$fraction"
    }

    /** `24` becomes `24%`, `12.6825` becomes `12.68%`. */
    fun percent(value: Double): String {
        val text = BigDecimal(value).setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()
        return "$text%"
    }
}
