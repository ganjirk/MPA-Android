package com.grk.mpa.app.data.api

import java.io.IOException
import java.net.URI
import java.util.concurrent.TimeUnit
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Talks to the MPA website's JSON API with OkHttp.
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
    private val http = OkHttpClient.Builder()
        .connectTimeout(TIMEOUT_MILLIS.toLong(), TimeUnit.MILLISECONDS)
        .readTimeout(TIMEOUT_MILLIS.toLong(), TimeUnit.MILLISECONDS)
        .followRedirects(false)
        .followSslRedirects(false)
        .retryOnConnectionFailure(false)
        .build()

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

    override suspend fun profile(accessToken: String): ApiResult<RemoteProfile> =
        call("api/v1/account/profile/", bearer = accessToken) { json.decodeFromString<RemoteProfile>(it) }

    override suspend fun updateProfile(
        accessToken: String,
        request: UpdateProfileRequest
    ): ApiResult<RemoteProfile> =
        call(
            "api/v1/account/profile/",
            method = "PATCH",
            body = json.encodeToString(request),
            bearer = accessToken
        ) { json.decodeFromString<RemoteProfile>(it) }

    override suspend fun uploadProfilePicture(
        accessToken: String,
        bytes: ByteArray,
        mimeType: String,
        fileName: String
    ): ApiResult<RemoteProfile> = withContext(Dispatchers.IO) {
        try {
            val mediaType = mimeType.toMediaType()
            val body = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("profile_picture", fileName, bytes.toRequestBody(mediaType))
                .build()
            val request = Request.Builder()
                .url(base + "api/v1/account/profile/")
                .header("Accept", "application/json")
                .header("Authorization", "Bearer $accessToken")
                .patch(body)
                .build()
            http.newCall(request).execute().use { response ->
                val text = response.body?.string().orEmpty()
                when {
                    response.code in 200..299 -> try {
                        ApiResult.Success(json.decodeFromString<RemoteProfile>(text))
                    } catch (e: SerializationException) {
                        ApiResult.BadResponse
                    } catch (e: IllegalArgumentException) {
                        ApiResult.BadResponse
                    }
                    response.code == 401 -> ApiResult.Unauthorized
                    response.code == 429 -> ApiResult.RateLimited
                    else -> ApiResult.HttpError(response.code)
                }
            }
        } catch (e: IOException) {
            ApiResult.NetworkError
        }
    }

    override suspend fun resendVerification(accessToken: String): ApiResult<AccountActionResponse> =
        call(
            "api/v1/account/verify-email/resend/",
            method = "POST",
            body = "{}",
            bearer = accessToken
        ) { json.decodeFromString<AccountActionResponse>(it) }

    override suspend fun changePassword(
        accessToken: String,
        request: PasswordChangeRequest
    ): ApiResult<AccountActionResponse> =
        call(
            "api/v1/account/password/",
            method = "POST",
            body = json.encodeToString(request),
            bearer = accessToken
        ) { json.decodeFromString<AccountActionResponse>(it) }

    override suspend fun signOutEverywhere(accessToken: String): ApiResult<AccountActionResponse> =
        call(
            "api/v1/account/sign-out-everywhere/",
            method = "POST",
            body = "{}",
            bearer = accessToken
        ) { json.decodeFromString<AccountActionResponse>(it) }

    override suspend fun deactivateAccount(
        accessToken: String,
        password: String,
        confirmation: String
    ): ApiResult<AccountActionResponse> =
        call(
            "api/v1/account/deactivate/",
            method = "POST",
            body = json.encodeToString(mapOf("password" to password, "confirmation" to confirmation)),
            bearer = accessToken
        ) { json.decodeFromString<AccountActionResponse>(it) }

    override suspend fun exportData(accessToken: String): ApiResult<String> =
        call("api/v1/account/export/", bearer = accessToken) { it }

    override suspend fun tasks(accessToken: String): ApiResult<List<RemoteTask>> =
        call("api/v1/tasks/", bearer = accessToken) { json.decodeFromString<List<RemoteTask>>(it) }

    override suspend fun createTask(
        accessToken: String,
        request: CreateTaskRequest
    ): ApiResult<RemoteTask> =
        call(
            "api/v1/tasks/",
            method = "POST",
            body = json.encodeToString(request),
            bearer = accessToken
        ) { json.decodeFromString<RemoteTask>(it) }

    override suspend fun updateTask(
        accessToken: String,
        id: Long,
        request: UpdateTaskRequest
    ): ApiResult<RemoteTask> {
        require(id > 0) { "Task id must be positive" }
        return call(
            "api/v1/tasks/$id/",
            method = "PATCH",
            body = json.encodeToString(request),
            bearer = accessToken
        ) { json.decodeFromString<RemoteTask>(it) }
    }

    override suspend fun deleteTask(accessToken: String, id: Long): ApiResult<Unit> {
        require(id > 0) { "Task id must be positive" }
        return call(
            "api/v1/tasks/$id/",
            method = "DELETE",
            bearer = accessToken
        ) { Unit }
    }

    override suspend fun expenses(accessToken: String, month: String?): ApiResult<List<RemoteExpense>> {
        require(month == null || MONTH_PATTERN.matches(month)) { "Month must use YYYY-MM format" }
        val query = month?.let { "?month=$it" }.orEmpty()
        return call("api/v1/expenses/$query", bearer = accessToken) {
            json.decodeFromString<List<RemoteExpense>>(it)
        }
    }

    override suspend fun createExpense(
        accessToken: String,
        request: CreateExpenseRequest
    ): ApiResult<RemoteExpense> =
        call(
            "api/v1/expenses/",
            method = "POST",
            body = json.encodeToString(request),
            bearer = accessToken
        ) { json.decodeFromString<RemoteExpense>(it) }

    override suspend fun updateExpense(
        accessToken: String,
        id: Long,
        request: UpdateExpenseRequest
    ): ApiResult<RemoteExpense> {
        require(id > 0) { "Expense id must be positive" }
        return call(
            "api/v1/expenses/$id/",
            method = "PATCH",
            body = json.encodeToString(request),
            bearer = accessToken
        ) { json.decodeFromString<RemoteExpense>(it) }
    }

    override suspend fun deleteExpense(accessToken: String, id: Long): ApiResult<Unit> {
        require(id > 0) { "Expense id must be positive" }
        return call("api/v1/expenses/$id/", method = "DELETE", bearer = accessToken) { Unit }
    }

    override suspend fun expenseSummary(
        accessToken: String,
        month: String
    ): ApiResult<RemoteExpenseSummary> {
        require(MONTH_PATTERN.matches(month)) { "Month must use YYYY-MM format" }
        return call("api/v1/expenses/summary/?month=$month", bearer = accessToken) {
            json.decodeFromString<RemoteExpenseSummary>(it)
        }
    }

    override suspend fun budgets(accessToken: String): ApiResult<List<RemoteMonthlyBudget>> =
        call("api/v1/budgets/", bearer = accessToken) {
            json.decodeFromString<List<RemoteMonthlyBudget>>(it)
        }

    override suspend fun saveBudget(
        accessToken: String,
        request: SaveMonthlyBudgetRequest
    ): ApiResult<RemoteMonthlyBudget> {
        require(BUDGET_MONTH_PATTERN.matches(request.month)) {
            "Budget month must use YYYY-MM-01 format"
        }
        require(request.month.endsWith("-01")) {
            "Budget month must be the first day of the month"
        }
        return call(
            "api/v1/budgets/",
            method = "POST",
            body = json.encodeToString(request),
            bearer = accessToken
        ) { json.decodeFromString<RemoteMonthlyBudget>(it) }
    }

    private suspend fun <T> call(
        path: String,
        body: String? = null,
        bearer: String? = null,
        method: String = if (body != null) "POST" else "GET",
        parse: (String) -> T
    ): ApiResult<T> = withContext(Dispatchers.IO) {
        try {
            val requestBody = body?.toRequestBody(JSON_MEDIA_TYPE)
            val builder = Request.Builder()
                .url(base + path)
                .header("Accept", "application/json")
            if (bearer != null) builder.header("Authorization", "Bearer $bearer")
            val request = builder.method(method, requestBody).build()

            http.newCall(request).execute().use { response ->
                val text = response.body?.string().orEmpty()
                when {
                    response.code in 200..299 -> try {
                        ApiResult.Success(parse(text))
                    } catch (e: SerializationException) {
                        ApiResult.BadResponse
                    } catch (e: IllegalArgumentException) {
                        ApiResult.BadResponse
                    }
                    response.code == 401 -> ApiResult.Unauthorized
                    response.code == 429 -> ApiResult.RateLimited
                    else -> ApiResult.HttpError(response.code)
                }
            }
        } catch (e: IOException) {
            ApiResult.NetworkError
        }
    }

    private companion object {
        const val TIMEOUT_MILLIS = 15_000
        val MONTH_PATTERN = Regex("^\\d{4}-(0[1-9]|1[0-2])$")
        val BUDGET_MONTH_PATTERN = Regex("^\\d{4}-(0[1-9]|1[0-2])-\\d{2}$")
        val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
        val LOOPBACK_HOSTS = setOf("localhost", "127.0.0.1", "::1", "[::1]")

        fun isSecure(url: String): Boolean {
            val uri = runCatching { URI(url) }.getOrNull() ?: return false
            return uri.scheme == "https" || (uri.scheme == "http" && uri.host in LOOPBACK_HOSTS)
        }
    }
}
