package com.grk.mpa.app.data.auth

import com.grk.mpa.app.data.api.Account
import com.grk.mpa.app.data.api.ApiResult
import com.grk.mpa.app.data.api.MpaApi

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

    /** Forgets the tokens on this device. The server has no endpoint to revoke them yet; they expire on their own. */
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
