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

class KtorTaskRemoteDataSource(
    private val httpClient: HttpClient
): TaskRemoteDataSource {
    override suspend fun createTask(task: AgendaItem.Task) {
        httpClient.post("/task") {
            contentType(ContentType.Application.Json)
            setBody(task.toCreateDto())
        }
    }

    override suspend fun updateTask(task: AgendaItem.Task) {
        httpClient.put("/task") {
            contentType(ContentType.Application.Json)
            setBody(task.toUpdateDto())
        }
    }

    override suspend fun deleteTask(taskId: String, deletedAt: String) {
        httpClient.delete("/task/$taskId") {
            parameter("deletedAt", deletedAt)
        }
    }
}