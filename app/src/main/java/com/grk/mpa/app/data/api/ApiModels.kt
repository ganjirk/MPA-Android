package com.grk.mpa.app.data.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(val username: String, val password: String)

@Serializable
data class RefreshRequest(val refresh: String)

/** Response of `POST /api/v1/auth/token/`. */
@Serializable
data class TokenPair(val access: String, val refresh: String)

/** Response of `POST /api/v1/auth/token/refresh/`. The server rotates refresh tokens, so it may send a new one. */
@Serializable
data class RefreshedTokens(val access: String, val refresh: String? = null)

/** Response of `GET /api/v1/account/`: the minimal profile the server exposes. */
@Serializable
data class Account(
    val id: Long,
    val username: String,
    @SerialName("first_name") val firstName: String = "",
    @SerialName("last_name") val lastName: String = "",
    val email: String = "",
    @SerialName("email_verified") val emailVerified: Boolean = false
) {
    /** Full name when the account has one, otherwise the username. */
    val displayName: String
        get() = listOf(firstName, lastName).filter { it.isNotBlank() }.joinToString(" ").ifBlank { username }
}
