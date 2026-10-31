
package com.grk.mpa.app.ui.games

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grk.mpa.app.core.Ludo
import com.grk.mpa.app.core.LudoKind
import com.grk.mpa.app.core.LudoPhase
import com.grk.mpa.app.core.LudoState
import com.grk.mpa.app.data.LudoRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.random.Random

class LudoViewModel(
    private val repository: LudoRepository,
    private val coroutineScope: CoroutineScope? = null,
    private val random: Random = Random.Default
) : ViewModel() {
    private val scope: CoroutineScope get() = coroutineScope ?: viewModelScope

    var playerCount by mutableStateOf(2)
        private set

    var kinds by mutableStateOf(
        listOf(LudoKind.HUMAN, LudoKind.CPU, LudoKind.CPU, LudoKind.CPU)
    )
        private set

    var state by mutableStateOf(newState())
        private set

    var isLoaded by mutableStateOf(false)
        private set

    var gameStarted by mutableStateOf(false)
        private set

    var restored by mutableStateOf(false)
        private set

    init {
        scope.launch {
            repository.load()?.let { saved ->
                state = saved
                playerCount = saved.seats.size
                kinds = kinds.toMutableList().also { list ->
                    saved.seats.forEach { list[it.color] = it.kind }
                }
                restored = true
                gameStarted = true
            }
            isLoaded = true
        }
    }

    private fun newState(): LudoState {
        val colours = Ludo.coloursFor(playerCount)
        return Ludo.newGame(colours.map { kinds[it] })
    }

    val isHumanTurn: Boolean
        get() = state.seats[state.turn].kind == LudoKind.HUMAN

    val legalTokens: List<Int>
        get() = Ludo.legalTokens(state)

    val canRoll: Boolean
        get() = gameStarted && isLoaded &&
            state.phase == LudoPhase.ROLL && isHumanTurn

    val canPickToken: Boolean
        get() = gameStarted && isLoaded &&
            state.phase == LudoPhase.MOVE && isHumanTurn &&
            legalTokens.size > 1

    fun onPlayerCountChange(count: Int) {
        if (!isLoaded || gameStarted || count !in 2..4) return
        playerCount = count
    }

    fun onKindChange(color: Int, kind: LudoKind) {
        if (!isLoaded || gameStarted || color !in 0..3) return
        kinds = kinds.toMutableList().also { it[color] = kind }
    }

    /** Return to setup without replacing the previously saved game. */
    fun newGame() {
        if (!isLoaded) return
        gameStarted = false
        restored = false
    }

    /** Start a fresh game using the settings selected in setup. */
    fun startGame() {
        if (!isLoaded || gameStarted) return
        state = newState()
        gameStarted = true
        restored = false
        persist()
    }

    fun roll(value: Int = Ludo.rollValue(random)) {
        if (!gameStarted || !isLoaded || state.phase != LudoPhase.ROLL) return
        apply(Ludo.rollDice(state, value))
    }

    fun onTokenClick(seat: Int, token: Int) {
        if (!canPickToken || seat != state.turn) return
        apply(Ludo.moveToken(state, token))
    }

    fun autoMove(): Boolean {
        if (!gameStarted || !isLoaded || state.phase != LudoPhase.MOVE) {
            return false
        }

        val legal = legalTokens
        val token = when {
            legal.isEmpty() -> return false
            legal.size == 1 -> legal[0]
            !isHumanTurn -> Ludo.chooseToken(
                state,
                state.seats[state.turn].kind,
                random
            )
            else -> return false
        }

        apply(Ludo.moveToken(state, token))
        return true
    }

    private fun apply(next: LudoState) {
        if (next === state) return
        state = next
        restored = false
        persist()
    }

    private fun persist() {
        val snapshot = state
        scope.launch { repository.save(snapshot) }
    }
}
