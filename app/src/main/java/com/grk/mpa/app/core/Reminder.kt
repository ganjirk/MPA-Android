package com.grk.mpa.app.core

data class Reminder(
    val id: Long,
    val title: String,
    val triggerAtMillis: Long,
    val isCompleted: Boolean = false
)

enum class ReminderFilter { UPCOMING, COMPLETED }
