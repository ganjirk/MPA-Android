package com.grk.mpa.app.ui.utilities

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.grk.mpa.app.core.ConversionUnit
import com.grk.mpa.app.core.NumberFormatter
import com.grk.mpa.app.core.UnitCategories
import com.grk.mpa.app.core.UnitCategory
import com.grk.mpa.app.core.UnitConverter

/**
 * Like the website: every unit of the chosen category has its own field. Typing in one field
 * converts into all the others.
 */
class UnitConverterViewModel : ViewModel() {
    var category by mutableStateOf(UnitCategories.all.first())
        private set

    /** The unit the user last typed in, or null before they have typed anything. */
    var sourceUnit by mutableStateOf<ConversionUnit?>(null)
        private set
    var input by mutableStateOf("")
        private set

    private val parsedValue: Double?
        get() = input.trim().replace(',', '.').toDoubleOrNull()?.takeIf { it.isFinite() }

    val isInvalid: Boolean get() = sourceUnit != null && input.isNotBlank() && parsedValue == null

    fun onCategoryChange(newCategory: UnitCategory) {
        category = newCategory
        sourceUnit = null
        input = ""
    }

    fun onValueChange(unit: ConversionUnit, text: String) {
        sourceUnit = unit
        input = text
    }

    /** What to show in the field of [unit]: the typed text in the source field, the conversion elsewhere. */
    fun displayValue(unit: ConversionUnit): String {
        val source = sourceUnit ?: return ""
        if (unit === source) return input
        val value = parsedValue ?: return ""
        return NumberFormatter.format(UnitConverter.convert(value, source, unit))
    }
}
