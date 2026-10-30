package com.grk.mpa.app.data.api

/** The parts of the MPA website's `/api/v1/` that the app uses. */
interface MpaApi {
    suspend fun login(username: String, password: String): ApiResult<TokenPair>
    suspend fun refresh(refreshToken: String): ApiResult<RefreshedTokens>
    suspend fun account(accessToken: String): ApiResult<Account>

    // Default implementations keep existing test doubles and alternate clients source-compatible.
    suspend fun tasks(accessToken: String): ApiResult<List<RemoteTask>> = ApiResult.HttpError(501)
    suspend fun createTask(accessToken: String, request: CreateTaskRequest): ApiResult<RemoteTask> =
        ApiResult.HttpError(501)
    suspend fun updateTask(accessToken: String, id: Long, request: UpdateTaskRequest): ApiResult<RemoteTask> =
        ApiResult.HttpError(501)
    suspend fun deleteTask(accessToken: String, id: Long): ApiResult<Unit> = ApiResult.HttpError(501)
}
