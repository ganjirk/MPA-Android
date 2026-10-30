package com.grk.mpa.app.data.api

/** The outcome of one API call. Network and server problems are values, not exceptions. */
sealed interface ApiResult<out T> {
    data class Success<out T>(val value: T) : ApiResult<T>

    /** HTTP 401: wrong credentials, or an expired or invalid token. */
    data object Unauthorized : ApiResult<Nothing>

    /** HTTP 429: too many requests. */
    data object RateLimited : ApiResult<Nothing>

    /** The server could not be reached (no connection, timeout, DNS failure). */
    data object NetworkError : ApiResult<Nothing>

    /** The server answered successfully but the body was not what we expected. */
    data object BadResponse : ApiResult<Nothing>

    /** Any other HTTP error, including unfollowed redirects. */
    data class HttpError(val code: Int) : ApiResult<Nothing>
}
