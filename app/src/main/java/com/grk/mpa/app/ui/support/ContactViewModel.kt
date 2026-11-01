package com.grk.mpa.app.ui.support

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grk.mpa.app.core.ContactDraft
import com.grk.mpa.app.core.ContactError
import com.grk.mpa.app.core.ContactHistory
import com.grk.mpa.app.core.ContactRules
import com.grk.mpa.app.core.ContactTopic
import com.grk.mpa.app.core.SentMessage
import com.grk.mpa.app.data.ContactHistoryRepository
import com.grk.mpa.app.data.api.ContactApi
import com.grk.mpa.app.data.api.ContactResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** What happened to the last attempt to send. */
sealed interface ContactOutcome {
    /** [reference] may be null if the website saved the message but did not show one. */
    data class Sent(val reference: String?) : ContactOutcome
    data object Rejected : ContactOutcome
    data object RateLimited : ContactOutcome
    data object NetworkError : ContactOutcome
    data object Failed : ContactOutcome
}

class ContactViewModel(
    private val api: ContactApi,
    private val coroutineScope: CoroutineScope? = null,
    private val historyRepository: ContactHistoryRepository? = null,
    private val clock: () -> Long = System::currentTimeMillis
) : ViewModel() {
    private val scope: CoroutineScope get() = coroutineScope ?: viewModelScope

    var draft by mutableStateOf(ContactDraft())
        private set

    /** Problems are only shown after the person has tried to send. */
    var errors by mutableStateOf<Set<ContactError>>(emptySet())
        private set
    var isSending by mutableStateOf(false)
        private set
    var outcome by mutableStateOf<ContactOutcome?>(null)
        private set

    /** Messages sent from this device, newest first. */
    var sentMessages by mutableStateOf<List<SentMessage>>(emptyList())
        private set

    init {
        historyRepository?.let { repo -> scope.launch { sentMessages = repo.load() } }
    }

    fun onNameChange(text: String) = edit { it.copy(name = text.take(ContactRules.NAME_MAX)) }
    fun onEmailChange(text: String) = edit { it.copy(email = text) }
    fun onTopicChange(topic: ContactTopic) = edit { it.copy(topic = topic) }
    fun onSubjectChange(text: String) = edit { it.copy(subject = text.take(ContactRules.SUBJECT_MAX)) }
    fun onMessageChange(text: String) = edit { it.copy(message = text.take(ContactRules.MESSAGE_MAX)) }

    private fun edit(change: (ContactDraft) -> ContactDraft) {
        if (isSending) return
        draft = change(draft)
        if (errors.isNotEmpty()) errors = ContactRules.validate(draft)
        if (outcome != null && outcome !is ContactOutcome.Sent) outcome = null
    }

    fun send() {
        if (isSending || outcome is ContactOutcome.Sent) return
        val problems = ContactRules.validate(draft)
        errors = problems
        if (problems.isNotEmpty()) return
        isSending = true
        outcome = null
        val clean = ContactRules.clean(draft)
        scope.launch {
            outcome = when (val result = api.send(clean)) {
                is ContactResult.Sent -> {
                    draft = ContactDraft(topic = clean.topic, name = clean.name, email = clean.email)
                    result.reference?.let { remember(it, clean) }
                    ContactOutcome.Sent(result.reference)
                }
                ContactResult.Rejected -> ContactOutcome.Rejected
                ContactResult.RateLimited -> ContactOutcome.RateLimited
                ContactResult.NetworkError -> ContactOutcome.NetworkError
                is ContactResult.Failed -> ContactOutcome.Failed
            }
            isSending = false
        }
    }

    /** After a message was sent: back to an empty form (name and email are kept). */
    fun sendAnother() {
        outcome = null
        errors = emptySet()
    }

    private fun remember(reference: String, sent: ContactDraft) {
        val updated = ContactHistory.add(
            sentMessages,
            SentMessage(clock(), reference, sent.topic, sent.subject, sent.message)
        )
        sentMessages = updated
        historyRepository?.let { repo -> scope.launch { repo.save(updated) } }
    }
}
