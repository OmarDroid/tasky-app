package com.omaroid.tasky.core.data.agenda.remote

import com.omaroid.tasky.core.domain.agenda.models.AgendaItem

interface ReminderRemoteDataSource {
    suspend fun createReminder(reminder: AgendaItem.Reminder)
    suspend fun updateReminder(reminder: AgendaItem.Reminder)
    suspend fun deleteReminder(reminderId: String, deletedAt: String)
}