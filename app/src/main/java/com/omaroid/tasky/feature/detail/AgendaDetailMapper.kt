package com.omaroid.tasky.feature.detail

import com.omaroid.tasky.core.domain.agenda.models.AgendaItem
import com.omaroid.tasky.core.domain.agenda.models.ReminderType
import com.omaroid.tasky.feature.detail.model.toDomain
import com.omaroid.tasky.feature.detail.model.toUi
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

fun AgendaItem.toDetailState(): AgendaDetailState {
    val zone = TimeZone.currentSystemDefault()
    return AgendaDetailState(
        id = id,
        title = title,
        description = description,
        dateTime = time.toLocalDateTime(zone),
        reminderType = ReminderType.fromOffset(time - remindAt),
        details = when (this) {
            is AgendaItem.Task -> AgendaItemDetails.Task(isDone = isDone)
            is AgendaItem.Reminder -> AgendaItemDetails.Reminder
            is AgendaItem.Event -> {
                val attendeesUi = attendees.map { it.toUi(hostId) }
                AgendaItemDetails.Event(
                    toTime = to.toLocalDateTime(zone),
                    attendees = attendeesUi,
                    goingAttendees = attendeesUi.filter { it.isGoing },
                    notGoingAttendees = attendeesUi.filter { !it.isGoing },
                    photos = photos.map { it.toUi() },
                    hostId = hostId,
                )
            }
        },
        isNew = false,
        isLoading = false,
    )
}

fun AgendaDetailState.toAgendaItem(): AgendaItem {
    val zone = TimeZone.currentSystemDefault()
    val instant = dateTime.toInstant(zone)
    val remindAt = instant - reminderType.offset
    return when (val d = details) {
        is AgendaItemDetails.Task -> AgendaItem.Task(
            id = id, title = title, description = description,
            time = instant, remindAt = remindAt, isDone = d.isDone,
        )
        is AgendaItemDetails.Reminder -> AgendaItem.Reminder(
            id = id, title = title, description = description,
            time = instant, remindAt = remindAt,
        )
        is AgendaItemDetails.Event -> AgendaItem.Event(
            id = id, title = title, description = description,
            time = instant, remindAt = remindAt,
            to = d.toTime.toInstant(zone),
            hostId = d.hostId,
            attendees = d.attendees.map { it.toDomain(eventId = id, remindAt = remindAt) },
            // Persist remote photos AND locally-picked ones (keyed by their content uri) so a newly
            // added photo survives save → reopen (view mode reads from Room). When the server confirms
            // the upload, the repository re-upserts the event with the real download urls.
            photos = d.photos.map { it.toDomain() } +
                d.localPhotoUris.map { com.omaroid.tasky.core.domain.agenda.models.Photo(key = it, url = it) },
        )
    }
}
