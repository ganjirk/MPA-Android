package com.grk.mpa.app.ui.games

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grk.mpa.app.core.Chess
import com.grk.mpa.app.core.ChessEntry
import com.grk.mpa.app.core.ChessGame
import com.grk.mpa.app.core.ChessLevel
import com.grk.mpa.app.core.ChessMode
import com.grk.mpa.app.core.ChessMove
import com.grk.mpa.app.core.ChessMoveRef
import com.grk.mpa.app.core.ChessPosition
import com.grk.mpa.app.core.ChessResult
import com.grk.mpa.app.core.ChessSave
import com.grk.mpa.app.core.ChessStatus
import com.grk.mpa.app.data.ChessRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.random.Random

/**
 * @param coroutineScope scope used for storage and thinking; null means [viewModelScope]. Tests pass their own.
 * @param thinkingDispatcher where the computer works out its move, so the screen never freezes.
 */
class ChessViewModel(
    private val repository: ChessRepository,
    private val coroutineScope: CoroutineScope? = null,
    private val thinkingDispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val random: Random = Random.Default
) : ViewModel() {
    private val scope: CoroutineScope get() = coroutineScope ?: viewModelScope

    var mode by mutableStateOf(ChessMode.COMPUTER)
        private set

    /** Human colour against the computer: 1 white, -1 black. */
    var side by mutableStateOf(1)
        private set
    var level by mutableStateOf(ChessLevel.MEDIUM)
        private set
    var flipped by mutableStateOf(false)
        private set

    var position by mutableStateOf(Chess.newGame())
        private set
    var history by mutableStateOf<List<ChessEntry>>(emptyList())
        private set
    var legalMoves by mutableStateOf<List<ChessMove>>(emptyList())
        private set
    var selected by mutableStateOf(-1)
        private set

    /** The moves a pawn could make to the same square with different promotions, while the player chooses. */
    var pendingPromotion by mutableStateOf<List<ChessMove>?>(null)
        private set
    var thinking by mutableStateOf(false)
        private set
    var isLoaded by mutableStateOf(false)
        private set

    private var resignedLoser = 0
    private var rules by mutableStateOf(Chess.status(position))
    private var thinkJob: Job? = null

    /** How the game ended, or null while it is going on. */
    val outcome: ChessStatus?
        get() = if (resignedLoser != 0) ChessStatus(true, ChessResult.RESIGN, -resignedLoser, false)
        else rules.takeIf { it.over }
    val resignedColor: Int get() = resignedLoser
    val inCheck: Boolean get() = rules.check

    val vsComputer: Boolean get() = mode == ChessMode.COMPUTER

    /** True when the person may tap a piece or square. */
    val canMove: Boolean
        get() = isLoaded && outcome == null && !thinking && pendingPromotion == null &&
            !(vsComputer && position.turn != side)

    val canUndo: Boolean
        get() = isLoaded && if (vsComputer) history.any { it.before.turn == side } else history.isNotEmpty()
    val canResign: Boolean get() = isLoaded && outcome == null && history.isNotEmpty()

    init {
        scope.launch {
            val saved = repository.load()
            val replay = saved?.let { ChessGame.replay(it.moves) }
            if (saved != null && replay != null) {
                mode = saved.mode
                side = saved.side
                level = saved.level
                flipped = saved.flipped
                position = replay.first
                history = replay.second
                resignedLoser = saved.resignedLoser
            } else {
                resetBoard()
            }
            isLoaded = true
            refresh()
        }
    }

    // ----- Setup -----

    fun onModeChange(newMode: ChessMode) {
        if (newMode == mode) return
        mode = newMode
        newGame()
    }

    fun onSideChange(newSide: Int) {
        if (newSide == side || (newSide != 1 && newSide != -1)) return
        side = newSide
        newGame()
    }

    fun onLevelChange(newLevel: ChessLevel) {
        level = newLevel
        persist()
    }

    fun flip() {
        flipped = !flipped
        persist()
    }

    fun newGame() {
        stopThinking()
        resetBoard()
        refresh()
    }

    private fun resetBoard() {
        position = Chess.newGame()
        history = emptyList()
        resignedLoser = 0
        selected = -1
        pendingPromotion = null
        flipped = vsComputer && side == -1
    }

    // ----- Playing -----

    fun onSquare(sq: Int) {
        if (!canMove || sq !in 0..63) return
        if (selected >= 0) {
            val picks = legalMoves.filter { it.from == selected && it.to == sq }
            if (picks.size > 1) {
                pendingPromotion = picks
                selected = -1
                return
            }
            if (picks.size == 1) {
                play(picks[0])
                return
            }
        }
        val mine = position.board[sq] * position.turn > 0 && legalMoves.any { it.from == sq }
        selected = if (mine && selected != sq) sq else -1
    }

    fun onPromotion(piece: Int) {
        val choices = pendingPromotion ?: return
        choices.firstOrNull { it.promo == piece }?.let(::play)
    }

    fun cancelPromotion() {
        pendingPromotion = null
    }

    private fun play(move: ChessMove) {
        history = history + ChessGame.entry(position, move, legalMoves)
        position = Chess.makeMove(position, move)
        selected = -1
        pendingPromotion = null
        refresh()
    }

    fun undo() {
        if (!canUndo) return
        stopThinking()
        val keep = if (vsComputer) history.indexOfLast { it.before.turn == side } else history.size - 1
        if (keep < 0) return
        position = history[keep].before
        history = history.take(keep)
        resignedLoser = 0
        selected = -1
        pendingPromotion = null
        refresh()
    }

    fun resign() {
        if (!canResign) return
        stopThinking()
        resignedLoser = if (vsComputer) side else position.turn
        pendingPromotion = null
        selected = -1
        persist()
    }

    private fun refresh() {
        legalMoves = Chess.legalMoves(position)
        rules = Chess.status(position)
        persist()
        if (outcome == null && vsComputer && position.turn == -side && !thinking) think()
    }

    private fun think() {
        thinking = true
        val snapshot = position
        val chosenLevel = level
        thinkJob = scope.launch {
            val move = withContext(thinkingDispatcher) { Chess.bestMove(snapshot, chosenLevel, random) }
            thinking = false
            if (move != null) play(move) else refresh()
        }
    }

    private fun stopThinking() {
        thinkJob?.cancel()
        thinkJob = null
        thinking = false
    }

    // ----- Saving -----

    private fun persist() {
        if (!isLoaded) return
        val save = ChessSave(
            mode, side, level, flipped, resignedLoser,
            history.map { ChessMoveRef(it.move.from, it.move.to, it.move.promo) }
        )
        scope.launch { repository.save(save) }
    }
}
