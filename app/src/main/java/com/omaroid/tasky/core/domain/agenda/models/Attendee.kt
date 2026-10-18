package com.omaroid.tasky.core.domain.agenda.models

import kotlin.time.Instant

data class Attendee(
    val email: String,
    val username: String,
    val userId: String,
    val eventId: String,
    val isGoing: Boolean,
    val remindAt: Instant,
)
