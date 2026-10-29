package com.grk.mpa.app.ui.utilities

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.grk.mpa.app.core.BinaryConverter
import com.grk.mpa.app.core.NumberFormatter

enum class BinaryMode { DECIMAL_TO_BINARY, BINARY_TO_DECIMAL }

class BinaryConverterViewModel : ViewModel() {
    var input by mutableStateOf("")
        private set
    var mode by mutableStateOf(BinaryMode.DECIMAL_TO_BINARY)
        private set

    /** The converted text, or null when the input is empty or invalid for the current mode. */
    val result: String?
        get() = when (mode) {
            BinaryMode.DECIMAL_TO_BINARY -> BinaryConverter.decimalToBinary(input)
            BinaryMode.BINARY_TO_DECIMAL -> BinaryConverter.binaryToDecimal(input)?.let { NumberFormatter.format(it) }
        }

    val isInvalid: Boolean get() = input.isNotBlank() && result == null

    fun onInputChange(text: String) {
        input = text
    }

    fun onModeChange(newMode: BinaryMode) {
        mode = newMode
    }
}
