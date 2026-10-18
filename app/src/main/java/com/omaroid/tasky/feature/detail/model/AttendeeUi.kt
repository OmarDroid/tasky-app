package com.omaroid.tasky.feature.detail.model

import com.omaroid.tasky.core.domain.agenda.models.Attendee
import com.omaroid.tasky.core.presentation.util.InitialsFormatter
import kotlin.time.Instant

data class AttendeeUi(
    val userId: String,
    val fullName: String,
    val initials: String, // pre-computed in toUi() — never compute initials in a composable
    val email: String,
    val isGoing: Boolean,
    val isCreator: Boolean,
)

fun Attendee.toUi(hostId: String): AttendeeUi {
    val isCreator = userId == hostId
    return AttendeeUi(
        userId = userId,
        fullName = username,
        initials = InitialsFormatter.format(username),
        email = email,
        // Event creators are always considered going (they can't mark themselves not going).
        isGoing = isCreator || isGoing,
        isCreator = isCreator,
    )
}

// Reconstruct a domain Attendee for persistence. The UI model does not carry eventId/remindAt,
// so the caller (the event being saved) supplies them.
fun AttendeeUi.toDomain(eventId: String, remindAt: Instant) = Attendee(
    email = email,
    username = fullName,
    userId = userId,
    eventId = eventId,
    isGoing = isGoing,
    remindAt = remindAt,
)
