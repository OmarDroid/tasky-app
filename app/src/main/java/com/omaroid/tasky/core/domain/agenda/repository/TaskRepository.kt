package com.omaroid.tasky.core.domain.agenda.repository

import com.omaroid.tasky.core.domain.agenda.models.AgendaItem
import kotlinx.coroutines.flow.Flow

interface TaskRepository {
    fun observeTask(taskId: String): Flow<AgendaItem.Task?>
    fun observeTasksForDay(startOfDayMs: Long, endOfDayMs: Long): Flow<List<AgendaItem.Task>>
    /** Create a new task — writes to local DB and calls the create API endpoint. */
    suspend fun createTask(task: AgendaItem.Task)
    /** Update an existing task — writes to local DB and calls the update API endpoint. */
    suspend fun updateTask(task: AgendaItem.Task)
    suspend fun deleteTask(taskId: String)
    suspend fun clearAll()
}