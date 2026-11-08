package com.grk.mpa.app.ui.utilities

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.grk.mpa.app.core.Compounding
import com.grk.mpa.app.core.InterestCalculator
import com.grk.mpa.app.core.InterestError
import com.grk.mpa.app.core.InterestInput
import com.grk.mpa.app.core.InterestOutcome
import com.grk.mpa.app.core.InterestType
import com.grk.mpa.app.core.RateUnit

class InterestCalculatorViewModel : ViewModel() {
    var principal by mutableStateOf("")
        private set
    var rate by mutableStateOf("")
        private set
    var rateUnit by mutableStateOf(RateUnit.RS_PER_100_MONTH)
        private set
    var years by mutableStateOf("")
        private set
    var months by mutableStateOf("")
        private set
    var days by mutableStateOf("")
        private set
    var type by mutableStateOf(InterestType.SIMPLE)
        private set
    var compounding by mutableStateOf(Compounding.MONTHLY)
        private set

    private fun decimal(text: String): Double? = text.trim().replace(',', '.').toDoubleOrNull()

    /** True while nothing has been typed yet, so the screen can show a prompt instead of an error. */
    val isEmpty: Boolean
        get() = principal.isBlank() && rate.isBlank() && years.isBlank() && months.isBlank() && days.isBlank()

    /** The result, or why the figures cannot be used. Null while nothing has been typed. */
    val outcome: InterestOutcome?
        get() {
            if (isEmpty) return null
            val p = decimal(principal) ?: return InterestOutcome.Invalid(InterestError.PRINCIPAL_INVALID)
            val r = decimal(rate) ?: return InterestOutcome.Invalid(InterestError.RATE_INVALID)
            return InterestCalculator.calculate(
                InterestInput(
                    principal = p, rate = r, rateUnit = rateUnit,
                    years = whole(years), months = whole(months), days = whole(days),
                    type = type, compounding = compounding
                )
            )
        }

    /** The typed rate as (per month, per year) percentages, shown under the rate box. Null if not a valid rate. */
    val rateEquivalent: Pair<Double, Double>?
        get() {
            val value = decimal(rate)?.takeIf { it >= 0 } ?: return null
            val annual = InterestCalculator.annualPercent(value, rateUnit)
            return InterestCalculator.monthlyPercent(annual) to annual
        }

    // Time boxes only take digits; an empty box counts as 0.
    private fun whole(text: String): Int = text.toIntOrNull() ?: 0

    fun onPrincipalChange(text: String) { principal = text.filter { it.isDigit() || it == '.' || it == ',' }.take(16) }
    fun onRateChange(text: String) { rate = text.filter { it.isDigit() || it == '.' || it == ',' }.take(10) }
    fun onYearsChange(text: String) { years = text.filter(Char::isDigit).take(3) }
    fun onMonthsChange(text: String) { months = text.filter(Char::isDigit).take(4) }
    fun onDaysChange(text: String) { days = text.filter(Char::isDigit).take(5) }
    fun onRateUnitChange(unit: RateUnit) { rateUnit = unit }
    fun onTypeChange(newType: InterestType) { type = newType }
    fun onCompoundingChange(value: Compounding) { compounding = value }

    fun clear() {
        principal = ""; rate = ""; years = ""; months = ""; days = ""
    }
}
