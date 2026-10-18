package com.omaroid.tasky.feature.agenda.model

import com.omaroid.tasky.core.domain.agenda.models.AgendaItem
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

fun AgendaItem.toUi(currentUserId: String?): AgendaItemUi {
    val zone = TimeZone.currentSystemDefault()
    return when (this) {
        is AgendaItem.Task -> {
            val ldt = time.toLocalDateTime(zone)
            AgendaItemUi.Task(
                id = id, title = title, description = description,
                startTime = ldt.time, displayDate = ldt.date, isDone = isDone,
            )
        }
        is AgendaItem.Reminder -> {
            val ldt = time.toLocalDateTime(zone)
            AgendaItemUi.Reminder(
                id = id, title = title, description = description,
                startTime = ldt.time, displayDate = ldt.date,
            )
        }
        is AgendaItem.Event -> {
            val fromLdt = time.toLocalDateTime(zone)
            val toLdt = to.toLocalDateTime(zone)
            AgendaItemUi.Event(
                id = id, title = title, description = description,
                startTime = fromLdt.time, from = fromLdt, to = toLdt,
                isCreator = hostId == currentUserId,
            )
        }
    }
}
