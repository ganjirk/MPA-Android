package com.grk.mpa.app.data

import com.grk.mpa.app.core.Reminder

interface ReminderRepository {
    suspend fun load(): List<Reminder>
    suspend fun save(items: List<Reminder>)
}
