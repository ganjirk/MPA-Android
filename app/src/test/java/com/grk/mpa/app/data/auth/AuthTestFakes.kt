package com.grk.mpa.app.data.auth

import com.grk.mpa.app.data.api.Account
import com.grk.mpa.app.data.api.ApiResult
import com.grk.mpa.app.data.api.MpaApi
import com.grk.mpa.app.data.api.RefreshedTokens
import com.grk.mpa.app.data.api.TokenPair

val TEST_ACCOUNT = Account(7, "mobile-user", "Mobile", "Tester", "mobile@example.com", false)

class FakeMpaApi : MpaApi {
    var loginResult: ApiResult<TokenPair> = ApiResult.Success(TokenPair("a1", "r1"))
    var refreshResult: ApiResult<RefreshedTokens> = ApiResult.Success(RefreshedTokens("a2", "r2"))

    /** Results handed out by successive account calls; when empty, the call succeeds. */
    val accountResults = ArrayDeque<ApiResult<Account>>()

    val loginCalls = mutableListOf<Pair<String, String>>()
    val refreshCalls = mutableListOf<String>()
    val accountCalls = mutableListOf<String>()

    override suspend fun login(username: String, password: String): ApiResult<TokenPair> {
        loginCalls += username to password
        return loginResult
    }

    override suspend fun refresh(refreshToken: String): ApiResult<RefreshedTokens> {
        refreshCalls += refreshToken
        return refreshResult
    }

    override suspend fun account(accessToken: String): ApiResult<Account> {
        accountCalls += accessToken
        return accountResults.removeFirstOrNull() ?: ApiResult.Success(TEST_ACCOUNT)
    }
}

class FakeAuthStorage(var tokens: StoredTokens? = null) : AuthStorage {
    var clearCount = 0
    override suspend fun load(): StoredTokens? = tokens
    override suspend fun save(tokens: StoredTokens) {
        this.tokens = tokens
    }
    override suspend fun clear() {
        clearCount++
        tokens = null
    }
}
