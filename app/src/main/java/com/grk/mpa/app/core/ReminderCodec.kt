package com.grk.mpa.app.core

import java.net.URLDecoder
import java.net.URLEncoder

object ReminderCodec {
    fun encode(items: List<Reminder>): String = items.joinToString("\n") {
        "${it.id}\t${it.triggerAtMillis}\t${it.isCompleted}\t${URLEncoder.encode(it.title, "UTF-8")}"
    }

    fun decode(raw: String): List<Reminder> = raw.lineSequence().mapNotNull { line ->
        val p = line.split('\t')
        if (p.size != 4) return@mapNotNull null
        val id = p[0].toLongOrNull() ?: return@mapNotNull null
        val at = p[1].toLongOrNull() ?: return@mapNotNull null
        val done = p[2].toBooleanStrictOrNull() ?: return@mapNotNull null
        val title = runCatching { URLDecoder.decode(p[3], "UTF-8") }.getOrNull()
            ?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
        Reminder(id, title, at, done)
    }.toList()
}
