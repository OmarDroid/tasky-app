package com.omaroid.tasky.core.data.agenda.remote

import com.omaroid.tasky.core.data.agenda.remote.dto.CreateEventRequestDto
import com.omaroid.tasky.core.data.agenda.remote.dto.EventDto
import com.omaroid.tasky.core.data.agenda.remote.dto.EventResponseDto
import com.omaroid.tasky.core.data.agenda.remote.dto.UpdateEventRequestDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.Serializable

/**
 * Tasky V2 event API. Create/update are JSON and return pre-signed S3 upload urls; the client PUTs
 * each JPEG to S3 on a bare (no-bearer) client, then confirms the upload keys so the server attaches
 * accessible photo urls.
 */
class KtorEventRemoteDataSource(
    private val httpClient: HttpClient,
    // A bare client (no bearer auth) — pre-signed S3 urls reject a conflicting Authorization header.
    private val uploadClient: HttpClient,
) : EventRemoteDataSource {

    override suspend fun createEvent(request: CreateEventRequestDto): EventResponseDto =
        httpClient.post("/event") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    override suspend fun updateEvent(eventId: String, request: UpdateEventRequestDto): EventResponseDto =
        httpClient.put("/event/$eventId") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    override suspend fun uploadPhoto(url: String, bytes: ByteArray) {
        uploadClient.put(url) {
            contentType(ContentType.Image.JPEG)
            setBody(bytes)
        }
    }

    override suspend fun confirmUpload(eventId: String, uploadedKeys: List<String>): EventDto =
        httpClient.post("/event/$eventId/confirm-upload") {
            contentType(ContentType.Application.Json)
            setBody(ConfirmUploadRequestDto(uploadedKeys))
        }.body()

    override suspend fun deleteEvent(eventId: String) {
        httpClient.delete("/event") { parameter("eventId", eventId) }
    }
}

@Serializable
private data class ConfirmUploadRequestDto(val uploadedKeys: List<String>)
