package com.grk.mpa.app.data

import com.grk.mpa.app.core.ChessSave

/** Where the Chess game in progress is kept. The UI only depends on this interface. */
interface ChessRepository {
    suspend fun load(): ChessSave?
    suspend fun save(save: ChessSave)
}
