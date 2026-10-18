package com.omaroid.tasky.core.util

import com.omaroid.tasky.core.data.agenda.local.entities.EventEntity
import com.omaroid.tasky.core.data.agenda.local.entities.TaskEntity
import com.omaroid.tasky.core.data.agenda.remote.dto.AttendeeDto
import com.omaroid.tasky.core.data.agenda.remote.dto.PhotoDto

/** Shared entity builders for DAO / sync tests — keeps each test focused on behaviour, not wiring. */
fun taskEntity(
    id: String = "task1",
    title: String = "Task",
    description: String = "Desc",
    time: Long = 1_000L,
    remindAt: Long = 900L,
    isDone: Boolean = false,
    updatedAt: Long = 1_000L,
) = TaskEntity(
    id = id, title = title, description = description,
    time = time, remindAt = remindAt, isDone = isDone, updatedAt = updatedAt,
)

fun eventEntity(
    id: String = "event1",
    title: String = "Event",
    description: String = "Desc",
    fromTime: Long = 1_000L,
    toTime: Long = 2_000L,
    remindAt: Long = 900L,
    hostId: String = "host1",
    attendees: List<AttendeeDto> = emptyList(),
    photos: List<PhotoDto> = emptyList(),
    updatedAt: Long = 1_000L,
) = EventEntity(
    id = id, title = title, description = description,
    fromTime = fromTime, toTime = toTime, remindAt = remindAt,
    hostId = hostId, attendees = attendees, photos = photos, updatedAt = updatedAt,
)
