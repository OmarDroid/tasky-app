package com.omaroid.tasky.core.data.agenda.remote

import com.omaroid.tasky.core.data.agenda.remote.dto.DailyAgendaResponseDto
import com.omaroid.tasky.core.data.agenda.remote.dto.EventDto
import com.omaroid.tasky.core.data.agenda.remote.dto.ReminderDto
import com.omaroid.tasky.core.data.agenda.remote.dto.TaskDto

class FakeAgendaRemoteDataSource : AgendaRemoteDataSource {
    var error: Exception? = null
    var tasks: List<TaskDto> = emptyList()
    var reminders: List<ReminderDto> = emptyList()
    var events: List<EventDto> = emptyList()
    var syncCallCount: Int = 0

    override suspend fun getFullAgenda(): DailyAgendaResponseDto {
        error?.let { throw it }
        syncCallCount++
        return DailyAgendaResponseDto(events, tasks, reminders)
    }

    override suspend fun getDailyAgenda(epochMs: Long) =
        DailyAgendaResponseDto(emptyList(), emptyList(), emptyList())

    override suspend fun syncDeletedItems(
        deletedEventsId: List<String>,
        deletedTasksId: List<String>,
        deletedReminderIds: List<String>,
    ) {
    }
}
