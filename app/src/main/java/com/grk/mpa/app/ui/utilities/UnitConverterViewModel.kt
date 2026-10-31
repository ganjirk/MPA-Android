package com.grk.mpa.app.ui.utilities

import androidx.compose.runtime.mutableStateMapOf
import androidx.lifecycle.ViewModel
import com.grk.mpa.app.core.ConversionUnit
import com.grk.mpa.app.core.NumberFormatter
import com.grk.mpa.app.core.UnitCategory
import com.grk.mpa.app.core.UnitConverter

/**
 * Keeps independent input state for every converter card. Typing in any unit field updates
 * the remaining fields in that category without clearing values in another category.
 */
class UnitConverterViewModel : ViewModel() {
    private val sourceUnits = mutableStateMapOf<String, ConversionUnit>()
    private val inputs = mutableStateMapOf<String, String>()

    fun onValueChange(category: UnitCategory, unit: ConversionUnit, text: String) {
        sourceUnits[category.id] = unit
        inputs[category.id] = text
    }

    fun displayValue(category: UnitCategory, unit: ConversionUnit): String {
        val source = sourceUnits[category.id] ?: return ""
        val input = inputs[category.id].orEmpty()
        if (unit === source) return input
        val value = input.trim().replace(',', '.').toDoubleOrNull()
            ?.takeIf { it.isFinite() } ?: return ""
        return NumberFormatter.format(UnitConverter.convert(value, source, unit))
    }

    fun isInvalid(category: UnitCategory): Boolean {
        val input = inputs[category.id].orEmpty()
        return input.isNotBlank() && input.trim().replace(',', '.').toDoubleOrNull()
            ?.isFinite() != true
    }

    fun isBelowMinimum(category: UnitCategory, unit: ConversionUnit): Boolean {
        if (sourceUnits[category.id] !== unit) return false
        val minimum = unit.minimum ?: return false
        val value = inputs[category.id].orEmpty().trim().replace(',', '.').toDoubleOrNull()
            ?.takeIf { it.isFinite() } ?: return false
        return value < minimum
    }
}
