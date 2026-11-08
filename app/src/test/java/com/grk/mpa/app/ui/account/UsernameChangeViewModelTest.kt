package com.grk.mpa.app.ui.account

import com.grk.mpa.app.data.api.ApiResult
import com.grk.mpa.app.data.api.MpaApi
import com.grk.mpa.app.data.api.RemoteProfile
import com.grk.mpa.app.data.api.UpdateProfileRequest
import com.grk.mpa.app.data.api.UsernameAvailability
import com.grk.mpa.app.data.auth.AuthRepository
import com.grk.mpa.app.data.auth.FakeAuthStorage
import com.grk.mpa.app.data.auth.FakeMpaApi
import com.grk.mpa.app.data.auth.StoredTokens
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private class ProfileApi : MpaApi by FakeMpaApi() {
    var profile = RemoteProfile(7, "mobile-user", "Mobile", "Tester", "mobile@example.com")
    var availability: ApiResult<UsernameAvailability> = ApiResult.Success(UsernameAvailability(true, "Username is available."))
    var updateResult: ((UpdateProfileRequest) -> ApiResult<RemoteProfile>)? = null
    val checked = mutableListOf<String>()
    val saved = mutableListOf<UpdateProfileRequest>()

    override suspend fun profile(accessToken: String): ApiResult<RemoteProfile> = ApiResult.Success(profile)

    override suspend fun usernameAvailable(accessToken: String, username: String): ApiResult<UsernameAvailability> {
        checked += username
        return availability
    }

    override suspend fun updateProfile(accessToken: String, request: UpdateProfileRequest): ApiResult<RemoteProfile> {
        saved += request
        return updateResult?.invoke(request) ?: ApiResult.Success(profile.copy(username = request.username ?: profile.username))
    }
}

class UsernameChangeViewModelTest {
    private val api = ProfileApi()

    private fun viewModel(): AccountViewModel {
        val storage = FakeAuthStorage(StoredTokens("a1", "r1"))
        return AccountViewModel(AuthRepository(api, storage), CoroutineScope(Dispatchers.Unconfined), usernameDebounceMillis = 0)
    }

    private fun request(username: String?) = UpdateProfileRequest(
        "Mobile", "Tester", "mobile@example.com", "", "", "", "", "", true, "", "", "", "", "", username
    )

    @Test
    fun aFreeNameIsReportedAvailable() {
        val vm = viewModel()
        vm.checkUsername("fresh_name")
        assertEquals(UsernameCheck.Available("Username is available."), vm.usernameCheck)
        assertEquals(listOf("fresh_name"), api.checked)
    }

    @Test
    fun aTakenNameIsReportedWithTheServersMessage() {
        api.availability = ApiResult.Success(UsernameAvailability(false, "That username is already taken."))
        val vm = viewModel()
        vm.checkUsername("Taken")
        assertEquals(UsernameCheck.Unavailable("That username is already taken."), vm.usernameCheck)
    }

    @Test
    fun badFormatIsRefusedWithoutAskingTheServer() {
        val vm = viewModel()
        vm.checkUsername("ab")
        assertEquals(UsernameCheck.Unavailable("Username must be at least 3 characters."), vm.usernameCheck)
        vm.checkUsername("has space")
        assertTrue((vm.usernameCheck as UsernameCheck.Unavailable).message.startsWith("Use only letters"))
        assertTrue(api.checked.isEmpty())
    }

    @Test
    fun yourOwnCurrentNameNeedsNoCheck() {
        val vm = viewModel()
        vm.checkUsername("mobile-user")
        assertEquals(UsernameCheck.Idle, vm.usernameCheck)
        assertTrue(api.checked.isEmpty())
    }

    @Test
    fun aServerOrNetworkProblemJustHidesTheHint() {
        api.availability = ApiResult.NetworkError
        val vm = viewModel()
        vm.checkUsername("another")
        assertEquals(UsernameCheck.Idle, vm.usernameCheck)
    }

    @Test
    fun savingANewNameUpdatesTheAccount() {
        val vm = viewModel()
        vm.saveProfile(request("new_name"))
        assertEquals("new_name", api.saved.single().username)
        assertEquals("new_name", vm.profile?.username)
        assertEquals(UsernameCheck.Idle, vm.usernameCheck)
        assertEquals("new_name", (vm.state as AccountState.SignedIn).account.username)
    }

    @Test
    fun aRefusedNameExplainsWhy() {
        api.updateResult = { ApiResult.HttpError(400) }
        api.availability = ApiResult.Success(UsernameAvailability(false, "That username is already taken."))
        val vm = viewModel()
        vm.saveProfile(request("Taken"))
        assertEquals("That username is already taken.", vm.actionError)
    }

    @Test
    fun otherRefusalsKeepTheGeneralMessage() {
        api.updateResult = { ApiResult.HttpError(400) }
        val vm = viewModel()
        vm.saveProfile(request("fine_name"))
        assertTrue(vm.actionError!!.startsWith("Profile could not be saved"))
        vm.saveProfile(request(null))
        assertTrue(vm.actionError!!.startsWith("Profile could not be saved"))
    }

    @Test
    fun resettingClearsTheHint() {
        val vm = viewModel()
        vm.checkUsername("ab")
        vm.resetUsernameCheck()
        assertEquals(UsernameCheck.Idle, vm.usernameCheck)
    }
}
