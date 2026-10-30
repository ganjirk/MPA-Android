package com.grk.mpa.app.ui.account

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grk.mpa.app.data.api.Account
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
                is SessionResult.Active -> AccountState.SignedIn(result.account)
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
                }
                SignInResult.InvalidCredentials -> error = LoginError.INVALID_CREDENTIALS
                SignInResult.RateLimited -> error = LoginError.RATE_LIMITED
                SignInResult.NetworkError -> error = LoginError.NETWORK
                SignInResult.Failed -> error = LoginError.OTHER
            }
            isSubmitting = false
        }
    }

    fun signOut() {
        scope.launch {
            repository.signOut()
            state = AccountState.SignedOut
        }
    }
}
