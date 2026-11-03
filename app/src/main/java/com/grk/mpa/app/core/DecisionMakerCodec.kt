package com.grk.mpa.app.core

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object DecisionMakerCodec {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        isLenient = false
    }

    fun encode(data: DecisionMakerData): String {
        return json.encodeToString(data)
    }

    fun decode(raw: String): DecisionMakerData {
        if (raw.isBlank()) {
            return DecisionMakerData()
        }

        return try {
            json.decodeFromString<DecisionMakerData>(raw)
        } catch (_: Exception) {
            DecisionMakerData()
        }
    }
}
