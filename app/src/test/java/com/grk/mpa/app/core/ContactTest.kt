package com.grk.mpa.app.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ContactTest {
    private val valid = ContactDraft("Asha", "asha@example.com", ContactTopic.BUG, "Crash on open", "The app closes when I open Chess.")

    @Test
    fun validDraftHasNoProblems() {
        assertTrue(ContactRules.validate(valid).isEmpty())
    }

    @Test
    fun reportsEveryProblem() {
        assertEquals(
            setOf(ContactError.NAME_REQUIRED, ContactError.EMAIL_INVALID, ContactError.SUBJECT_TOO_SHORT, ContactError.MESSAGE_TOO_SHORT),
            ContactRules.validate(ContactDraft(name = "  ", email = "nope", subject = "hi", message = "short"))
        )
    }

    @Test
    fun textIsTrimmedBeforeChecking() {
        val padded = valid.copy(subject = "  abc ", message = "   too short ")
        assertEquals(setOf(ContactError.SUBJECT_TOO_SHORT, ContactError.MESSAGE_TOO_SHORT), ContactRules.validate(padded))
        assertEquals("abc", ContactRules.clean(padded).subject)
    }

    @Test
    fun emailNeedsAnAtAndADot() {
        listOf("a@b", "a b@c.com", "@c.com", "a@.com").forEach {
            assertTrue(it, ContactError.EMAIL_INVALID in ContactRules.validate(valid.copy(email = it)))
        }
        assertTrue(ContactRules.validate(valid.copy(email = "a.b+c@d.co.in")).isEmpty())
    }

    @Test
    fun topicsUseTheWebsiteValues() {
        assertEquals(listOf("question", "bug", "idea", "account", "other"), ContactTopic.entries.map { it.wire })
    }
}
