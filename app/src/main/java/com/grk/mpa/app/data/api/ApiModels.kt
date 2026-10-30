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


/** One task returned by the authenticated mobile Tasks API. */
@Serializable
data class RemoteTask(
    val id: Long,
    val text: String,
    val notes: String = "",
    val priority: String = "",
    val completed: Boolean = false,
    @SerialName("created_date") val createdDate: String,
    @SerialName("due_date") val dueDate: String? = null,
    @SerialName("completed_on") val completedOn: String? = null
)

/** Body for POST /api/v1/tasks/. Ownership and id are always assigned by the server. */
@Serializable
data class CreateTaskRequest(
    val text: String,
    val notes: String = "",
    val priority: String = "",
    val completed: Boolean = false,
    @SerialName("due_date") val dueDate: String? = null
)

/**
 * Body for PATCH /api/v1/tasks/{id}/.
 * Null fields are omitted. Set dueDate to JsonNull explicitly to clear a due date.
 */
@Serializable
data class UpdateTaskRequest(
    val text: String? = null,
    val notes: String? = null,
    val priority: String? = null,
    val completed: Boolean? = null,
    @SerialName("due_date") val dueDate: kotlinx.serialization.json.JsonElement? = null
)
