package com.grk.mpa.app.data.api

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Talks to the MPA website's JSON API with plain [HttpURLConnection].
 *
 * - HTTPS only. Plain http is accepted just for loopback addresses, so tests and a local dev server work.
 * - Redirects are never followed, so a bearer token cannot be forwarded to another address.
 * - Nothing is logged, so credentials and tokens cannot leak into logs.
 */
class MpaApiClient(
    baseUrl: String = ApiConfig.BASE_URL,
    private val json: Json = Json { ignoreUnknownKeys = true }
) : MpaApi {
    private val base = baseUrl.trimEnd('/') + "/"

    init {
        require(isSecure(base)) { "The API must be reached over https" }
    }

    override suspend fun login(username: String, password: String): ApiResult<TokenPair> =
        call("api/v1/auth/token/", body = json.encodeToString(LoginRequest(username, password))) {
            json.decodeFromString<TokenPair>(it)
        }

    override suspend fun refresh(refreshToken: String): ApiResult<RefreshedTokens> =
        call("api/v1/auth/token/refresh/", body = json.encodeToString(RefreshRequest(refreshToken))) {
            json.decodeFromString<RefreshedTokens>(it)
        }

    override suspend fun account(accessToken: String): ApiResult<Account> =
        call("api/v1/account/", bearer = accessToken) { json.decodeFromString<Account>(it) }

    private suspend fun <T> call(
        path: String,
        body: String? = null,
        bearer: String? = null,
        parse: (String) -> T
    ): ApiResult<T> = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            connection = (URL(base + path).openConnection() as HttpURLConnection).apply {
                requestMethod = if (body != null) "POST" else "GET"
                connectTimeout = TIMEOUT_MILLIS
                readTimeout = TIMEOUT_MILLIS
                instanceFollowRedirects = false
                setRequestProperty("Accept", "application/json")
                if (bearer != null) setRequestProperty("Authorization", "Bearer $bearer")
                if (body != null) {
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json; charset=utf-8")
                    outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
                }
            }
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            when {
                code in 200..299 -> try {
                    ApiResult.Success(parse(text))
                } catch (e: SerializationException) {
                    ApiResult.BadResponse
                } catch (e: IllegalArgumentException) {
                    ApiResult.BadResponse
                }
                code == 401 -> ApiResult.Unauthorized
                code == 429 -> ApiResult.RateLimited
                else -> ApiResult.HttpError(code)
            }
        } catch (e: IOException) {
            ApiResult.NetworkError
        } finally {
            connection?.disconnect()
        }
    }

    private companion object {
        const val TIMEOUT_MILLIS = 15_000
        val LOOPBACK_HOSTS = setOf("localhost", "127.0.0.1", "::1", "[::1]")

        fun isSecure(url: String): Boolean {
            val uri = runCatching { URI(url) }.getOrNull() ?: return false
            return uri.scheme == "https" || (uri.scheme == "http" && uri.host in LOOPBACK_HOSTS)
        }
    }
}
