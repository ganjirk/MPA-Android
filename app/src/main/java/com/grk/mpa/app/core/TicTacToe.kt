package com.grk.mpa.app.core

import kotlin.random.Random

enum class Mark { X, O }

/** The result of a board. [line] holds the three winning cells (1 to 9) when there is a winner. */
data class Outcome(
    val winner: Mark? = null,
    val isDraw: Boolean = false,
    val line: List<Int> = emptyList()
) {
    val isOver: Boolean get() = winner != null || isDraw
}

data class TicTacToeMove(val cell: Int, val mark: Mark)

/** A game in progress. Cells are numbered 1 to 9, left to right, top to bottom. X always moves first. */
data class TicTacToeState(
    val moves: List<TicTacToeMove> = emptyList()
) {
    val board: Map<Int, Mark> get() = moves.associate { it.cell to it.mark }
    val outcome: Outcome get() = TicTacToe.outcome(board)
    val nextMark: Mark get() = if (moves.size % 2 == 0) Mark.X else Mark.O
}

/** Rules ported from the website's Tic Tac Toe (`tictac/views.py`). */
object TicTacToe {
    val WIN_LINES: List<List<Int>> = listOf(
        listOf(1, 2, 3), listOf(4, 5, 6), listOf(7, 8, 9),
        listOf(1, 4, 7), listOf(2, 5, 8), listOf(3, 6, 9),
        listOf(1, 5, 9), listOf(3, 5, 7)
    )
    private val CORNERS = listOf(1, 3, 7, 9)
    const val CELLS = 9

    fun outcome(board: Map<Int, Mark>): Outcome {
        for (line in WIN_LINES) {
            val first = board[line[0]] ?: continue
            if (board[line[1]] == first && board[line[2]] == first) return Outcome(winner = first, line = line)
        }
        return if (board.size == CELLS) Outcome(isDraw = true) else Outcome()
    }

    /**
     * The computer plays O: win if possible, else block X, else take the centre,
     * else a random corner, else any free cell. The board must have a free cell.
     */
    fun computerCell(board: Map<Int, Mark>, random: Random = Random.Default): Int {
        for (mark in listOf(Mark.O, Mark.X)) {
            for (line in WIN_LINES) {
                val marks = line.map { board[it] }
                if (marks.count { it == mark } == 2 && marks.count { it == null } == 1) {
                    return line[marks.indexOf(null)]
                }
            }
        }
        val free = (1..CELLS).filter { it !in board }
        require(free.isNotEmpty()) { "no free cell" }
        if (5 in free) return 5
        val corners = CORNERS.filter { it in free }
        val choices = corners.ifEmpty { free }
        return choices[random.nextInt(choices.size)]
    }

    /** Places the next mark on [cell]. Returns the same state if the game is over or the cell is invalid or taken. */
    fun play(state: TicTacToeState, cell: Int): TicTacToeState {
        if (cell !in 1..CELLS || state.outcome.isOver || state.moves.any { it.cell == cell }) return state
        return TicTacToeState(state.moves + TicTacToeMove(cell, state.nextMark))
    }

    /** The person plays X on [cell]; if the game goes on, the computer answers at once as O. */
    fun playAgainstComputer(state: TicTacToeState, cell: Int, random: Random = Random.Default): TicTacToeState {
        val afterHuman = play(state, cell)
        if (afterHuman === state || afterHuman.outcome.isOver) return afterHuman
        return play(afterHuman, computerCell(afterHuman.board, random))
    }
}
