package com.grk.mpa.app.ui.games

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grk.mpa.app.core.GuessLevel
import com.grk.mpa.app.core.GuessRecord
import com.grk.mpa.app.core.GuessResult
import com.grk.mpa.app.core.GuessStats
import com.grk.mpa.app.core.GuessSummary
import com.grk.mpa.app.core.NumberGuessGame
import com.grk.mpa.app.data.GuessStatsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

enum class GuessFeedback { NONE, INVALID, OUT_OF_RANGE, TOO_LOW, TOO_HIGH, CORRECT, LOST }

/**
 * @param gameFactory creates a new game for a level; tests use it to fix the secret number.
 * @param coroutineScope scope for storage calls; null means [viewModelScope].
 */
class NumberGuessViewModel(
    private val statsRepository: GuessStatsRepository,
    private val gameFactory: (GuessLevel) -> NumberGuessGame = { it.newGame() },
    private val coroutineScope: CoroutineScope? = null,
    private val clock: () -> Long = System::currentTimeMillis
) : ViewModel() {
    private val scope: CoroutineScope get() = coroutineScope ?: viewModelScope

    private var game: NumberGuessGame = gameFactory(GuessLevel.DEFAULT)
    private var records: List<GuessRecord> = emptyList()

    var level by mutableStateOf(GuessLevel.DEFAULT)
        private set
    var input by mutableStateOf("")
        private set
    var feedback by mutableStateOf(GuessFeedback.NONE)
        private set
    var attempts by mutableIntStateOf(0)
        private set
    var chancesLeft by mutableIntStateOf(GuessLevel.DEFAULT.chances)
        private set
    var isFinished by mutableStateOf(false)
        private set

    /** The secret number, shown once the game is over. */
    var revealedTarget by mutableStateOf<Int?>(null)
        private set
    var summary by mutableStateOf(GuessSummary.EMPTY)
        private set

    val maxNumber: Int get() = level.maxNumber
    val isLastChance: Boolean get() = !isFinished && chancesLeft == 1

    init {
        scope.launch {
            records = statsRepository.load()
            summary = GuessStats.summarize(records)
        }
    }

    fun onInputChange(text: String) {
        input = text.filter { it.isDigit() }.take(MAX_INPUT_LENGTH)
    }

    /** Changing the level always starts a new game, as on the website. */
    fun onLevelChange(newLevel: GuessLevel) {
        level = newLevel
        startNewGame()
    }

    fun newGame() = startNewGame()

    fun submit() {
        if (isFinished) return
        val value = input.toIntOrNull()
        if (value == null) {
            feedback = GuessFeedback.INVALID
            return
        }
        val result = game.guess(value)
        feedback = when (result) {
            GuessResult.OUT_OF_RANGE -> GuessFeedback.OUT_OF_RANGE
            GuessResult.TOO_LOW -> GuessFeedback.TOO_LOW
            GuessResult.TOO_HIGH -> GuessFeedback.TOO_HIGH
            GuessResult.CORRECT -> GuessFeedback.CORRECT
            GuessResult.LOST -> GuessFeedback.LOST
        }
        // A rejected guess keeps the text so it can be corrected; a counted guess starts a clean field.
        if (result == GuessResult.OUT_OF_RANGE) return
        input = ""
        attempts = game.attempts
        chancesLeft = game.attemptsLeft ?: 0
        if (game.isSolved || game.isLost) {
            isFinished = true
            revealedTarget = game.revealedTarget
            record(GuessRecord(level, game.attempts, won = game.isSolved, playedAtMillis = clock()))
        }
    }

    private fun startNewGame() {
        game = gameFactory(level)
        input = ""
        feedback = GuessFeedback.NONE
        attempts = 0
        chancesLeft = level.chances
        isFinished = false
        revealedTarget = null
    }

    private fun record(record: GuessRecord) {
        records = GuessStats.append(records, record)
        summary = GuessStats.summarize(records)
        scope.launch { statsRepository.append(record) }
    }

    private companion object {
        const val MAX_INPUT_LENGTH = 4
    }
}
