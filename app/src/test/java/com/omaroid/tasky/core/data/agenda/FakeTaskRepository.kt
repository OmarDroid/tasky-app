package com.omaroid.tasky.core.data.agenda

import com.omaroid.tasky.core.domain.agenda.models.AgendaItem
import com.omaroid.tasky.core.domain.agenda.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class FakeTaskRepository : TaskRepository {
    private val _tasks = MutableStateFlow<List<AgendaItem.Task>>(emptyList())
    val tasks: List<AgendaItem.Task> get() = _tasks.value

    override fun observeTask(id: String) = _tasks.map { it.find { t -> t.id == id } }
    override fun observeTasksForDay(startOfDayMs: Long, endOfDayMs: Long) = _tasks.map { list ->
        list.filter { it.time.toEpochMilliseconds() in startOfDayMs until endOfDayMs }
    }

    override suspend fun createTask(task: AgendaItem.Task) {
        _tasks.update { it.filterNot { t -> t.id == task.id } + task }
    }

    override suspend fun updateTask(task: AgendaItem.Task) {
        _tasks.update { it.filterNot { t -> t.id == task.id } + task }
    }

    /**
     * Test-only helper — seeds the in-memory store directly without going through
     * create/update semantics. Use this to arrange state in tests.
     */
    fun upsertTask(task: AgendaItem.Task) {
        _tasks.update { it.filterNot { t -> t.id == task.id } + task }
    }

    override suspend fun deleteTask(id: String) {
        _tasks.update { it.filterNot { t -> t.id == id } }
    }

    override suspend fun clearAll() {
        _tasks.value = emptyList()
    }
}
