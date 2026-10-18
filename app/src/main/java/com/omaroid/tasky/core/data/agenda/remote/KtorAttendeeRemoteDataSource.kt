package com.omaroid.tasky.core.data.agenda.remote

import com.omaroid.tasky.core.data.agenda.remote.dto.CheckAttendeeResponseDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess

class KtorAttendeeRemoteDataSource(
    private val httpClient: HttpClient,
) : AttendeeRemoteDataSource {

    // The API returns a flat { email, fullName, userId } on 200, or 404 when the user doesn't exist.
    override suspend fun getAttendeeByEmail(email: String): CheckAttendeeResponseDto? {
        val response = httpClient.get("/attendee") { parameter("email", email) }
        if (!response.status.isSuccess() || response.bodyAsText().isBlank()) return null
        return response.body()
    }

    override suspend fun deleteAttendee(eventId: String) {
        httpClient.delete("/attendee") { parameter("eventId", eventId) }
    }
}
