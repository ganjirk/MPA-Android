package com.grk.mpa.app.data

import com.grk.mpa.app.core.DecisionMakerData

interface DecisionMakerRepository {
    suspend fun load(): DecisionMakerData
    suspend fun save(data: DecisionMakerData)
}
