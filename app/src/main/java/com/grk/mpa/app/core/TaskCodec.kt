package com.grk.mpa.app.core

import java.net.URLDecoder
import java.net.URLEncoder

/**
 * Stores tasks as tab-separated records with a URL-encoded title.
 *
 * The original four-column format remains readable. New records append the
 * server ID and sync state, preserving all existing device-side task IDs.
 * Malformed records are skipped rather than failing the whole list.
 */
object TaskCodec {
    private const val SEPARATOR = '\t'
    private const val CHARSET = "UTF-8"

    fun encode(tasks: List<Task>): String = tasks.joinToString("\n") { task ->
        listOf(
            task.id.toString(),
            task.isDone.toString(),
            task.createdAtMillis.toString(),
            URLEncoder.encode(task.title, CHARSET),
            task.remoteId?.toString().orEmpty(),
            task.syncState.name
        ).joinToString(SEPARATOR.toString())
    }

    fun decode(text: String): List<Task> = text.lineSequence().mapNotNull { line ->
        val parts = line.split(SEPARATOR)
        if (parts.size != 4 && parts.size != 6) return@mapNotNull null
        val id = parts[0].toLongOrNull() ?: return@mapNotNull null
        val done = when (parts[1]) {
            "true" -> true
            "false" -> false
            else -> return@mapNotNull null
        }
        val created = parts[2].toLongOrNull() ?: return@mapNotNull null
        val title = runCatching { URLDecoder.decode(parts[3], CHARSET) }.getOrNull()
            ?.takeIf { it.isNotBlank() } ?: return@mapNotNull null

        // Four-column records predate sync support and remain device-local.
        val remoteId = if (parts.size == 6) {
            if (parts[4].isEmpty()) null else parts[4].toLongOrNull()
                ?: return@mapNotNull null
        } else null
        val syncState = if (parts.size == 6) {
            runCatching { TaskSyncState.valueOf(parts[5]) }.getOrNull()
                ?: return@mapNotNull null
        } else TaskSyncState.LOCAL_ONLY

        if (remoteId == null && syncState != TaskSyncState.LOCAL_ONLY) return@mapNotNull null
        if (remoteId != null && remoteId <= 0L) return@mapNotNull null
        Task(id, title, done, created, remoteId, syncState)
    }.toList()
}
