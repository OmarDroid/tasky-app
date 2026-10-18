package com.omaroid.tasky.core.data.agenda.remote

import com.omaroid.tasky.core.domain.agenda.models.AgendaItem

class FakeReminderRemoteDataSource : ReminderRemoteDataSource {
    val created = mutableListOf<AgendaItem.Reminder>()
    val updated = mutableListOf<AgendaItem.Reminder>()
    val deleted = mutableListOf<String>()
    var shouldThrow = false

    override suspend fun createReminder(reminder: AgendaItem.Reminder) {
        if (shouldThrow) throw RuntimeException("network error")
        created.add(reminder)
    }
    override suspend fun updateReminder(reminder: AgendaItem.Reminder) {
        if (shouldThrow) throw RuntimeException("network error")
        updated.add(reminder)
    }
    override suspend fun deleteReminder(reminderId: String, deletedAt: String) {
        if (shouldThrow) throw RuntimeException("network error")
        deleted.add(reminderId)
    }
}
