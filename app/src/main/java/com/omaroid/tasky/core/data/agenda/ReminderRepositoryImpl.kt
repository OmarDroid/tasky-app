package com.omaroid.tasky.core.data.agenda

import com.omaroid.tasky.core.data.agenda.local.dao.ReminderDao
import com.omaroid.tasky.core.data.agenda.remote.ReminderRemoteDataSource
import com.omaroid.tasky.core.data.agenda.remote.mappers.toDomain
import com.omaroid.tasky.core.data.agenda.remote.mappers.toEntity
import com.omaroid.tasky.core.data.agenda.remote.mappers.toIso8601
import com.omaroid.tasky.core.data.sync.Outbox
import com.omaroid.tasky.core.data.sync.SyncIntent
import com.omaroid.tasky.core.data.sync.SyncItemType
import com.omaroid.tasky.core.domain.agenda.models.AgendaItem
import com.omaroid.tasky.core.domain.agenda.repository.ReminderRepository
import kotlinx.coroutines.flow.map
import kotlin.time.Clock

class ReminderRepositoryImpl(
    private val reminderDao: ReminderDao,
    private val remoteDataSource: ReminderRemoteDataSource,
    private val outbox: Outbox,
    private val clock: Clock = Clock.System,
) : ReminderRepository {

    override fun observeReminder(reminderId: String) =
        reminderDao.observeReminder(reminderId).map { it?.toDomain() }

    override fun observeRemindersForDay(startMs: Long, endMs: Long) =
        reminderDao.getRemindersForDay(startMs, endMs).map { list -> list.map { it.toDomain() } }

    override suspend fun createReminder(reminder: AgendaItem.Reminder) {
        reminderDao.upsert(reminder.toEntity(updatedAt = clock.now().toEpochMilliseconds()))
        outbox.sync(SyncItemType.REMINDER, reminder.id, SyncIntent.CREATE) {
            remoteDataSource.createReminder(reminder)
        }
    }

    override suspend fun updateReminder(reminder: AgendaItem.Reminder) {
        reminderDao.upsert(reminder.toEntity(updatedAt = clock.now().toEpochMilliseconds()))
        outbox.sync(SyncItemType.REMINDER, reminder.id, SyncIntent.UPDATE) {
            remoteDataSource.updateReminder(reminder)
        }
    }

    override suspend fun deleteReminder(reminderId: String) {
        reminderDao.deleteById(reminderId)
        outbox.sync(SyncItemType.REMINDER, reminderId, SyncIntent.DELETE) {
            remoteDataSource.deleteReminder(reminderId, clock.now().toEpochMilliseconds().toIso8601())
        }
    }

    override suspend fun clearAll() = reminderDao.clearAll()
}