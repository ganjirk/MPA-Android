package com.grk.mpa.app.core

data class Task(
    val id: Long,
    val title: String,
    val isDone: Boolean = false,
    val createdAtMillis: Long = 0L
)

enum class TaskFilter { ALL, ACTIVE, DONE }
