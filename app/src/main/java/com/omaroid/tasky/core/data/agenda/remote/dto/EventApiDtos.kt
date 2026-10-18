package com.omaroid.tasky.core.data.agenda.remote.dto

import kotlinx.serialization.Serializable

/**
 * Request/response DTOs for the event endpoints, matching the Tasky V2 API docs.
 *
 * Create/update are `application/json`. The photo flow is two-phase:
 *   1. POST /event (or PUT /event/{eventId}) sends client-generated `photoKeys`/`newPhotoKeys`
 *      and gets back the event plus `uploadUrls` (pre-signed S3 PUT urls).
 *   2. The client PUTs each JPEG to its url, then POST /event/{eventId}/confirm-upload with the
 *      server-generated `uploadKey`s; the server then attaches downloadable photo urls.
 */
@Serializable
data class CreateEventRequestDto(
    val id: String,
    val title: String,
    val description: String,
    val from: String,
    val to: String,
    val remindAt: String,
    val attendeeIds: List<String>,
    val photoKeys: List<String>, // client-generated keys, e.g. "photo0", "photo1"
    val updatedAt: String,
)

// PUT /event/{eventId} — the event id is the path param, so it is NOT in the body.
@Serializable
data class UpdateEventRequestDto(
    val title: String,
    val description: String,
    val from: String,
    val to: String,
    val remindAt: String,
    val attendeeIds: List<String>,
    val newPhotoKeys: List<String>,
    val deletedPhotoKeys: List<String>,
    val isGoing: Boolean,
    val updatedAt: String,
)

@Serializable
data class UploadUrlDto(
    val photoKey: String,
    val uploadKey: String,
    val url: String,
)

@Serializable
data class EventResponseDto(
    val event: EventDto,
    val uploadUrls: List<UploadUrlDto> = emptyList(),
)

// GET /attendee?email= → a flat object (200) or 404 when the user does not exist. Uses "fullName".
@Serializable
data class CheckAttendeeResponseDto(
    val email: String,
    val fullName: String,
    val userId: String,
)
