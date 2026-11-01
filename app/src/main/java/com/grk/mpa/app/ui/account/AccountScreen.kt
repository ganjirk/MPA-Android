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
import androidx.compose.runtime.remember
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
import com.grk.mpa.app.data.api.RemoteProfile
import com.grk.mpa.app.data.api.UpdateProfileRequest
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
                is AccountState.SignedIn -> SignedInContent(state.account, viewModel, Modifier)
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
private fun SignedInContent(account: Account, viewModel: AccountViewModel, modifier: Modifier) {
    val profile = viewModel.profile
    var editing by rememberSaveable { mutableStateOf(false) }
    var confirmSignOut by rememberSaveable { mutableStateOf(false) }
    var confirmRevoke by rememberSaveable { mutableStateOf(false) }
    var showPasswordDialog by rememberSaveable { mutableStateOf(false) }
    var showDeactivateDialog by rememberSaveable { mutableStateOf(false) }

    var firstName by remember(profile) { mutableStateOf(profile?.firstName ?: account.firstName) }
    var lastName by remember(profile) { mutableStateOf(profile?.lastName ?: account.lastName) }
    var email by remember(profile) { mutableStateOf(profile?.email ?: account.email) }
    var phone by remember(profile) { mutableStateOf(profile?.phone.orEmpty()) }
    var mobile by remember(profile) { mutableStateOf(profile?.mobile.orEmpty()) }
    var address by remember(profile) { mutableStateOf(profile?.address.orEmpty()) }
    var designation by remember(profile) { mutableStateOf(profile?.designation.orEmpty()) }
    var sex by remember(profile) { mutableStateOf(profile?.sex.orEmpty()) }
    var website by remember(profile) { mutableStateOf(profile?.website.orEmpty()) }
    var github by remember(profile) { mutableStateOf(profile?.githubID.orEmpty()) }
    var twitter by remember(profile) { mutableStateOf(profile?.twitID.orEmpty()) }
    var instagram by remember(profile) { mutableStateOf(profile?.instaID.orEmpty()) }
    var facebook by remember(profile) { mutableStateOf(profile?.fbID.orEmpty()) }

    ScreenColumn(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(64.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        (profile?.displayName ?: account.displayName).take(1).uppercase(),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            Column {
                Text(
                    profile?.displayName ?: account.displayName,
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

        ProfilePhotoAction(viewModel, profile?.profilePictureUrl, profile?.displayName ?: account.displayName)

        if (viewModel.profileLoading) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                Text("Loading profile…", style = MaterialTheme.typography.bodySmall)
            }
        } else if (profile == null) {
            OutlinedButton(onClick = viewModel::loadProfile, modifier = Modifier.fillMaxWidth()) {
                Text("Retry loading profile")
            }
        }

        if (profile?.email?.isNotBlank() == true || account.email.isNotBlank()) {
            InfoCard(
                stringResource(
                    if (profile?.emailVerified ?: account.emailVerified) R.string.account_email_verified else R.string.account_email_unverified,
                    profile?.email ?: account.email
                )
            )
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            SectionTitle("Personal profile")
            TextButton(onClick = { editing = !editing }, enabled = profile != null) {
                Text(if (editing) "Close editor" else "Edit")
            }
        }

        if (editing && profile != null) {
            ProfileField("First name", firstName, { firstName = it })
            ProfileField("Last name", lastName, { lastName = it })
            ProfileField("Email", email, { email = it }, keyboardType = KeyboardType.Email)
            ProfileField("Phone", phone, { phone = it }, keyboardType = KeyboardType.Phone)
            ProfileField("Mobile", mobile, { mobile = it }, keyboardType = KeyboardType.Phone)
            ProfileField("Address", address, { address = it })
            ProfileField("Designation", designation, { designation = it })
            ProfileField("Sex (M, F, O or blank)", sex, { sex = it })
            SectionTitle("Online profiles")
            ProfileField("Website", website, { website = it })
            ProfileField("GitHub", github, { github = it })
            ProfileField("Twitter / X", twitter, { twitter = it })
            ProfileField("Instagram", instagram, { instagram = it })
            ProfileField("Facebook", facebook, { facebook = it })
            Button(
                onClick = {
                    viewModel.saveProfile(
                        UpdateProfileRequest(
                            firstName, lastName, email, phone, mobile, address, designation,
                            sex.trim().uppercase(), website, github, twitter, instagram, facebook
                        )
                    )
                },
                enabled = !viewModel.profileSaving && firstName.isNotBlank() && lastName.isNotBlank() && email.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (viewModel.profileSaving) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                else Text("Save profile")
            }
        } else if (profile != null) {
            InfoCard("Email: ${profile.email}\nPhone: ${profile.phone.ifBlank { "Not added" }}\nMobile: ${profile.mobile.ifBlank { "Not added" }}\nAddress: ${profile.address.ifBlank { "Not added" }}\nDesignation: ${profile.designation.ifBlank { "Not added" }}")
        }

        viewModel.actionMessage?.let { InfoCard(it) }
        viewModel.actionError?.let { InfoCard(it) }

        if (profile != null && !profile.emailVerified) {
            OutlinedButton(onClick = viewModel::resendVerification, modifier = Modifier.fillMaxWidth()) {
                Text("Resend email verification")
            }
        }
        OutlinedButton(onClick = { showPasswordDialog = true }, modifier = Modifier.fillMaxWidth()) {
            Text("Change password")
        }
        OutlinedButton(onClick = { confirmRevoke = true }, modifier = Modifier.fillMaxWidth()) {
            Text("Sign out everywhere")
        }
        DataExportAction(viewModel)
        OutlinedButton(onClick = { showDeactivateDialog = true }, modifier = Modifier.fillMaxWidth()) {
            Text("Deactivate account")
        }
        OutlinedButton(onClick = { confirmSignOut = true }, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.account_sign_out))
        }
        InfoCard("Your profile is stored securely with your MPA account. Password changes and sign-out actions may require you to sign in again.")
    }

    if (confirmSignOut) {
        AlertDialog(
            onDismissRequest = { confirmSignOut = false },
            title = { Text(stringResource(R.string.account_sign_out_confirm_title)) },
            text = { Text(stringResource(R.string.account_sign_out_confirm_message)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmSignOut = false
                    viewModel.signOut()
                }) { Text(stringResource(R.string.account_sign_out)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmSignOut = false }) { Text(stringResource(R.string.account_cancel)) }
            }
        )
    }

    if (confirmRevoke) {
        AlertDialog(
            onDismissRequest = { confirmRevoke = false },
            title = { Text("Sign out everywhere?") },
            text = { Text("This revokes registered refresh tokens on your account. You will need to sign in again on this device.") },
            confirmButton = {
                TextButton(onClick = { confirmRevoke = false; viewModel.signOutEverywhere() }) { Text("Continue") }
            },
            dismissButton = { TextButton(onClick = { confirmRevoke = false }) { Text("Cancel") } }
        )
    }

    if (showPasswordDialog) {
        PasswordChangeDialog(
            onDismiss = { showPasswordDialog = false },
            onSubmit = { current, new, confirm ->
                showPasswordDialog = false
                viewModel.changePassword(current, new, confirm)
            }
        )
    }

    if (showDeactivateDialog) {
        DeactivateAccountDialog(
            onDismiss = { showDeactivateDialog = false },
            onSubmit = { password ->
                showDeactivateDialog = false
                viewModel.deactivateAccount(password)
            }
        )
    }
}

@Composable
private fun ProfileField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType)
    )
}

@Composable
private fun PasswordChangeDialog(
    onDismiss: () -> Unit,
    onSubmit: (String, String, String) -> Unit
) {
    var current by rememberSaveable { mutableStateOf("") }
    var new by rememberSaveable { mutableStateOf("") }
    var confirm by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Change password") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(current, { current = it }, label = { Text("Current password") }, visualTransformation = PasswordVisualTransformation(), singleLine = true)
                OutlinedTextField(new, { new = it }, label = { Text("New password") }, visualTransformation = PasswordVisualTransformation(), singleLine = true)
                OutlinedTextField(confirm, { confirm = it }, label = { Text("Confirm new password") }, visualTransformation = PasswordVisualTransformation(), singleLine = true)
            }
        },
        confirmButton = {
            TextButton(onClick = { onSubmit(current, new, confirm) }, enabled = current.isNotEmpty() && new.isNotEmpty() && confirm.isNotEmpty()) { Text("Update") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun DeactivateAccountDialog(
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit
) {
    var password by rememberSaveable { mutableStateOf("") }
    var confirmation by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Deactivate account?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("You will no longer be able to sign in. Your account data will be retained.")
                OutlinedTextField(password, { password = it }, label = { Text("Current password") }, visualTransformation = PasswordVisualTransformation(), singleLine = true)
                OutlinedTextField(confirmation, { confirmation = it }, label = { Text("Type deactivate to confirm") }, singleLine = true)
            }
        },
        confirmButton = {
            TextButton(onClick = { onSubmit(password) }, enabled = password.isNotEmpty() && confirmation == "deactivate") { Text("Deactivate") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

private fun LoginError.messageRes(): Int = when (this) {
    LoginError.INVALID_CREDENTIALS -> R.string.account_error_invalid
    LoginError.RATE_LIMITED -> R.string.account_error_rate_limited
    LoginError.NETWORK -> R.string.account_error_network
    LoginError.OTHER -> R.string.account_error_other
}
