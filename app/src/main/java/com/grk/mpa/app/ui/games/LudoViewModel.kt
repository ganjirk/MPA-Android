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

/**
 * The game flow without any timing: the screen decides when to call [roll] and [autoMove] so that dice and
 * computer moves can be paced and animated.
 *
 * @param coroutineScope scope used for storage calls; null means [viewModelScope]. Tests pass their own.
 */
class LudoViewModel(
    private val repository: LudoRepository,
    private val coroutineScope: CoroutineScope? = null,
    private val random: Random = Random.Default
) : ViewModel() {
    private val scope: CoroutineScope get() = coroutineScope ?: viewModelScope

    var playerCount by mutableStateOf(2)
        private set

    /** Choice for each colour (index 0 red .. 3 blue). Only the colours in play are used. */
    var kinds by mutableStateOf(listOf(LudoKind.HUMAN, LudoKind.CPU, LudoKind.CPU, LudoKind.CPU))
        private set

    var state by mutableStateOf(newState())
        private set
    var isLoaded by mutableStateOf(false)
        private set

    /** True when the game on screen was restored from the device, until the first move is made. */
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
            }
            isLoaded = true
        }
    }

    private fun newState(): LudoState {
        val colours = Ludo.coloursFor(playerCount)
        return Ludo.newGame(colours.map { kinds[it] })
    }

    val isHumanTurn: Boolean get() = state.seats[state.turn].kind == LudoKind.HUMAN
    val legalTokens: List<Int> get() = Ludo.legalTokens(state)

    /** Humans can roll when it is their turn and the game is not over. */
    val canRoll: Boolean get() = isLoaded && state.phase == LudoPhase.ROLL && isHumanTurn

    /** Humans pick a token only when there is a real choice; a single legal move is made for them. */
    val canPickToken: Boolean get() = isLoaded && state.phase == LudoPhase.MOVE && isHumanTurn && legalTokens.size > 1

    fun onPlayerCountChange(count: Int) {
        if (!isLoaded || count !in 2..4) return
        playerCount = count
    }

    fun onKindChange(color: Int, kind: LudoKind) {
        if (!isLoaded || color !in 0..3) return
        kinds = kinds.toMutableList().also { it[color] = kind }
    }

    fun newGame() {
        if (!isLoaded) return
        state = newState()
        restored = false
        persist()
    }

    /** Rolls for the player whose turn it is, humans and computer alike. */
    fun roll(value: Int = Ludo.rollValue(random)) {
        if (!isLoaded || state.phase != LudoPhase.ROLL) return
        apply(Ludo.rollDice(state, value))
    }

    /** A human tapping one of their glowing tokens. */
    fun onTokenClick(seat: Int, token: Int) {
        if (!canPickToken || seat != state.turn) return
        apply(Ludo.moveToken(state, token))
    }

    /**
     * Makes the move nobody has to choose: the only legal move, or the computer's pick.
     * Returns false when it is a human's decision (or there is nothing to move).
     */
    fun autoMove(): Boolean {
        if (!isLoaded || state.phase != LudoPhase.MOVE) return false
        val legal = legalTokens
        val token = when {
            legal.isEmpty() -> return false
            legal.size == 1 -> legal[0]
            !isHumanTurn -> Ludo.chooseToken(state, state.seats[state.turn].kind, random)
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
