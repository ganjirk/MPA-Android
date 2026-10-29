package com.grk.mpa.app.ui.utilities

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BinaryConverterViewModelTest {
    @Test
    fun decimalToBinaryIsTheDefaultMode() {
        val vm = BinaryConverterViewModel()
        assertEquals(BinaryMode.DECIMAL_TO_BINARY, vm.mode)
        vm.onInputChange("10")
        assertEquals("1010", vm.result)
    }

    @Test
    fun binaryToDecimalFormatsTheResult() {
        val vm = BinaryConverterViewModel()
        vm.onModeChange(BinaryMode.BINARY_TO_DECIMAL)
        vm.onInputChange("1010.1")
        assertEquals("10.5", vm.result)
    }

    @Test
    fun invalidInputForTheModeIsFlagged() {
        val vm = BinaryConverterViewModel()
        vm.onModeChange(BinaryMode.BINARY_TO_DECIMAL)
        vm.onInputChange("102")
        assertNull(vm.result)
        assertTrue(vm.isInvalid)
    }

    @Test
    fun switchingModeRevalidatesTheSameInput() {
        val vm = BinaryConverterViewModel()
        vm.onInputChange("12")
        assertEquals("1100", vm.result)
        vm.onModeChange(BinaryMode.BINARY_TO_DECIMAL)
        assertTrue(vm.isInvalid)
    }

    @Test
    fun emptyInputIsNotAnError() {
        val vm = BinaryConverterViewModel()
        assertNull(vm.result)
        assertFalse(vm.isInvalid)
    }
}
