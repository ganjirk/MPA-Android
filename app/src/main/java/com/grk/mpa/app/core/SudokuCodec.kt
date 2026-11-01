package com.grk.mpa.app.core

/** A game saved so it can be resumed: the puzzle, the answer, your entries, notes, time and hints. */
data class SudokuSave(
    val difficulty: SudokuDifficulty,
    val puzzle: List<Int>,
    val solution: List<Int>,
    val values: List<Int>,
    val notes: List<Set<Int>>,
    val elapsedMs: Long,
    val hints: Int
)

/** One saved game per difficulty, and the best hint-free time per difficulty. */
data class SudokuStore(
    val games: Map<SudokuDifficulty, SudokuSave> = emptyMap(),
    val bestMs: Map<SudokuDifficulty, Long> = emptyMap()
)

/**
 * Stores a [SudokuStore] as plain text, one record per line, fields separated by tabs:
 * - `G difficulty puzzle solution values notes elapsedMs hints`, where puzzle, solution and values are
 *   81 digits and notes are 81 comma-separated digit groups (empty when a cell has no notes)
 * - `B difficulty bestMs`
 * Records that fail any check (lengths, digits, a valid solution, givens that match) are skipped.
 */
object SudokuCodec {
    private const val TAB = "\t"

    fun encode(store: SudokuStore): String {
        val lines = mutableListOf<String>()
        store.games.values.forEach { g ->
            lines += listOf(
                "G",
                g.difficulty.name,
                g.puzzle.joinToString(""),
                g.solution.joinToString(""),
                g.values.joinToString(""),
                g.notes.joinToString(",") { it.sorted().joinToString("") },
                g.elapsedMs,
                g.hints
            ).joinToString(TAB)
        }
        store.bestMs.forEach { (difficulty, ms) ->
            lines += listOf("B", difficulty.name, ms).joinToString(TAB)
        }
        return lines.joinToString("\n")
    }

    fun decode(text: String): SudokuStore {
        val games = linkedMapOf<SudokuDifficulty, SudokuSave>()
        val best = linkedMapOf<SudokuDifficulty, Long>()
        for (line in text.lineSequence()) {
            val parts = line.split(TAB)
            when (parts.firstOrNull()) {
                "G" -> parseGame(parts)?.let { games[it.difficulty] = it }
                "B" -> parseBest(parts)?.let { (d, ms) -> best[d] = ms }
            }
        }
        return SudokuStore(games, best)
    }

    private fun difficultyOf(name: String): SudokuDifficulty? = SudokuDifficulty.entries.firstOrNull { it.name == name }

    private fun parseBest(parts: List<String>): Pair<SudokuDifficulty, Long>? {
        if (parts.size != 3) return null
        val difficulty = difficultyOf(parts[1]) ?: return null
        val ms = parts[2].toLongOrNull()?.takeIf { it > 0 } ?: return null
        return difficulty to ms
    }

    private fun parseGame(parts: List<String>): SudokuSave? {
        if (parts.size != 8) return null
        val difficulty = difficultyOf(parts[1]) ?: return null
        val puzzle = digits(parts[2]) ?: return null
        val solution = digits(parts[3]) ?: return null
        val values = digits(parts[4]) ?: return null
        val notes = parseNotes(parts[5]) ?: return null
        val elapsed = parts[6].toLongOrNull()?.takeIf { it >= 0 } ?: return null
        val hints = parts[7].toIntOrNull()?.takeIf { it >= 0 } ?: return null

        if (!isValidSolution(solution)) return null
        for (i in 0 until 81) {
            if (puzzle[i] != 0 && puzzle[i] != solution[i]) return null
            if (puzzle[i] != 0 && values[i] != puzzle[i]) return null
        }
        if (values == solution) return null // a finished game is never kept
        return SudokuSave(difficulty, puzzle, solution, values, notes, elapsed, hints)
    }

    private fun digits(text: String): List<Int>? {
        if (text.length != 81 || text.any { it !in '0'..'9' }) return null
        return text.map { it - '0' }
    }

    private fun parseNotes(text: String): List<Set<Int>>? {
        val cells = text.split(",")
        if (cells.size != 81) return null
        return cells.map { cell ->
            if (cell.any { it !in '1'..'9' }) return null
            cell.map { it - '0' }.toSet()
        }
    }

    /** True when every row, column and 3x3 box holds the digits 1 to 9 exactly once. */
    fun isValidSolution(grid: List<Int>): Boolean {
        if (grid.size != 81) return false
        val all = (1..9).toSet()
        for (k in 0 until 9) {
            val row = (0 until 9).map { grid[k * 9 + it] }
            val col = (0 until 9).map { grid[it * 9 + k] }
            val box = (0 until 9).map { grid[(k / 3 * 3 + it / 3) * 9 + (k % 3 * 3 + it % 3)] }
            if (row.toSet() != all || col.toSet() != all || box.toSet() != all) return false
        }
        return true
    }
}
