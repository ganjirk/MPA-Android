package com.grk.mpa.app.core

import org.junit.Assert.assertEquals
import org.junit.Test

class ContactHistoryTest {
    private fun msg(ref: String, at: Long = 1L) = SentMessage(at, ref, ContactTopic.IDEA, "Subject $ref", "Line one\nTab\there & more ₹")

    @Test
    fun newestComesFirstAndDuplicatesAreReplaced() {
        var list = ContactHistory.add(emptyList(), msg("A"))
        list = ContactHistory.add(list, msg("B"))
        list = ContactHistory.add(list, msg("A", at = 9))
        assertEquals(listOf("A", "B"), list.map { it.reference })
        assertEquals(9L, list[0].sentAtMillis)
    }

    @Test
    fun listIsCapped() {
        var list = emptyList<SentMessage>()
        repeat(ContactHistory.MAX_ENTRIES + 10) { list = ContactHistory.add(list, msg("R$it")) }
        assertEquals(ContactHistory.MAX_ENTRIES, list.size)
        assertEquals("R${ContactHistory.MAX_ENTRIES + 9}", list.first().reference)
    }

    @Test
    fun roundTripKeepsTextWithNewlinesTabsAndSymbols() {
        val list = listOf(msg("MPA-1", 5), msg("MPA-2", 4))
        assertEquals(list, ContactHistoryCodec.decode(ContactHistoryCodec.encode(list)))
    }

    @Test
    fun unreadableLinesAreSkipped() {
        val good = ContactHistoryCodec.encode(listOf(msg("OK")))
        val text = "garbage\n$good\n1\tref\tnotatopic\ta\tb\nx\tref\tbug\ta\tb"
        assertEquals(listOf("OK"), ContactHistoryCodec.decode(text).map { it.reference })
        assertEquals(emptyList<SentMessage>(), ContactHistoryCodec.decode(""))
    }
}
