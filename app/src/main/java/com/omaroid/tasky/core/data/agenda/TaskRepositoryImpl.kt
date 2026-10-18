package com.omaroid.tasky.core.data.agenda

import com.omaroid.tasky.core.data.agenda.local.dao.TaskDao
import com.omaroid.tasky.core.data.agenda.remote.TaskRemoteDataSource
import com.omaroid.tasky.core.data.agenda.remote.mappers.toDomain
import com.omaroid.tasky.core.data.agenda.remote.mappers.toEntity
import com.omaroid.tasky.core.data.agenda.remote.mappers.toIso8601
import com.omaroid.tasky.core.data.sync.Outbox
import com.omaroid.tasky.core.data.sync.SyncIntent
import com.omaroid.tasky.core.data.sync.SyncItemType
import com.omaroid.tasky.core.domain.agenda.models.AgendaItem
import com.omaroid.tasky.core.domain.agenda.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Clock

class TaskRepositoryImpl(
    private val taskDao: TaskDao,
    private val remoteDataSource: TaskRemoteDataSource,
    private val outbox: Outbox,
    private val clock: Clock = Clock.System,
) : TaskRepository {
    override fun observeTask(taskId: String) =
        taskDao.observeTask(taskId).map { it?.toDomain() }

    override fun observeTasksForDay(
        startOfDayMs: Long,
        endOfDayMs: Long,
    ) =
        taskDao.getTasksForDay(startOfDayMs, endOfDayMs).map { list -> list.map { it.toDomain() } }

    override suspend fun createTask(task: AgendaItem.Task) {
        taskDao.upsert(task.toEntity(updatedAt = clock.now().toEpochMilliseconds()))
        outbox.sync(SyncItemType.TASK, task.id, SyncIntent.CREATE) { remoteDataSource.createTask(task) }
    }

    override suspend fun updateTask(task: AgendaItem.Task) {
        taskDao.upsert(task.toEntity(updatedAt = clock.now().toEpochMilliseconds()))
        outbox.sync(SyncItemType.TASK, task.id, SyncIntent.UPDATE) { remoteDataSource.updateTask(task) }
    }

    override suspend fun deleteTask(taskId: String) {
        taskDao.deleteById(taskId)
        outbox.sync(SyncItemType.TASK, taskId, SyncIntent.DELETE) {
            remoteDataSource.deleteTask(taskId, clock.now().toEpochMilliseconds().toIso8601())
        }
    }

    override suspend fun clearAll() = taskDao.clearAll()
}