package com.omaroid.tasky.core.domain.agenda.models

import kotlin.time.Instant

sealed interface AgendaItem {
    val id: String
    val title: String
    val description: String
    val time: Instant
    val remindAt: Instant

    data class Task(
        override val id: String,
        override val title: String,
        override val description: String,
        override val time: Instant,
        override val remindAt: Instant,
        val isDone: Boolean,
    ) : AgendaItem

    data class Reminder(
        override val id: String,
        override val title: String,
        override val description: String,
        override val time: Instant,
        override val remindAt: Instant,
    ) : AgendaItem

    data class Event(
        override val id: String,
        override val title: String,
        override val description: String,
        override val time: Instant,
        val to: Instant,
        override val remindAt: Instant,
        val hostId: String,
        val attendees: List<Attendee>,
        val photos: List<Photo>,
    ) : AgendaItem
}