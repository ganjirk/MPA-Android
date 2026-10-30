package com.grk.mpa.app.ui.account

import com.grk.mpa.app.data.api.ApiResult
import com.grk.mpa.app.data.auth.AuthRepository
import com.grk.mpa.app.data.auth.FakeAuthStorage
import com.grk.mpa.app.data.auth.FakeMpaApi
import com.grk.mpa.app.data.auth.StoredTokens
import com.grk.mpa.app.data.auth.TEST_ACCOUNT
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AccountViewModelTest {
    private val api = FakeMpaApi()
    private val storage = FakeAuthStorage()

    private fun viewModel() =
        AccountViewModel(AuthRepository(api, storage), CoroutineScope(Dispatchers.Unconfined))

    @Test
    fun startsSignedOutWhenNothingIsStored() {
        assertEquals(AccountState.SignedOut, viewModel().state)
    }

    @Test
    fun restoresAStoredSession() {
        storage.tokens = StoredTokens("a1", "r1")
        assertEquals(AccountState.SignedIn(TEST_ACCOUNT), viewModel().state)
    }

    @Test
    fun showsOfflineWhenTheServerCannotBeReachedThenRetries() {
        storage.tokens = StoredTokens("a1", "r1")
        api.accountResults += ApiResult.NetworkError
        val vm = viewModel()
        assertEquals(AccountState.Offline, vm.state)
        vm.restore()
        assertEquals(AccountState.SignedIn(TEST_ACCOUNT), vm.state)
    }

    @Test
    fun cannotSubmitUntilBothFieldsAreFilled() {
        val vm = viewModel()
        assertFalse(vm.canSubmit)
        vm.onUsernameChange("mobile-user")
        assertFalse(vm.canSubmit)
        vm.onPasswordChange("pw")
        assertTrue(vm.canSubmit)
        vm.onUsernameChange("   ")
        assertFalse(vm.canSubmit)
    }

    @Test
    fun successfulSignInShowsTheAccountAndClearsTheForm() {
        val vm = viewModel()
        vm.onUsernameChange("mobile-user")
        vm.onPasswordChange("pw")
        vm.signIn()
        assertEquals(AccountState.SignedIn(TEST_ACCOUNT), vm.state)
        assertEquals("", vm.username)
        assertEquals("", vm.password)
        assertFalse(vm.isSubmitting)
        assertEquals(StoredTokens("a1", "r1"), storage.tokens)
    }

    @Test
    fun wrongPasswordShowsAnErrorUntilTheUserEdits() {
        api.loginResult = ApiResult.Unauthorized
        val vm = viewModel()
        vm.onUsernameChange("u")
        vm.onPasswordChange("bad")
        vm.signIn()
        assertEquals(LoginError.INVALID_CREDENTIALS, vm.error)
        assertEquals(AccountState.SignedOut, vm.state)
        assertFalse(vm.isSubmitting)
        vm.onPasswordChange("better")
        assertNull(vm.error)
    }

    @Test
    fun otherFailuresMapToTheirOwnMessages() {
        val vm = viewModel()
        vm.onUsernameChange("u")
        vm.onPasswordChange("p")
        api.loginResult = ApiResult.RateLimited
        vm.signIn()
        assertEquals(LoginError.RATE_LIMITED, vm.error)
        api.loginResult = ApiResult.NetworkError
        vm.signIn()
        assertEquals(LoginError.NETWORK, vm.error)
        api.loginResult = ApiResult.HttpError(500)
        vm.signIn()
        assertEquals(LoginError.OTHER, vm.error)
    }

    @Test
    fun signOutClearsTheStoredSession() {
        storage.tokens = StoredTokens("a1", "r1")
        val vm = viewModel()
        vm.signOut()
        assertEquals(AccountState.SignedOut, vm.state)
        assertNull(storage.tokens)
    }
}
