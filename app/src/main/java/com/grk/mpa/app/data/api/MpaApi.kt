package com.grk.mpa.app.data.api

/** The parts of the MPA website's `/api/v1/` that the app uses. */
interface MpaApi {
    suspend fun login(username: String, password: String): ApiResult<TokenPair>
    suspend fun refresh(refreshToken: String): ApiResult<RefreshedTokens>
    suspend fun account(accessToken: String): ApiResult<Account>
    suspend fun profile(accessToken: String): ApiResult<RemoteProfile> = ApiResult.HttpError(501)
    suspend fun updateProfile(accessToken: String, request: UpdateProfileRequest): ApiResult<RemoteProfile> = ApiResult.HttpError(501)
    suspend fun uploadProfilePicture(accessToken: String, bytes: ByteArray, mimeType: String, fileName: String): ApiResult<RemoteProfile> = ApiResult.HttpError(501)
    suspend fun resendVerification(accessToken: String): ApiResult<AccountActionResponse> = ApiResult.HttpError(501)
    suspend fun changePassword(accessToken: String, request: PasswordChangeRequest): ApiResult<AccountActionResponse> = ApiResult.HttpError(501)
    suspend fun signOutEverywhere(accessToken: String): ApiResult<AccountActionResponse> = ApiResult.HttpError(501)
    suspend fun deactivateAccount(accessToken: String, password: String, confirmation: String): ApiResult<AccountActionResponse> = ApiResult.HttpError(501)

    // Default implementations keep existing test doubles and alternate clients source-compatible.
    suspend fun tasks(accessToken: String): ApiResult<List<RemoteTask>> = ApiResult.HttpError(501)
    suspend fun createTask(accessToken: String, request: CreateTaskRequest): ApiResult<RemoteTask> =
        ApiResult.HttpError(501)
    suspend fun updateTask(accessToken: String, id: Long, request: UpdateTaskRequest): ApiResult<RemoteTask> =
        ApiResult.HttpError(501)
    suspend fun deleteTask(accessToken: String, id: Long): ApiResult<Unit> = ApiResult.HttpError(501)

    suspend fun expenses(accessToken: String, month: String? = null): ApiResult<List<RemoteExpense>> =
        ApiResult.HttpError(501)
    suspend fun createExpense(accessToken: String, request: CreateExpenseRequest): ApiResult<RemoteExpense> =
        ApiResult.HttpError(501)
    suspend fun updateExpense(accessToken: String, id: Long, request: UpdateExpenseRequest): ApiResult<RemoteExpense> =
        ApiResult.HttpError(501)
    suspend fun deleteExpense(accessToken: String, id: Long): ApiResult<Unit> = ApiResult.HttpError(501)
    suspend fun expenseSummary(accessToken: String, month: String): ApiResult<RemoteExpenseSummary> =
        ApiResult.HttpError(501)
    suspend fun budgets(accessToken: String): ApiResult<List<RemoteMonthlyBudget>> = ApiResult.HttpError(501)
    suspend fun saveBudget(accessToken: String, request: SaveMonthlyBudgetRequest): ApiResult<RemoteMonthlyBudget> =
        ApiResult.HttpError(501)
}
