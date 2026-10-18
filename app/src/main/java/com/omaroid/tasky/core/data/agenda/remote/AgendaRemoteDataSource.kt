package com.omaroid.tasky.core.data.agenda.remote

import com.omaroid.tasky.core.data.agenda.remote.dto.DailyAgendaResponseDto

interface AgendaRemoteDataSource {
    suspend fun getDailyAgenda(epochMs: Long): DailyAgendaResponseDto
    suspend fun getFullAgenda(): DailyAgendaResponseDto
    suspend fun syncDeletedItems(
        deletedEventsId: List<String>,
        deletedTasksId: List<String>,
        deletedReminderIds: List<String>
    )
}