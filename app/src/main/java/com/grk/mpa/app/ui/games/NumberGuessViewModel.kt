package com.grk.mpa.app.ui.games

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.grk.mpa.app.core.GuessResult
import com.grk.mpa.app.core.NumberGuessGame

enum class GuessFeedback { NONE, INVALID, OUT_OF_RANGE, TOO_LOW, TOO_HIGH, CORRECT }

class NumberGuessViewModel(private val game: NumberGuessGame) : ViewModel() {
    constructor() : this(NumberGuessGame())

    var input by mutableStateOf("")
        private set
    var feedback by mutableStateOf(GuessFeedback.NONE)
        private set
    var attempts by mutableIntStateOf(0)
        private set
    var isSolved by mutableStateOf(false)
        private set

    val range: IntRange get() = game.range

    fun onInputChange(text: String) {
        input = text.filter { it.isDigit() }.take(MAX_INPUT_LENGTH)
    }

    fun submit() {
        if (isSolved) return
        val value = input.toIntOrNull()
        if (value == null) {
            feedback = GuessFeedback.INVALID
            return
        }
        feedback = when (game.guess(value)) {
            GuessResult.OUT_OF_RANGE -> GuessFeedback.OUT_OF_RANGE
            GuessResult.TOO_LOW -> GuessFeedback.TOO_LOW
            GuessResult.TOO_HIGH -> GuessFeedback.TOO_HIGH
            GuessResult.CORRECT -> GuessFeedback.CORRECT
        }
        attempts = game.attempts
        isSolved = game.isSolved
    }

    fun reset() {
        game.reset()
        input = ""
        feedback = GuessFeedback.NONE
        attempts = 0
        isSolved = false
    }

    private companion object {
        const val MAX_INPUT_LENGTH = 4
    }
}
