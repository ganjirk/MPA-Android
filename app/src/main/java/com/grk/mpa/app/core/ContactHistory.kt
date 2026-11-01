package com.grk.mpa.app.core

import java.net.URLDecoder
import java.net.URLEncoder

/** A message that was sent from this device. The website keeps the same list for signed-in users ("My messages"). */
data class SentMessage(
    val sentAtMillis: Long,
    val reference: String,
    val topic: ContactTopic,
    val subject: String,
    val message: String
)

object ContactHistory {
    const val MAX_ENTRIES = 50

    /** Newest first, and never more than [MAX_ENTRIES]. */
    fun add(list: List<SentMessage>, entry: SentMessage): List<SentMessage> =
        (listOf(entry) + list.filterNot { it.reference == entry.reference }).take(MAX_ENTRIES)
}

/**
 * Plain text storage, one message per line, fields separated by tabs:
 * `sentAtMillis reference(url-encoded) topic subject(url-encoded) message(url-encoded)`.
 * Lines that cannot be read are skipped instead of failing the whole file.
 */
object ContactHistoryCodec {
    private const val TAB = "\t"
    private const val CHARSET = "UTF-8"

    private fun enc(text: String) = URLEncoder.encode(text, CHARSET)
    private fun dec(text: String) = runCatching { URLDecoder.decode(text, CHARSET) }.getOrNull()

    fun encode(list: List<SentMessage>): String = list.joinToString("\n") {
        listOf(it.sentAtMillis, enc(it.reference), it.topic.wire, enc(it.subject), enc(it.message)).joinToString(TAB)
    }

    fun decode(text: String): List<SentMessage> = text.lineSequence().mapNotNull { line ->
        val p = line.split(TAB)
        if (p.size != 5) return@mapNotNull null
        val at = p[0].toLongOrNull() ?: return@mapNotNull null
        val reference = dec(p[1])?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
        val topic = ContactTopic.entries.firstOrNull { it.wire == p[2] } ?: return@mapNotNull null
        val subject = dec(p[3]) ?: return@mapNotNull null
        val message = dec(p[4]) ?: return@mapNotNull null
        SentMessage(at, reference, topic, subject, message)
    }.take(ContactHistory.MAX_ENTRIES).toList()
}
