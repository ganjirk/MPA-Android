package com.grk.mpa.app.ui.utilities

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.grk.mpa.app.core.LengthConverter
import com.grk.mpa.app.core.LengthUnit

class LengthConverterViewModel : ViewModel() {
    var input by mutableStateOf("")
        private set
    var fromUnit by mutableStateOf(LengthUnit.METRE)
        private set

    /** The parsed input, or null when it is empty or not a finite number. */
    val value: Double?
        get() = input.trim().replace(',', '.').toDoubleOrNull()?.takeIf { it.isFinite() }

    val isInvalid: Boolean get() = input.isNotBlank() && value == null

    /** The input converted into every unit, in [LengthUnit] order; empty when there is no valid input. */
    val results: List<Pair<LengthUnit, Double>>
        get() = value?.let { v -> LengthConverter.convertAll(v, fromUnit).toList() } ?: emptyList()

    fun onInputChange(text: String) {
        input = text
    }

    fun onUnitChange(unit: LengthUnit) {
        fromUnit = unit
    }
}
