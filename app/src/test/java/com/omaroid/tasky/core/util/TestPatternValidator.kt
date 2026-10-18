package com.omaroid.tasky.core.util

import com.omaroid.tasky.core.domain.agenda.models.AgendaItem
import com.omaroid.tasky.core.domain.util.PatternValidator
import kotlin.collections.emptyList
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

class TestPatternValidator(private val result: Boolean = true) : PatternValidator {
    override fun matches(value: String): Boolean = result
}

fun task(id: String, time: Instant, isDone: Boolean = false) = AgendaItem.Task(
    id = id, title = "Task $id", description = "",
    time = time, remindAt = time - 30.minutes, isDone = isDone,
)

fun reminder(id: String, time: Instant) = AgendaItem.Reminder(
    id = id, title = "Reminder $id", description = "",
    time = time, remindAt = time - 30.minutes,
)

fun event(id: String, time: Instant, hostId: String) = AgendaItem.Event(
    id = id, title = "Event $id", description = "",
    time = time, to = time + 1.hours,
    remindAt = time - 30.minutes, hostId = hostId,
    attendees = emptyList(), photos = emptyList(),
)