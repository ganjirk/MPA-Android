package com.grk.mpa.app.ui.games

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.grk.mpa.app.core.PongGame
import com.grk.mpa.app.core.PongSide
import com.grk.mpa.app.core.PongState

class PongViewModel(private val game: PongGame) : ViewModel() {
    constructor() : this(PongGame())

    var state by mutableStateOf(game.snapshot())
        private set
    var isPaused by mutableStateOf(false)
        private set

    /** Advances the game by [steps] fixed steps. Ignored while paused. */
    fun onFrame(steps: Int) {
        if (isPaused || steps <= 0) return
        repeat(steps) { game.step() }
        state = game.snapshot()
    }

    fun movePaddle(side: PongSide, centerY: Float) {
        game.movePaddle(side, centerY)
        state = game.snapshot()
    }

    fun togglePause() {
        isPaused = !isPaused
    }

    fun reset() {
        game.reset()
        isPaused = false
        state = game.snapshot()
    }
}
