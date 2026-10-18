package com.omaroid.tasky.core.data.agenda.remote

import com.omaroid.tasky.core.domain.agenda.models.AgendaItem

interface TaskRemoteDataSource {
    suspend fun createTask(task: AgendaItem.Task)
    suspend fun updateTask(task: AgendaItem.Task)
    suspend fun deleteTask(taskId: String, deletedAt: String)
}