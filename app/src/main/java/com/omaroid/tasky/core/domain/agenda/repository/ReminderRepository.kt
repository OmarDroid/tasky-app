package com.omaroid.tasky.core.domain.agenda.repository

import com.omaroid.tasky.core.domain.agenda.models.AgendaItem
import kotlinx.coroutines.flow.Flow

interface ReminderRepository {
    fun observeReminder(reminderId: String): Flow<AgendaItem.Reminder?>
    fun observeRemindersForDay(startOfDayMs: Long, endOfDayMs: Long): Flow<List<AgendaItem.Reminder>>
    /** Create a new reminder — writes to local DB and calls the create API endpoint. */
    suspend fun createReminder(reminder: AgendaItem.Reminder)
    /** Update an existing reminder — writes to local DB and calls the update API endpoint. */
    suspend fun updateReminder(reminder: AgendaItem.Reminder)
    suspend fun deleteReminder(reminderId: String)
    suspend fun clearAll()
}