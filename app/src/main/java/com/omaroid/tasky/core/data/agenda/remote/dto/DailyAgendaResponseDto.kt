package com.omaroid.tasky.core.data.agenda.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class DailyAgendaResponseDto(
    val events: List<EventDto>,
    val tasks: List<TaskDto>,
    val reminders: List<ReminderDto>,
)
