package com.omaroid.tasky.core.data.agenda.remote

import com.omaroid.tasky.core.domain.agenda.models.AgendaItem

class FakeTaskRemoteDataSource : TaskRemoteDataSource {
    val created = mutableListOf<AgendaItem.Task>()
    val updated = mutableListOf<AgendaItem.Task>()
    val deleted = mutableListOf<String>()
    var shouldThrow = false

    override suspend fun createTask(task: AgendaItem.Task) {
        if (shouldThrow) throw RuntimeException("network error")
        created.add(task)
    }
    override suspend fun updateTask(task: AgendaItem.Task) {
        if (shouldThrow) throw RuntimeException("network error")
        updated.add(task)
    }
    override suspend fun deleteTask(taskId: String, deletedAt: String) {
        if (shouldThrow) throw RuntimeException("network error")
        deleted.add(taskId)
    }
}
