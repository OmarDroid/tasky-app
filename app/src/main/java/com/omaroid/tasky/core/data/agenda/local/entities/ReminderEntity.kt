package com.omaroid.tasky.core.data.agenda.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val time: Long,
    val remindAt: Long,
    val updatedAt: Long,
)
