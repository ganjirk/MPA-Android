package com.grk.mpa.app.data.auth

import kotlinx.serialization.Serializable

@Serializable
data class StoredTokens(val access: String, val refresh: String)

/** Where the sign-in tokens are kept between app launches. */
interface AuthStorage {
    suspend fun load(): StoredTokens?
    suspend fun save(tokens: StoredTokens)
    suspend fun clear()
}
