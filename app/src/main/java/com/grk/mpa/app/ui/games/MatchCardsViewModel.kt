package com.grk.mpa.app.ui.games

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grk.mpa.app.core.MatchCards
import com.grk.mpa.app.core.MatchDifficulty
import com.grk.mpa.app.core.MatchGame
import com.grk.mpa.app.core.MatchHistory
import com.grk.mpa.app.core.MatchResult
import com.grk.mpa.app.data.MatchCardsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.random.Random

enum class MatchStatus { PROMPT, MATCH, NO_MATCH, DONE }

/**
 * The game without any timing of its own: the screen calls [hideMismatch] after the pause and [tick] a couple of
 * times a second to refresh the clock.
 *
 * @param coroutineScope scope used for storage calls; null means [viewModelScope]. Tests pass their own.
 * @param clock wall-clock milliseconds.
 */
class MatchCardsViewModel(
    private val repository: MatchCardsRepository,
    private val coroutineScope: CoroutineScope? = null,
    private val random: Random = Random.Default,
    private val clock: () -> Long = System::currentTimeMillis
) : ViewModel() {
    private val scope: CoroutineScope get() = coroutineScope ?: viewModelScope

    var difficulty by mutableStateOf(MatchDifficulty.EASY)
        private set
    var game by mutableStateOf(MatchCards.newGame(MatchDifficulty.EASY, random))
        private set
    var status by mutableStateOf(MatchStatus.PROMPT)
        private set
    var elapsedSeconds by mutableIntStateOf(0)
        private set
    var history by mutableStateOf<List<MatchResult>>(emptyList())
        private set

    private var startedAt: Long? = null

    init {
        scope.launch { history = repository.load() }
    }

    fun onDifficultyChange(newDifficulty: MatchDifficulty) {
        difficulty = newDifficulty
        newGame()
    }

    /** "Shuffle & restart" and "Play again". */
    fun newGame() {
        game = MatchCards.newGame(difficulty, random)
        status = MatchStatus.PROMPT
        startedAt = null
        elapsedSeconds = 0
    }

    fun onCardClick(index: Int) {
        val before = game
        val after = MatchCards.flip(before, index)
        if (after === before) return
        if (startedAt == null) startedAt = clock()
        game = after
        when {
            after.finished -> finish(after)
            after.matches > before.matches -> status = MatchStatus.MATCH
            after.mismatchPending -> status = MatchStatus.NO_MATCH
        }
    }

    /** Called by the screen about 750 ms after two different cards were turned over. */
    fun hideMismatch() {
        game = MatchCards.hideMismatch(game)
    }

    fun tick() {
        val start = startedAt ?: return
        if (game.finished) return
        elapsedSeconds = ((clock() - start) / 1000).toInt()
    }

    private fun finish(done: MatchGame) {
        val start = startedAt ?: clock()
        elapsedSeconds = maxOf(1, ((clock() - start) / 1000).toInt())
        status = MatchStatus.DONE
        val updated = MatchHistory.add(
            history,
            MatchResult(clock(), done.difficulty, done.moves, elapsedSeconds)
        )
        history = updated
        scope.launch { repository.save(updated) }
    }
}
