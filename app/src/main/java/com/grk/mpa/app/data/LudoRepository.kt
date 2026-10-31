package com.grk.mpa.app.data

import com.grk.mpa.app.core.LudoState

/** Where the Ludo game in progress is kept. The UI only depends on this interface. */
interface LudoRepository {
    suspend fun load(): LudoState?
    suspend fun save(state: LudoState)
}
