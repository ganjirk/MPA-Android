package com.grk.mpa.app.core

object ReminderOperations {
    const val MAX_TITLE_LENGTH = 100

    fun normalizeTitle(raw: String): String? =
        raw.trim().replace(Regex("\\s+"), " ").take(MAX_TITLE_LENGTH).trim().takeIf { it.isNotEmpty() }

    fun add(items: List<Reminder>, title: String, at: Long): List<Reminder> {
        val normalized = normalizeTitle(title) ?: return items
        val id = (items.maxOfOrNull { it.id } ?: 0L) + 1L
        return (items + Reminder(id, normalized, at)).sortedBy { it.triggerAtMillis }
    }

    fun update(items: List<Reminder>, id: Long, title: String, at: Long): List<Reminder> {
        val normalized = normalizeTitle(title) ?: return items
        return items.map { if (it.id == id) it.copy(title = normalized, triggerAtMillis = at) else it }
            .sortedBy { it.triggerAtMillis }
    }

    fun complete(items: List<Reminder>, id: Long) =
        items.map { if (it.id == id) it.copy(isCompleted = true) else it }

    fun delete(items: List<Reminder>, id: Long) = items.filterNot { it.id == id }
    fun upcoming(items: List<Reminder>) = items.filterNot { it.isCompleted }.sortedBy { it.triggerAtMillis }
    fun completed(items: List<Reminder>) = items.filter { it.isCompleted }.sortedByDescending { it.triggerAtMillis }
}
