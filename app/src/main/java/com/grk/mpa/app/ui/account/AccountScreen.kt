package com.grk.mpa.app.ui.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.grk.mpa.app.R
import com.grk.mpa.app.data.api.Account
import com.grk.mpa.app.data.api.ApiConfig
import com.grk.mpa.app.data.api.MpaApiClient
import com.grk.mpa.app.data.auth.AuthRepository
import com.grk.mpa.app.data.auth.EncryptedAuthStorage
import com.grk.mpa.app.ui.theme.MpaDimensions

private val accountViewModelFactory = viewModelFactory {
    initializer {
        AccountViewModel(
            AuthRepository(MpaApiClient(), EncryptedAuthStorage(checkNotNull(this[APPLICATION_KEY])))
        )
    }
}

@Composable
fun AccountScreen(
    onOpenHelp: () -> Unit = {},
    onOpenContact: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: AccountViewModel = viewModel(factory = accountViewModelFactory)
) {
    Column(modifier.fillMaxSize()) {
        Box(Modifier.weight(1f)) {
            when (val state = viewModel.state) {
                AccountState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                AccountState.SignedOut -> SignedOutContent(viewModel, Modifier)
                AccountState.Offline -> OfflineContent(viewModel, Modifier)
                is AccountState.SignedIn -> SignedInContent(state.account, viewModel::signOut, Modifier)
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(horizontal = MpaDimensions.screenHorizontalPadding),
            horizontalArrangement = Arrangement.spacedBy(MpaDimensions.compactSpacing)
        ) {
            TextButton(onClick = onOpenHelp) { Text(stringResource(R.string.support_help_link)) }
            TextButton(onClick = onOpenContact) { Text(stringResource(R.string.support_contact_link)) }
        }
    }
}

@Composable
private fun ScreenColumn(modifier: Modifier, content: @Composable () -> Unit) {
    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = MpaDimensions.screenHorizontalPadding)
            .padding(top = MpaDimensions.screenTopPadding, bottom = MpaDimensions.screenTopPadding),
        verticalArrangement = Arrangement.spacedBy(MpaDimensions.itemSpacing)
    ) { content() }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground
    )
}

@Composable
private fun InfoCard(text: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Text(
            text,
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SignedOutContent(viewModel: AccountViewModel, modifier: Modifier) {
    val uriHandler = LocalUriHandler.current
    ScreenColumn(modifier) {
        SectionTitle(stringResource(R.string.account_sign_in_title))
        Text(
            stringResource(R.string.account_sign_in_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        OutlinedTextField(
            value = viewModel.username,
            onValueChange = viewModel::onUsernameChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.account_username)) },
            singleLine = true,
            enabled = !viewModel.isSubmitting,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next)
        )
        OutlinedTextField(
            value = viewModel.password,
            onValueChange = viewModel::onPasswordChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.account_password)) },
            singleLine = true,
            enabled = !viewModel.isSubmitting,
            visualTransformation = PasswordVisualTransformation(),
            isError = viewModel.error != null,
            supportingText = viewModel.error?.let { error -> { Text(stringResource(error.messageRes())) } },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { viewModel.signIn() })
        )
        Button(
            onClick = viewModel::signIn,
            enabled = viewModel.canSubmit,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (viewModel.isSubmitting) {
                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
            } else {
                Text(stringResource(R.string.account_sign_in))
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = { uriHandler.openUri(ApiConfig.website("signup/")) }) {
                Text(stringResource(R.string.account_create))
            }
            TextButton(onClick = { uriHandler.openUri(ApiConfig.website("forgot_password/")) }) {
                Text(stringResource(R.string.account_forgot_password))
            }
        }
        InfoCard(stringResource(R.string.account_local_data_note))
    }
}

@Composable
private fun OfflineContent(viewModel: AccountViewModel, modifier: Modifier) {
    ScreenColumn(modifier) {
        SectionTitle(stringResource(R.string.account_offline_title))
        Text(
            stringResource(R.string.account_offline_message),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Button(onClick = viewModel::restore, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.account_retry))
        }
        OutlinedButton(onClick = viewModel::signOut, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.account_sign_out))
        }
    }
}

@Composable
private fun SignedInContent(account: Account, onSignOut: () -> Unit, modifier: Modifier) {
    val uriHandler = LocalUriHandler.current
    var confirmSignOut by rememberSaveable { mutableStateOf(false) }

    ScreenColumn(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(64.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        account.displayName.take(1).uppercase(),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            Column {
                Text(
                    account.displayName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    stringResource(R.string.account_username_format, account.username),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (account.email.isNotBlank()) {
            InfoCard(
                stringResource(
                    if (account.emailVerified) R.string.account_email_verified else R.string.account_email_unverified,
                    account.email
                )
            )
        }
        InfoCard(stringResource(R.string.account_local_data_note))
        Button(
            onClick = { uriHandler.openUri(ApiConfig.website("user_profile/")) },
            modifier = Modifier.fillMaxWidth()
        ) { Text(stringResource(R.string.account_manage_on_website)) }
        OutlinedButton(onClick = { confirmSignOut = true }, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.account_sign_out))
        }
    }

    if (confirmSignOut) {
        AlertDialog(
            onDismissRequest = { confirmSignOut = false },
            title = { Text(stringResource(R.string.account_sign_out_confirm_title)) },
            text = { Text(stringResource(R.string.account_sign_out_confirm_message)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmSignOut = false
                    onSignOut()
                }) { Text(stringResource(R.string.account_sign_out)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmSignOut = false }) { Text(stringResource(R.string.account_cancel)) }
            }
        )
    }
}

private fun LoginError.messageRes(): Int = when (this) {
    LoginError.INVALID_CREDENTIALS -> R.string.account_error_invalid
    LoginError.RATE_LIMITED -> R.string.account_error_rate_limited
    LoginError.NETWORK -> R.string.account_error_network
    LoginError.OTHER -> R.string.account_error_other
}
