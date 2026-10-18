package com.omaroid.tasky.core.data.agenda.remote

import com.omaroid.tasky.core.data.agenda.remote.mappers.toCreateDto
import com.omaroid.tasky.core.data.agenda.remote.mappers.toUpdateDto
import com.omaroid.tasky.core.domain.agenda.models.AgendaItem
import io.ktor.client.HttpClient
import io.ktor.client.request.delete
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

class KtorReminderRemoteDataSource(
    private val httpClient: HttpClient
): ReminderRemoteDataSource {
    override suspend fun createReminder(reminder: AgendaItem.Reminder) {
        httpClient.post("/reminder") {
            contentType(ContentType.Application.Json)
            setBody(reminder.toCreateDto())
        }
    }

    override suspend fun updateReminder(reminder: AgendaItem.Reminder) {
        httpClient.put("/reminder") {
            contentType(ContentType.Application.Json)
            setBody(reminder.toUpdateDto())
        }
    }

    override suspend fun deleteReminder(reminderId: String, deletedAt: String) {
        httpClient.delete("/reminder/$reminderId") {
            parameter("deletedAt", deletedAt)
        }
    }
}