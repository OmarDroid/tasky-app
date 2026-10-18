package com.omaroid.tasky.core.data.agenda.remote

import com.omaroid.tasky.core.data.agenda.remote.dto.CreateEventRequestDto
import com.omaroid.tasky.core.data.agenda.remote.dto.EventDto
import com.omaroid.tasky.core.data.agenda.remote.dto.EventResponseDto
import com.omaroid.tasky.core.data.agenda.remote.dto.UpdateEventRequestDto

interface EventRemoteDataSource {
    // POST /event (JSON) → event + pre-signed uploadUrls for each client photoKey.
    suspend fun createEvent(request: CreateEventRequestDto): EventResponseDto
    // PUT /event/{eventId} (JSON) → event + uploadUrls for each newPhotoKey.
    suspend fun updateEvent(eventId: String, request: UpdateEventRequestDto): EventResponseDto
    // Phase 2: PUT raw JPEG bytes to a pre-signed S3 url (no auth — baked into the url).
    suspend fun uploadPhoto(url: String, bytes: ByteArray)
    // POST /event/{eventId}/confirm-upload → the event WITH accessible photo urls (event object).
    suspend fun confirmUpload(eventId: String, uploadedKeys: List<String>): EventDto
    // DELETE /event?eventId=
    suspend fun deleteEvent(eventId: String)
}
