package com.grk.mpa.app.ui.account

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grk.mpa.app.data.api.Account
import com.grk.mpa.app.data.api.ApiResult
import com.grk.mpa.app.data.api.RemoteProfile
import com.grk.mpa.app.data.api.UpdateProfileRequest
import com.grk.mpa.app.data.api.PasswordChangeRequest
import com.grk.mpa.app.data.auth.AuthRepository
import com.grk.mpa.app.data.auth.SessionResult
import com.grk.mpa.app.data.auth.SignInResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

sealed interface AccountState {
    /** Checking whether a stored session is still valid. */
    data object Loading : AccountState
    data object SignedOut : AccountState
    data class SignedIn(val account: Account) : AccountState

    /** A session is stored but the server could not be reached. */
    data object Offline : AccountState
}

enum class LoginError { INVALID_CREDENTIALS, RATE_LIMITED, NETWORK, OTHER }

/**
 * @param coroutineScope scope for network calls; null means [viewModelScope]. Tests pass their own.
 */
class AccountViewModel(
    private val repository: AuthRepository,
    private val coroutineScope: CoroutineScope? = null
) : ViewModel() {
    private val scope: CoroutineScope get() = coroutineScope ?: viewModelScope

    var state by mutableStateOf<AccountState>(AccountState.Loading)
        private set
    var username by mutableStateOf("")
        private set

    /** Held in memory only while the form is open; cleared after every attempt that succeeds. */
    var password by mutableStateOf("")
        private set
    var isSubmitting by mutableStateOf(false)
        private set
    var error by mutableStateOf<LoginError?>(null)
        private set
    var profile by mutableStateOf<RemoteProfile?>(null)
        private set
    var profileLoading by mutableStateOf(false)
        private set
    var profileSaving by mutableStateOf(false)
        private set
    var actionMessage by mutableStateOf<String?>(null)
        private set
    var actionError by mutableStateOf<String?>(null)
        private set

    val canSubmit: Boolean get() = !isSubmitting && username.isNotBlank() && password.isNotEmpty()

    init {
        restore()
    }

    fun onUsernameChange(text: String) {
        username = text
        error = null
    }

    fun onPasswordChange(text: String) {
        password = text
        error = null
    }

    /** Checks the stored session. Also used by the Retry button when offline. */
    fun restore() {
        state = AccountState.Loading
        scope.launch {
            state = when (val result = repository.restoreSession()) {
                is SessionResult.Active -> AccountState.SignedIn(result.account).also { loadProfile() }
                SessionResult.SignedOut -> AccountState.SignedOut
                SessionResult.Offline -> AccountState.Offline
            }
        }
    }

    fun signIn() {
        if (!canSubmit) return
        isSubmitting = true
        error = null
        scope.launch {
            when (val result = repository.signIn(username, password)) {
                is SignInResult.Success -> {
                    username = ""
                    password = ""
                    state = AccountState.SignedIn(result.account)
                    loadProfile()
                }
                SignInResult.InvalidCredentials -> error = LoginError.INVALID_CREDENTIALS
                SignInResult.RateLimited -> error = LoginError.RATE_LIMITED
                SignInResult.NetworkError -> error = LoginError.NETWORK
                SignInResult.Failed -> error = LoginError.OTHER
            }
            isSubmitting = false
        }
    }


    fun loadProfile() {
        profileLoading = true
        scope.launch {
            when (val result = repository.profile()) {
                is ApiResult.Success -> profile = result.value
                else -> actionError = "Could not load profile. Please try again."
            }
            profileLoading = false
        }
    }

    fun saveProfile(request: UpdateProfileRequest) {
        if (profileSaving) return
        profileSaving = true
        actionMessage = null
        actionError = null
        scope.launch {
            when (val result = repository.updateProfile(request)) {
                is ApiResult.Success -> {
                    profile = result.value
                    actionMessage = "Profile saved."
                    state = AccountState.SignedIn(
                        Account(
                            id = result.value.id,
                            username = result.value.username,
                            firstName = result.value.firstName,
                            lastName = result.value.lastName,
                            email = result.value.email,
                            emailVerified = result.value.emailVerified
                        )
                    )
                }
                ApiResult.Unauthorized -> actionError = "Your session expired. Please sign in again."
                ApiResult.NetworkError -> actionError = "Network unavailable. Your changes were not saved."
                is ApiResult.HttpError -> actionError = "Profile could not be saved. Check the fields and try again."
                else -> actionError = "Profile could not be saved. Please try again."
            }
            profileSaving = false
        }
    }


    fun uploadProfilePicture(bytes: ByteArray, mimeType: String, fileName: String) {
        profileSaving = true
        actionMessage = null
        actionError = null
        scope.launch {
            when (val result = repository.uploadProfilePicture(bytes, mimeType, fileName)) {
                is ApiResult.Success -> {
                    profile = result.value
                    actionMessage = "Profile picture updated."
                }
                ApiResult.NetworkError -> actionError = "Network unavailable. Picture was not uploaded."
                else -> actionError = "Picture could not be uploaded. Choose a JPG, PNG or WebP under 1 MB."
            }
            profileSaving = false
        }
    }

    fun resendVerification() {
        actionMessage = null
        actionError = null
        scope.launch {
            when (val result = repository.resendVerification()) {
                is ApiResult.Success -> actionMessage = result.value.detail
                ApiResult.NetworkError -> actionError = "Network unavailable. Please try again."
                else -> actionError = "Could not send the verification email. Please try again."
            }
        }
    }

    fun changePassword(current: String, new: String, confirm: String) {
        actionMessage = null
        actionError = null
        scope.launch {
            when (val result = repository.changePassword(PasswordChangeRequest(current, new, confirm))) {
                is ApiResult.Success -> {
                    state = AccountState.SignedOut
                    profile = null
                    actionMessage = result.value.detail
                }
                ApiResult.NetworkError -> actionError = "Network unavailable. Password was not changed."
                else -> actionError = "Password could not be changed. Check your current password and new password."
            }
        }
    }

    fun signOutEverywhere() {
        actionMessage = null
        actionError = null
        scope.launch {
            when (val result = repository.signOutEverywhere()) {
                is ApiResult.Success -> {
                    state = AccountState.SignedOut
                    profile = null
                    actionMessage = result.value.detail
                }
                ApiResult.NetworkError -> actionError = "Network unavailable. Could not revoke other sessions."
                else -> actionError = "Could not sign out other sessions. Please try again."
            }
        }
    }

    fun deactivateAccount(password: String) {
        actionMessage = null
        actionError = null
        scope.launch {
            when (val result = repository.deactivateAccount(password)) {
                is ApiResult.Success -> {
                    state = AccountState.SignedOut
                    profile = null
                    actionMessage = result.value.detail
                }
                ApiResult.NetworkError -> actionError = "Network unavailable. Account was not deactivated."
                else -> actionError = "Account could not be deactivated. Check your password."
            }
        }
    }

    fun signOut() {
        scope.launch {
            repository.signOut()
            state = AccountState.SignedOut
        }
    }
}
