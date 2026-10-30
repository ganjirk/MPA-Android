package com.grk.mpa.app.data.auth

import com.grk.mpa.app.data.api.ApiResult
import com.grk.mpa.app.data.api.RefreshedTokens
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AuthRepositoryTest {
    private val api = FakeMpaApi()
    private val storage = FakeAuthStorage()
    private val repository = AuthRepository(api, storage)

    @Test
    fun signInStoresTokensAndReturnsTheAccount() = runBlocking {
        val result = repository.signIn("  mobile-user ", "pw")
        assertEquals(SignInResult.Success(TEST_ACCOUNT), result)
        assertEquals(StoredTokens("a1", "r1"), storage.tokens)
        assertEquals("mobile-user", api.loginCalls.single().first)
        assertEquals(listOf("a1"), api.accountCalls)
    }

    @Test
    fun wrongPasswordStoresNothing() = runBlocking {
        api.loginResult = ApiResult.Unauthorized
        assertEquals(SignInResult.InvalidCredentials, repository.signIn("u", "bad"))
        assertNull(storage.tokens)
    }

    @Test
    fun loginProblemsAreReportedDistinctly() = runBlocking {
        api.loginResult = ApiResult.RateLimited
        assertEquals(SignInResult.RateLimited, repository.signIn("u", "p"))
        api.loginResult = ApiResult.NetworkError
        assertEquals(SignInResult.NetworkError, repository.signIn("u", "p"))
        api.loginResult = ApiResult.HttpError(500)
        assertEquals(SignInResult.Failed, repository.signIn("u", "p"))
        api.loginResult = ApiResult.BadResponse
        assertEquals(SignInResult.Failed, repository.signIn("u", "p"))
        assertNull(storage.tokens)
    }

    @Test
    fun failingToLoadTheAccountAfterLoginDoesNotLeaveTokensBehind() = runBlocking {
        api.accountResults += ApiResult.NetworkError
        assertEquals(SignInResult.NetworkError, repository.signIn("u", "p"))
        assertNull(storage.tokens)

        api.accountResults += ApiResult.HttpError(500)
        assertEquals(SignInResult.Failed, repository.signIn("u", "p"))
        assertNull(storage.tokens)
    }

    @Test
    fun noStoredSessionMeansSignedOut() = runBlocking {
        assertEquals(SessionResult.SignedOut, repository.restoreSession())
        assertEquals(emptyList<String>(), api.accountCalls)
    }

    @Test
    fun validStoredSessionIsRestored() = runBlocking {
        storage.tokens = StoredTokens("a1", "r1")
        assertEquals(SessionResult.Active(TEST_ACCOUNT), repository.restoreSession())
        assertEquals(emptyList<String>(), api.refreshCalls)
    }

    @Test
    fun expiredAccessTokenIsRefreshedAndTheRotatedTokenSaved() = runBlocking {
        storage.tokens = StoredTokens("old-access", "r1")
        api.accountResults += ApiResult.Unauthorized
        assertEquals(SessionResult.Active(TEST_ACCOUNT), repository.restoreSession())
        assertEquals(listOf("r1"), api.refreshCalls)
        assertEquals(listOf("old-access", "a2"), api.accountCalls)
        assertEquals(StoredTokens("a2", "r2"), storage.tokens)
    }

    @Test
    fun theOldRefreshTokenIsKeptWhenTheServerSendsNoNewOne() = runBlocking {
        storage.tokens = StoredTokens("old-access", "r1")
        api.accountResults += ApiResult.Unauthorized
        api.refreshResult = ApiResult.Success(RefreshedTokens("a2", null))
        repository.restoreSession()
        assertEquals(StoredTokens("a2", "r1"), storage.tokens)
    }

    @Test
    fun anExpiredRefreshTokenSignsTheUserOut() = runBlocking {
        storage.tokens = StoredTokens("old-access", "old-refresh")
        api.accountResults += ApiResult.Unauthorized
        api.refreshResult = ApiResult.Unauthorized
        assertEquals(SessionResult.SignedOut, repository.restoreSession())
        assertNull(storage.tokens)
    }

    @Test
    fun networkProblemsNeverSignTheUserOut() = runBlocking {
        storage.tokens = StoredTokens("a1", "r1")
        api.accountResults += ApiResult.NetworkError
        assertEquals(SessionResult.Offline, repository.restoreSession())
        assertEquals(StoredTokens("a1", "r1"), storage.tokens)

        api.accountResults += ApiResult.Unauthorized
        api.refreshResult = ApiResult.NetworkError
        assertEquals(SessionResult.Offline, repository.restoreSession())
        assertEquals(StoredTokens("a1", "r1"), storage.tokens)

        api.accountResults += ApiResult.HttpError(503)
        assertEquals(SessionResult.Offline, repository.restoreSession())
        assertEquals(0, storage.clearCount)
    }

    @Test
    fun signOutClearsTheTokens() = runBlocking {
        storage.tokens = StoredTokens("a1", "r1")
        repository.signOut()
        assertNull(storage.tokens)
    }
}
