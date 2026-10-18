package com.omaroid.tasky.core.data.agenda.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class TaskDto(
    val id: String,
    val title: String,
    val description: String,
    val time: String,       // ISO 8601 string from API
    val remindAt: String,
    val isDone: Boolean,
)