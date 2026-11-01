package com.grk.mpa.app.data.auth

import com.grk.mpa.app.data.api.Account
import com.grk.mpa.app.data.api.ApiResult
import com.grk.mpa.app.data.api.MpaApi
import com.grk.mpa.app.data.api.RemoteProfile
import com.grk.mpa.app.data.api.UpdateProfileRequest
import com.grk.mpa.app.data.api.AccountActionResponse
import com.grk.mpa.app.data.api.PasswordChangeRequest

sealed interface SignInResult {
    data class Success(val account: Account) : SignInResult
    data object InvalidCredentials : SignInResult
    data object RateLimited : SignInResult
    data object NetworkError : SignInResult
    data object Failed : SignInResult
}

sealed interface SessionResult {
    /** Stored tokens are valid; this is the signed-in account. */
    data class Active(val account: Account) : SessionResult

    /** Nobody is signed in, or the stored session is no longer valid. */
    data object SignedOut : SessionResult

    /** There is a stored session but the server could not be reached, so its state is unknown. */
    data object Offline : SessionResult
}

/** Signs in and out and keeps the session fresh. Knows nothing about the UI. */
class AuthRepository(private val api: MpaApi, private val storage: AuthStorage) {

    suspend fun signIn(username: String, password: String): SignInResult {
        val tokens = when (val result = api.login(username.trim(), password)) {
            is ApiResult.Success -> result.value
            ApiResult.Unauthorized -> return SignInResult.InvalidCredentials
            ApiResult.RateLimited -> return SignInResult.RateLimited
            ApiResult.NetworkError -> return SignInResult.NetworkError
            ApiResult.BadResponse, is ApiResult.HttpError -> return SignInResult.Failed
        }
        storage.save(StoredTokens(tokens.access, tokens.refresh))
        return when (val account = api.account(tokens.access)) {
            is ApiResult.Success -> SignInResult.Success(account.value)
            else -> {
                // Do not stay half signed in.
                storage.clear()
                if (account == ApiResult.NetworkError) SignInResult.NetworkError else SignInResult.Failed
            }
        }
    }

    /** Checks the stored session at app start. Never signs the user out because of a network problem. */
    suspend fun restoreSession(): SessionResult {
        val tokens = storage.load() ?: return SessionResult.SignedOut
        return when (val result = fetchAccount(tokens)) {
            is ApiResult.Success -> SessionResult.Active(result.value)
            ApiResult.Unauthorized -> {
                storage.clear()
                SessionResult.SignedOut
            }
            else -> SessionResult.Offline
        }
    }

    suspend fun profile(): ApiResult<RemoteProfile> = withSession { api.profile(it) }

    suspend fun updateProfile(request: UpdateProfileRequest): ApiResult<RemoteProfile> =
        withSession { api.updateProfile(it, request) }

    suspend fun uploadProfilePicture(bytes: ByteArray, mimeType: String, fileName: String): ApiResult<RemoteProfile> =
        withSession { api.uploadProfilePicture(it, bytes, mimeType, fileName) }

    suspend fun resendVerification(): ApiResult<AccountActionResponse> =
        withSession { api.resendVerification(it) }

    suspend fun changePassword(request: PasswordChangeRequest): ApiResult<AccountActionResponse> {
        val result = withSession { api.changePassword(it, request) }
        if (result is ApiResult.Success && result.value.reauthenticate) storage.clear()
        return result
    }

    suspend fun signOutEverywhere(): ApiResult<AccountActionResponse> {
        val result = withSession { api.signOutEverywhere(it) }
        if (result is ApiResult.Success) storage.clear()
        return result
    }

    suspend fun exportData(): ApiResult<String> = withSession { api.exportData(it) }

    suspend fun deactivateAccount(password: String): ApiResult<AccountActionResponse> {
        val result = withSession { api.deactivateAccount(it, password, "deactivate") }
        if (result is ApiResult.Success) storage.clear()
        return result
    }

    /** Refresh once and retry an authenticated operation; clear tokens only on a confirmed 401. */
    private suspend fun <T> withSession(operation: suspend (String) -> ApiResult<T>): ApiResult<T> {
        val tokens = storage.load() ?: return ApiResult.Unauthorized
        val first = operation(tokens.access)
        if (first != ApiResult.Unauthorized) return first
        return when (val refreshed = api.refresh(tokens.refresh)) {
            is ApiResult.Success -> {
                val updated = StoredTokens(refreshed.value.access, refreshed.value.refresh ?: tokens.refresh)
                storage.save(updated)
                val retry = operation(updated.access)
                if (retry == ApiResult.Unauthorized) storage.clear()
                retry
            }
            ApiResult.Unauthorized -> {
                storage.clear()
                ApiResult.Unauthorized
            }
            ApiResult.RateLimited -> ApiResult.RateLimited
            ApiResult.NetworkError -> ApiResult.NetworkError
            ApiResult.BadResponse -> ApiResult.BadResponse
            is ApiResult.HttpError -> refreshed
        }
    }

    /** Forgets the tokens on this device. The server's sign-out-everywhere action is separate. */
    suspend fun signOut() = storage.clear()

    private suspend fun fetchAccount(tokens: StoredTokens): ApiResult<Account> {
        val first = api.account(tokens.access)
        if (first != ApiResult.Unauthorized) return first
        // The access token is short-lived. Try the refresh token once.
        return when (val refreshed = api.refresh(tokens.refresh)) {
            is ApiResult.Success -> {
                val updated = StoredTokens(refreshed.value.access, refreshed.value.refresh ?: tokens.refresh)
                storage.save(updated)
                api.account(updated.access)
            }
            ApiResult.Unauthorized -> ApiResult.Unauthorized
            ApiResult.RateLimited -> ApiResult.RateLimited
            ApiResult.NetworkError -> ApiResult.NetworkError
            ApiResult.BadResponse -> ApiResult.BadResponse
            is ApiResult.HttpError -> refreshed
        }
    }
}
