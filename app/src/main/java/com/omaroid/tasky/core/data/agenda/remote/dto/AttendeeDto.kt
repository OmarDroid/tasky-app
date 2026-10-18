package com.omaroid.tasky.core.data.agenda.remote.dto

import kotlinx.serialization.Serializable

// Attendee as returned inside an event (Tasky V2): `username` (full name) and an ISO-8601 remindAt.
// All fields defaulted so a missing one in an event/agenda response can't fail the whole parse.
@Serializable
data class AttendeeDto(
    val email: String = "",
    val username: String = "",
    val userId: String = "",
    val eventId: String = "",
    val isGoing: Boolean = true,
    val remindAt: String = "",
)
