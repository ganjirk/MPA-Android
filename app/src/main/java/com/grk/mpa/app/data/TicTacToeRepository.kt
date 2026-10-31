package com.grk.mpa.app.data

import com.grk.mpa.app.core.TicTacToeStats

/** Where Tic Tac Toe results are stored. The UI only depends on this interface. */
interface TicTacToeRepository {
    suspend fun load(): TicTacToeStats
    suspend fun save(stats: TicTacToeStats)
}
