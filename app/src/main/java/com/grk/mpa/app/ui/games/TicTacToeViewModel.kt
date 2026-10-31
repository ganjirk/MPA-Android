package com.grk.mpa.app.ui.games

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grk.mpa.app.core.GameResult
import com.grk.mpa.app.core.Mark
import com.grk.mpa.app.core.MatchWinner
import com.grk.mpa.app.core.NameError
import com.grk.mpa.app.core.Outcome
import com.grk.mpa.app.core.TicTacToe
import com.grk.mpa.app.core.TicTacToeStats
import com.grk.mpa.app.core.TicTacToeStatsOperations
import com.grk.mpa.app.core.TicTacToeState
import com.grk.mpa.app.data.TicTacToeRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.random.Random

enum class TicTacToeMode { COMPUTER, LOCAL }

/**
 * @param coroutineScope scope used for storage calls; null means [viewModelScope]. Tests pass their own.
 */
class TicTacToeViewModel(
    private val repository: TicTacToeRepository,
    private val coroutineScope: CoroutineScope? = null,
    private val random: Random = Random.Default,
    private val clock: () -> Long = System::currentTimeMillis
) : ViewModel() {
    private val scope: CoroutineScope get() = coroutineScope ?: viewModelScope

    var mode by mutableStateOf(TicTacToeMode.COMPUTER)
        private set
    var state by mutableStateOf(TicTacToeState())
        private set
    var stats by mutableStateOf(TicTacToeStats())
        private set
    var isLoaded by mutableStateOf(false)
        private set
    var playerXName by mutableStateOf("Player 1")
        private set
    var playerOName by mutableStateOf("Player 2")
        private set
    var nameError by mutableStateOf<NameError?>(null)
        private set

    val outcome: Outcome get() = state.outcome

    /** Names can only be changed before the first move of a two-player game. */
    val canEditNames: Boolean get() = mode == TicTacToeMode.LOCAL && state.moves.isEmpty()

    init {
        scope.launch {
            stats = repository.load()
            isLoaded = true
        }
    }

    fun onModeChange(newMode: TicTacToeMode) {
        if (newMode == mode) return
        mode = newMode
        newGame()
    }

    fun onNameChange(mark: Mark, text: String) {
        if (!canEditNames) return
        val limited = text.take(TicTacToeStatsOperations.MAX_NAME_LENGTH)
        if (mark == Mark.X) playerXName = limited else playerOName = limited
        nameError = null
    }

    /** Abandoned games are not counted, as on the website. */
    fun newGame() {
        state = TicTacToeState()
        nameError = null
    }

    fun onCellClick(cell: Int) {
        if (!isLoaded || state.outcome.isOver) return
        val next = when (mode) {
            TicTacToeMode.COMPUTER -> TicTacToe.playAgainstComputer(state, cell, random)
            TicTacToeMode.LOCAL -> {
                if (state.moves.isEmpty()) {
                    nameError = TicTacToeStatsOperations.validateNames(playerXName, playerOName)
                    if (nameError != null) return
                }
                TicTacToe.play(state, cell)
            }
        }
        if (next === state) return
        state = next
        if (next.outcome.isOver) recordResult(next.outcome)
    }

    private fun recordResult(outcome: Outcome) {
        val newStats = when (mode) {
            TicTacToeMode.COMPUTER -> TicTacToeStatsOperations.recordComputerGame(
                stats,
                when {
                    outcome.isDraw -> GameResult.DRAW
                    outcome.winner == Mark.X -> GameResult.WIN
                    else -> GameResult.LOSS
                }
            )
            TicTacToeMode.LOCAL -> TicTacToeStatsOperations.recordLocalMatch(
                stats,
                playerXName,
                playerOName,
                when {
                    outcome.isDraw -> MatchWinner.DRAW
                    outcome.winner == Mark.X -> MatchWinner.X
                    else -> MatchWinner.O
                },
                clock()
            )
        }
        stats = newStats
        scope.launch { repository.save(newStats) }
    }
}
