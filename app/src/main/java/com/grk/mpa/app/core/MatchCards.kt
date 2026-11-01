package com.grk.mpa.app.core

import kotlin.random.Random

/** Same three sizes as the website's Match Cards. */
enum class MatchDifficulty(val pairs: Int, val wire: String) {
    EASY(6, "easy"),
    MEDIUM(8, "medium"),
    HARD(12, "hard")
}

data class MatchCard(val id: Int, val symbol: String, val matched: Boolean = false)

/**
 * One game of Match Cards. [first] and [second] are the positions of the cards turned over right now (-1 for none);
 * [mismatchPending] is true while two different cards are showing and must be turned back with [MatchCards.hideMismatch].
 */
data class MatchGame(
    val difficulty: MatchDifficulty,
    val cards: List<MatchCard>,
    val first: Int = -1,
    val second: Int = -1,
    val moves: Int = 0,
    val matches: Int = 0,
    val mismatchPending: Boolean = false
) {
    val finished: Boolean get() = matches == difficulty.pairs

    /** A card shows its symbol when matched or one of the two just turned over. */
    fun isRevealed(index: Int): Boolean = cards[index].matched || index == first || index == second
}

object MatchCards {
    val SYMBOLS = listOf("🍎", "🚀", "🌈", "🐼", "🎸", "🍀", "⚽", "🦊", "🌻", "🍉", "🎈", "🐳")

    fun newGame(difficulty: MatchDifficulty, random: Random = Random.Default): MatchGame {
        val symbols = SYMBOLS.take(difficulty.pairs)
        val deck = (symbols + symbols).shuffled(random).mapIndexed { i, s -> MatchCard(i, s) }
        return MatchGame(difficulty, deck)
    }

    /** Turns a card over. Cards that cannot be turned right now leave the game unchanged. */
    fun flip(game: MatchGame, index: Int): MatchGame {
        if (index !in game.cards.indices || game.mismatchPending || game.finished) return game
        if (game.cards[index].matched || index == game.first) return game
        if (game.first == -1) return game.copy(first = index)

        val moves = game.moves + 1
        val a = game.first
        return if (game.cards[a].symbol == game.cards[index].symbol) {
            val cards = game.cards.mapIndexed { i, c -> if (i == a || i == index) c.copy(matched = true) else c }
            game.copy(cards = cards, first = -1, second = -1, moves = moves, matches = game.matches + 1)
        } else {
            game.copy(second = index, moves = moves, mismatchPending = true)
        }
    }

    /** Turns the two cards of a miss face down again. */
    fun hideMismatch(game: MatchGame): MatchGame =
        if (game.mismatchPending) game.copy(first = -1, second = -1, mismatchPending = false) else game
}

/** A finished game. */
data class MatchResult(
    val completedAtMillis: Long,
    val difficulty: MatchDifficulty,
    val moves: Int,
    val elapsedSeconds: Int
) {
    val pairs: Int get() = difficulty.pairs
}

object MatchHistory {
    const val MAX_ENTRIES = 30

    /** Newest first, never more than [MAX_ENTRIES]. */
    fun add(list: List<MatchResult>, result: MatchResult): List<MatchResult> =
        (listOf(result) + list).take(MAX_ENTRIES)

    /** Fewest moves wins; a tie goes to the faster game. Like the website's history page. */
    fun best(list: List<MatchResult>, difficulty: MatchDifficulty): MatchResult? =
        list.filter { it.difficulty == difficulty }.minWithOrNull(compareBy({ it.moves }, { it.elapsedSeconds }))

    fun formatTime(seconds: Int): String = "%02d:%02d".format(java.util.Locale.ROOT, seconds / 60, seconds % 60)
}

/**
 * Plain text storage, one result per line, fields separated by tabs:
 * `completedAtMillis difficulty moves elapsedSeconds`. Lines that cannot be read are skipped.
 */
object MatchHistoryCodec {
    private const val TAB = "\t"

    fun encode(list: List<MatchResult>): String = list.joinToString("\n") {
        listOf(it.completedAtMillis, it.difficulty.wire, it.moves, it.elapsedSeconds).joinToString(TAB)
    }

    fun decode(text: String): List<MatchResult> = text.lineSequence().mapNotNull { line ->
        val p = line.split(TAB)
        if (p.size != 4) return@mapNotNull null
        val at = p[0].toLongOrNull() ?: return@mapNotNull null
        val difficulty = MatchDifficulty.entries.firstOrNull { it.wire == p[1] } ?: return@mapNotNull null
        val moves = p[2].toIntOrNull()?.takeIf { it >= difficulty.pairs } ?: return@mapNotNull null
        val seconds = p[3].toIntOrNull()?.takeIf { it >= 1 } ?: return@mapNotNull null
        MatchResult(at, difficulty, moves, seconds)
    }.take(MatchHistory.MAX_ENTRIES).toList()
}
