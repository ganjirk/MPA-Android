package com.grk.mpa.app.ui.home

/** The home search: every word typed must appear somewhere in the title or description, ignoring case. */
object HomeSearch {
    fun matches(query: String, vararg fields: String): Boolean {
        val words = query.trim().lowercase().split(Regex("\\s+")).filter { it.isNotEmpty() }
        if (words.isEmpty()) return true
        val haystack = fields.joinToString(" ").lowercase()
        return words.all { it in haystack }
    }
}
