package com.grk.mpa.app.ui.support

import androidx.annotation.StringRes
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.grk.mpa.app.R
import com.grk.mpa.app.core.ContactError
import com.grk.mpa.app.core.ContactRules
import com.grk.mpa.app.core.ContactTopic
import com.grk.mpa.app.core.SentMessage
import com.grk.mpa.app.data.DataStoreContactHistoryRepository
import com.grk.mpa.app.data.api.WebsiteContactClient
import java.text.DateFormat
import java.util.Date
import com.grk.mpa.app.ui.theme.MpaDimensions

private val contactViewModelFactory = viewModelFactory {
    initializer {
        ContactViewModel(
            WebsiteContactClient(),
            historyRepository = DataStoreContactHistoryRepository(checkNotNull(this[APPLICATION_KEY]))
        )
    }
}

@StringRes
private fun ContactTopic.labelRes(): Int = when (this) {
    ContactTopic.QUESTION -> R.string.contact_topic_question
    ContactTopic.BUG -> R.string.contact_topic_bug
    ContactTopic.IDEA -> R.string.contact_topic_idea
    ContactTopic.ACCOUNT -> R.string.contact_topic_account
    ContactTopic.OTHER -> R.string.contact_topic_other
}

@Composable
fun ContactScreen(
    onBack: () -> Unit,
    onHelp: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ContactViewModel = viewModel(factory = contactViewModelFactory)
) {
    val draft = viewModel.draft
    val errors = viewModel.errors
    val outcome = viewModel.outcome
    val enabled = !viewModel.isSending

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = MpaDimensions.screenHorizontalPadding)
            .padding(top = MpaDimensions.compactSpacing, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(MpaDimensions.itemSpacing)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(MpaDimensions.compactSpacing)) {
            OutlinedButton(onClick = onBack) { Text(stringResource(R.string.support_back)) }
            OutlinedButton(onClick = onHelp) { Text(stringResource(R.string.support_help_link)) }
        }
        Text(
            stringResource(R.string.contact_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        if (outcome is ContactOutcome.Sent) {
            SentCard(outcome.reference, onAnother = viewModel::sendAnother)
            SentMessagesCard(viewModel.sentMessages)
            return@Column
        }

        Text(
            stringResource(R.string.contact_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        OutlinedTextField(
            value = draft.name,
            onValueChange = viewModel::onNameChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.contact_name)) },
            singleLine = true,
            enabled = enabled,
            isError = ContactError.NAME_REQUIRED in errors,
            supportingText = { if (ContactError.NAME_REQUIRED in errors) Text(stringResource(R.string.contact_error_name)) },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
        )
        OutlinedTextField(
            value = draft.email,
            onValueChange = viewModel::onEmailChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.contact_email)) },
            singleLine = true,
            enabled = enabled,
            isError = ContactError.EMAIL_INVALID in errors,
            supportingText = { if (ContactError.EMAIL_INVALID in errors) Text(stringResource(R.string.contact_error_email)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next)
        )
        Text(
            stringResource(R.string.contact_topic),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        Row(
            Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(MpaDimensions.compactSpacing)
        ) {
            ContactTopic.entries.forEach { topic ->
                FilterChip(
                    selected = draft.topic == topic,
                    onClick = { viewModel.onTopicChange(topic) },
                    enabled = enabled,
                    label = { Text(stringResource(topic.labelRes())) }
                )
            }
        }
        OutlinedTextField(
            value = draft.subject,
            onValueChange = viewModel::onSubjectChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.contact_subject)) },
            singleLine = true,
            enabled = enabled,
            isError = ContactError.SUBJECT_TOO_SHORT in errors,
            supportingText = { if (ContactError.SUBJECT_TOO_SHORT in errors) Text(stringResource(R.string.contact_error_subject)) },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
        )
        OutlinedTextField(
            value = draft.message,
            onValueChange = viewModel::onMessageChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.contact_message)) },
            minLines = 5,
            enabled = enabled,
            isError = ContactError.MESSAGE_TOO_SHORT in errors,
            supportingText = {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        if (ContactError.MESSAGE_TOO_SHORT in errors) stringResource(R.string.contact_error_message)
                        else stringResource(R.string.contact_hint_range)
                    )
                    Text(stringResource(R.string.contact_count, draft.message.length, ContactRules.MESSAGE_MAX))
                }
            }
        )
        failureText(outcome)?.let {
            Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
        }
        Button(onClick = viewModel::send, enabled = enabled) {
            Text(stringResource(if (viewModel.isSending) R.string.contact_sending else R.string.contact_send))
        }
        Text(
            stringResource(R.string.contact_privacy),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        SentMessagesCard(viewModel.sentMessages)
    }
}

@Composable
private fun failureText(outcome: ContactOutcome?): String? = when (outcome) {
    ContactOutcome.Rejected -> stringResource(R.string.contact_failed_rejected)
    ContactOutcome.RateLimited -> stringResource(R.string.contact_failed_rate_limited)
    ContactOutcome.NetworkError -> stringResource(R.string.contact_failed_network)
    ContactOutcome.Failed -> stringResource(R.string.contact_failed_other)
    else -> null
}

@Composable
private fun SentCard(reference: String?, onAnother: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(MpaDimensions.compactSpacing)) {
            Text(
                stringResource(R.string.contact_sent_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                stringResource(if (reference != null) R.string.contact_sent_body else R.string.contact_sent_no_reference),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            if (reference != null) {
                Text(
                    stringResource(R.string.contact_sent_reference),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                SelectionContainer {
                    Text(
                        reference,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            Button(onClick = onAnother) { Text(stringResource(R.string.contact_send_another)) }
        }
    }
}

/** The website's "My messages", kept on this device: what was sent and the reference ID to quote. */
@Composable
private fun SentMessagesCard(messages: List<SentMessage>) {
    if (messages.isEmpty()) return
    val format = remember { DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(MpaDimensions.compactSpacing)) {
            Text(
                stringResource(R.string.contact_history_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                stringResource(R.string.contact_history_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            messages.forEach { m ->
                Column(Modifier.fillMaxWidth().padding(top = 6.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    SelectionContainer {
                        Text(
                            m.reference,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(m.subject, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                    Text(
                        stringResource(m.topic.labelRes()) + " · " + format.format(Date(m.sentAtMillis)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        m.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3
                    )
                }
            }
        }
    }
}
