package com.omaroid.tasky.feature.agenda.model

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime

sealed interface AgendaItemUi {
    val id: String
    val title: String
    val description: String
    val startTime: LocalTime

    data class Task(
        override val id: String,
        override val title: String,
        override val description: String,
        override val startTime: LocalTime,
        val displayDate: LocalDate,
        val isDone: Boolean,
    ) : AgendaItemUi

    data class Reminder(
        override val id: String,
        override val title: String,
        override val description: String,
        override val startTime: LocalTime,
        val displayDate: LocalDate,
    ) : AgendaItemUi

    data class Event(
        override val id: String,
        override val title: String,
        override val description: String,
        override val startTime: LocalTime,
        val from: LocalDateTime,
        val to: LocalDateTime,
        val isCreator: Boolean,
    ) : AgendaItemUi
}