package com.grk.mpa.app.core

import java.net.URLDecoder
import java.net.URLEncoder

/**
 * Stores a task list as plain text, one task per line:
 * `id <TAB> done <TAB> createdAtMillis <TAB> url-encoded title`.
 * The title is URL-encoded, so tabs and line breaks in it cannot break the format.
 * Lines that cannot be read are skipped rather than failing the whole list.
 */
object TaskCodec {
    private const val SEPARATOR = '\t'
    private const val CHARSET = "UTF-8"

    fun encode(tasks: List<Task>): String = tasks.joinToString("\n") { task ->
        listOf(
            task.id.toString(),
            task.isDone.toString(),
            task.createdAtMillis.toString(),
            URLEncoder.encode(task.title, CHARSET)
        ).joinToString(SEPARATOR.toString())
    }

    fun decode(text: String): List<Task> = text.lineSequence().mapNotNull { line ->
        val parts = line.split(SEPARATOR)
        if (parts.size != 4) return@mapNotNull null
        val id = parts[0].toLongOrNull() ?: return@mapNotNull null
        val done = when (parts[1]) {
            "true" -> true
            "false" -> false
            else -> return@mapNotNull null
        }
        val created = parts[2].toLongOrNull() ?: return@mapNotNull null
        val title = runCatching { URLDecoder.decode(parts[3], CHARSET) }.getOrNull()
            ?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
        Task(id, title, done, created)
    }.toList()
}
