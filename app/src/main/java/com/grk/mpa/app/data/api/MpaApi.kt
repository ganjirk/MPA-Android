package com.grk.mpa.app.data.api

/** The parts of the MPA website's `/api/v1/` that the app uses. */
interface MpaApi {
    suspend fun login(username: String, password: String): ApiResult<TokenPair>
    suspend fun refresh(refreshToken: String): ApiResult<RefreshedTokens>
    suspend fun account(accessToken: String): ApiResult<Account>
}
