package com.omaroid.tasky.core.data.agenda

import com.omaroid.tasky.core.domain.agenda.models.AgendaItem
import com.omaroid.tasky.core.domain.agenda.repository.ReminderRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class FakeReminderRepository : ReminderRepository {
    private val _reminders = MutableStateFlow<List<AgendaItem.Reminder>>(emptyList())
    val reminders: List<AgendaItem.Reminder> get() = _reminders.value

    override fun observeReminder(id: String) = _reminders.map { it.find { r -> r.id == id } }
    override fun observeRemindersForDay(startOfDayMs: Long, endOfDayMs: Long) =
        _reminders.map { list ->
            list.filter { it.time.toEpochMilliseconds() in startOfDayMs until endOfDayMs }
        }

    override suspend fun createReminder(reminder: AgendaItem.Reminder) {
        _reminders.update { it.filterNot { x -> x.id == reminder.id } + reminder }
    }

    override suspend fun updateReminder(reminder: AgendaItem.Reminder) {
        _reminders.update { it.filterNot { x -> x.id == reminder.id } + reminder }
    }

    /**
     * Test-only helper — seeds the in-memory store directly without going through
     * create/update semantics. Use this to arrange state in tests.
     */
    fun upsertReminder(reminder: AgendaItem.Reminder) {
        _reminders.update { it.filterNot { x -> x.id == reminder.id } + reminder }
    }

    override suspend fun deleteReminder(id: String) {
        _reminders.update { it.filterNot { r -> r.id == id } }
    }

    override suspend fun clearAll() {
        _reminders.value = emptyList()
    }
}
