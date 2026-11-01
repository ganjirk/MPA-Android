package com.grk.mpa.app.ui.support

import com.grk.mpa.app.core.ContactDraft
import com.grk.mpa.app.core.ContactError
import com.grk.mpa.app.core.ContactTopic
import com.grk.mpa.app.core.SentMessage
import com.grk.mpa.app.data.ContactHistoryRepository
import com.grk.mpa.app.data.api.ContactApi
import com.grk.mpa.app.data.api.ContactResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private class FakeContactApi(var result: ContactResult) : ContactApi {
    val sent = mutableListOf<ContactDraft>()
    override suspend fun send(draft: ContactDraft): ContactResult {
        sent += draft
        return result
    }
}

private class FakeHistory(var stored: List<SentMessage> = emptyList()) : ContactHistoryRepository {
    override suspend fun load() = stored
    override suspend fun save(messages: List<SentMessage>) {
        stored = messages
    }
}

class ContactViewModelTest {
    private fun viewModel(api: FakeContactApi, history: FakeHistory = FakeHistory()) =
        ContactViewModel(api, CoroutineScope(Dispatchers.Unconfined), history) { 1_000L }

    private fun ContactViewModel.fill() {
        onNameChange("Asha")
        onEmailChange("asha@example.com")
        onTopicChange(ContactTopic.IDEA)
        onSubjectChange("A new game")
        onMessageChange("Please add Match Cards to the app.")
    }

    @Test
    fun invalidFormShowsErrorsAndSendsNothing() {
        val api = FakeContactApi(ContactResult.Sent("X"))
        val vm = viewModel(api)
        vm.send()
        assertEquals(4, vm.errors.size)
        assertTrue(api.sent.isEmpty())
        vm.onNameChange("Asha")
        assertFalse(ContactError.NAME_REQUIRED in vm.errors) // errors update as the person fixes them
    }

    @Test
    fun sentMessageShowsReferenceAndIsRemembered() {
        val history = FakeHistory()
        val api = FakeContactApi(ContactResult.Sent("MPA-123"))
        val vm = viewModel(api, history)
        vm.fill()
        vm.send()
        assertEquals(ContactOutcome.Sent("MPA-123"), vm.outcome)
        assertEquals(1, api.sent.size)
        assertEquals("A new game", vm.sentMessages.single().subject)
        assertEquals("MPA-123", history.stored.single().reference)
        assertEquals(1_000L, history.stored.single().sentAtMillis)
        assertEquals("", vm.draft.message) // form is empty again, name and email are kept
        assertEquals("Asha", vm.draft.name)
    }

    @Test
    fun savedHistoryIsLoaded() {
        val old = SentMessage(5L, "OLD-1", ContactTopic.BUG, "Old", "Old message text")
        val vm = viewModel(FakeContactApi(ContactResult.NetworkError), FakeHistory(listOf(old)))
        assertEquals(listOf(old), vm.sentMessages)
    }

    @Test
    fun failuresKeepTheDraftAndAreNotRemembered() {
        val cases = mapOf(
            ContactResult.Rejected to ContactOutcome.Rejected,
            ContactResult.RateLimited to ContactOutcome.RateLimited,
            ContactResult.NetworkError to ContactOutcome.NetworkError,
            ContactResult.Failed(500) to ContactOutcome.Failed
        )
        cases.forEach { (result, expected) ->
            val vm = viewModel(FakeContactApi(result))
            vm.fill()
            vm.send()
            assertEquals(expected, vm.outcome)
            assertEquals("A new game", vm.draft.subject)
            assertTrue(vm.sentMessages.isEmpty())
            assertFalse(vm.isSending)
            vm.onSubjectChange("A newer game") // editing clears the failure message
            assertEquals(null, vm.outcome)
        }
    }

    @Test
    fun sentWithoutReferenceIsNotListed() {
        val vm = viewModel(FakeContactApi(ContactResult.Sent(null)))
        vm.fill()
        vm.send()
        assertEquals(ContactOutcome.Sent(null), vm.outcome)
        assertTrue(vm.sentMessages.isEmpty())
    }

    @Test
    fun sendAnotherReturnsToTheForm() {
        val vm = viewModel(FakeContactApi(ContactResult.Sent("R")))
        vm.fill()
        vm.send()
        vm.sendAnother()
        assertEquals(null, vm.outcome)
    }
}
