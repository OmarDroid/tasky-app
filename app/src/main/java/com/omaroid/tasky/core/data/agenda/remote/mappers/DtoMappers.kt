package com.omaroid.tasky.core.data.agenda.remote.mappers

import com.omaroid.tasky.core.data.agenda.local.entities.EventEntity
import com.omaroid.tasky.core.data.agenda.local.entities.ReminderEntity
import com.omaroid.tasky.core.data.agenda.local.entities.TaskEntity
import com.omaroid.tasky.core.data.agenda.remote.dto.AttendeeDto
import com.omaroid.tasky.core.data.agenda.remote.dto.CheckAttendeeResponseDto
import com.omaroid.tasky.core.data.agenda.remote.dto.CreateEventRequestDto
import com.omaroid.tasky.core.data.agenda.remote.dto.EventDto
import com.omaroid.tasky.core.data.agenda.remote.dto.PhotoDto
import com.omaroid.tasky.core.data.agenda.remote.dto.ReminderDto
import com.omaroid.tasky.core.data.agenda.remote.dto.TaskDto
import com.omaroid.tasky.core.data.agenda.remote.dto.UpdateEventRequestDto
import com.omaroid.tasky.core.domain.agenda.models.AgendaItem
import com.omaroid.tasky.core.domain.agenda.models.Attendee
import com.omaroid.tasky.core.domain.agenda.models.Photo
import kotlin.time.Instant

// ── AttendeeDto ↔ Attendee ─────────────────────────────────────────────────

fun AttendeeDto.toDomain() = Attendee(
    email = email, username = username, userId = userId,
    eventId = eventId, isGoing = isGoing,
    remindAt = if (remindAt.isBlank()) Instant.fromEpochMilliseconds(0) else Instant.parse(remindAt),
)

fun Attendee.toDto() = AttendeeDto(
    email = email, username = username, userId = userId,
    eventId = eventId, isGoing = isGoing,
    remindAt = remindAt.toString(),
)

// ── PhotoDto ↔ Photo ───────────────────────────────────────────────────────

fun PhotoDto.toDomain() = Photo(key = key, url = url)
fun Photo.toDto() = PhotoDto(key = key, url = url)

// ── CheckAttendeeResponseDto → Attendee (GET /attendee lookup) ─────────────
// Only the identity fields are known at lookup time; eventId/remindAt are filled by the caller when
// the attendee is actually attached to an event.

fun CheckAttendeeResponseDto.toDomain() = Attendee(
    email = email, username = fullName, userId = userId,
    eventId = "", isGoing = true,
    remindAt = Instant.fromEpochMilliseconds(0),
)

// ── TaskDto → TaskEntity ───────────────────────────────────────────────────

fun TaskDto.toEntity(updatedAt: Long) = TaskEntity(
    id = id, title = title, description = description,
    time = time.toEpochMs(), remindAt = remindAt.toEpochMs(),
    isDone = isDone, updatedAt = updatedAt,
)

// ── TaskEntity ↔ AgendaItem.Task ───────────────────────────────────────────

fun TaskEntity.toDomain() = AgendaItem.Task(
    id = id, title = title, description = description,
    time = Instant.fromEpochMilliseconds(time),
    remindAt = Instant.fromEpochMilliseconds(remindAt),
    isDone = isDone,
)

fun AgendaItem.Task.toEntity(updatedAt: Long) = TaskEntity(
    id = id, title = title, description = description,
    time = time.toEpochMilliseconds(), remindAt = remindAt.toEpochMilliseconds(),
    isDone = isDone, updatedAt = updatedAt,
)

fun TaskDto.toDomain() = AgendaItem.Task(
    id = id, title = title, description = description,
    time = Instant.parse(time), remindAt = Instant.parse(remindAt),
    isDone = isDone,
)

fun AgendaItem.Task.toCreateDto() = TaskDto(
    id = id, title = title, description = description,
    time = time.toString(), remindAt = remindAt.toString(), isDone = isDone,
)

fun AgendaItem.Task.toUpdateDto() = toCreateDto()

// ── ReminderDto → ReminderEntity ──────────────────────────────────────────

fun ReminderDto.toEntity(updatedAt: Long) = ReminderEntity(
    id = id, title = title, description = description,
    time = time.toEpochMs(), remindAt = remindAt.toEpochMs(),
    updatedAt = updatedAt,
)

fun ReminderEntity.toDomain() = AgendaItem.Reminder(
    id = id, title = title, description = description,
    time = Instant.fromEpochMilliseconds(time),
    remindAt = Instant.fromEpochMilliseconds(remindAt),
)

fun AgendaItem.Reminder.toEntity(updatedAt: Long) = ReminderEntity(
    id = id, title = title, description = description,
    time = time.toEpochMilliseconds(), remindAt = remindAt.toEpochMilliseconds(),
    updatedAt = updatedAt,
)

fun ReminderDto.toDomain() = AgendaItem.Reminder(
    id = id, title = title, description = description,
    time = Instant.parse(time), remindAt = Instant.parse(remindAt),
)

fun AgendaItem.Reminder.toCreateDto() = ReminderDto(
    id = id, title = title, description = description,
    time = time.toString(), remindAt = remindAt.toString(),
)

fun AgendaItem.Reminder.toUpdateDto() = toCreateDto()

// ── EventDto → EventEntity (ISO strings → epoch ms in storage) ─────────────

fun EventDto.toEntity(updatedAt: Long) = EventEntity(
    id = id, title = title, description = description,
    fromTime = from.toEpochMs(), toTime = to.toEpochMs(),
    // Some responses omit remindAt; fall back to `from` so the row is still valid.
    remindAt = remindAt.ifBlank { from }.toEpochMs(),
    hostId = hostId,
    attendees = attendees,
    photos = photos,
    updatedAt = updatedAt,
)

// ── EventEntity ↔ AgendaItem.Event ────────────────────────────────────────

fun EventEntity.toDomain() = AgendaItem.Event(
    id = id, title = title, description = description,
    time = Instant.fromEpochMilliseconds(fromTime),
    to = Instant.fromEpochMilliseconds(toTime),
    remindAt = Instant.fromEpochMilliseconds(remindAt),
    hostId = hostId,
    attendees = attendees.map { it.toDomain() },
    photos = photos.map { it.toDomain() },
)

fun AgendaItem.Event.toEntity(updatedAt: Long) = EventEntity(
    id = id, title = title, description = description,
    fromTime = time.toEpochMilliseconds(), toTime = to.toEpochMilliseconds(),
    remindAt = remindAt.toEpochMilliseconds(),
    hostId = hostId,
    attendees = attendees.map { it.toDto() },
    photos = photos.map { it.toDto() },
    updatedAt = updatedAt,
)

// ── AgendaItem.Event → API request DTOs (ISO strings) ──────────────────────

fun AgendaItem.Event.toCreateDto(
    photoKeys: List<String>,
    updatedAt: Long,
) = CreateEventRequestDto(
    id = id, title = title, description = description,
    from = time.toString(), to = to.toString(), remindAt = remindAt.toString(),
    attendeeIds = attendees.map { it.userId },
    photoKeys = photoKeys,
    updatedAt = updatedAt.toIso8601(),
)

fun AgendaItem.Event.toUpdateDto(
    updatedAt: Long,
    isGoing: Boolean,
    newPhotoKeys: List<String>,
    deletedPhotoKeys: List<String>,
) = UpdateEventRequestDto(
    title = title, description = description,
    from = time.toString(), to = to.toString(), remindAt = remindAt.toString(),
    attendeeIds = attendees.map { it.userId },
    newPhotoKeys = newPhotoKeys,
    deletedPhotoKeys = deletedPhotoKeys,
    isGoing = isGoing,
    updatedAt = updatedAt.toIso8601(),
)
