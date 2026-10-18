package com.omaroid.tasky.core.data.agenda.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class ReminderDto(
    val id: String,
    val title: String,
    val description: String,
    val time: String,
    val remindAt: String,
)
