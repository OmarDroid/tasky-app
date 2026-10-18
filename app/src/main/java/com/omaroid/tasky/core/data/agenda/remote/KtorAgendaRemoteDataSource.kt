package com.omaroid.tasky.core.data.agenda.remote

import com.omaroid.tasky.core.data.agenda.remote.dto.DailyAgendaResponseDto
import com.omaroid.tasky.core.data.agenda.remote.mappers.toIso8601
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

class KtorAgendaRemoteDataSource(
    private val httpClient: HttpClient,
) : AgendaRemoteDataSource {
    override suspend fun getDailyAgenda(epochMs: Long): DailyAgendaResponseDto =
        httpClient.get("/agenda") {
            // The Tasky API expects the day as an ISO-8601 timestamp.
            parameter("time", epochMs.toIso8601())
        }.body()

    override suspend fun getFullAgenda(): DailyAgendaResponseDto =
        httpClient.get("/fullAgenda").body()

    override suspend fun syncDeletedItems(
        deletedEventIds: List<String>,
        deletedTaskIds: List<String>,
        deletedReminderIds: List<String>,
    ) {
        httpClient.post("/syncAgenda") {
            contentType(ContentType.Application.Json)
            setBody(
                mapOf(
                    "deletedEventIds" to deletedEventIds,
                    "deletedTaskIds" to deletedTaskIds,
                    "deletedReminderIds" to deletedReminderIds,
                ),
            )
        }
    }
}
