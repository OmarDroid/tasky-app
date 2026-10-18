package com.omaroid.tasky.core.data.agenda.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The event as the Tasky API returns it (GET /agenda, GET /fullAgenda, GET /event, and the `event`
 * field of the create/update/confirm responses). Per the Tasky V2 API docs all timestamps are
 * ISO-8601 strings; the event's photos are returned under the JSON key `photoKeys`.
 */
@Serializable
data class EventDto(
    val id: String,
    val title: String,
    // Non-critical fields are defaulted: the docs are inconsistent about which the event/agenda
    // responses include (e.g. some omit remindAt), and a single missing required field would throw a
    // MissingFieldException that silently fails the whole /agenda or /fullAgenda parse.
    val description: String = "",
    val from: String,
    val to: String,
    val remindAt: String = "",
    val hostId: String = "",
    val isUserEventCreator: Boolean = false,
    val attendees: List<AttendeeDto> = emptyList(),
    @SerialName("photoKeys") val photos: List<PhotoDto> = emptyList(),
)
