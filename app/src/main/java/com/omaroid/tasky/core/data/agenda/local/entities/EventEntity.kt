package com.omaroid.tasky.core.data.agenda.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.omaroid.tasky.core.data.agenda.remote.dto.AttendeeDto
import com.omaroid.tasky.core.data.agenda.remote.dto.PhotoDto

@Entity(tableName = "events")
@TypeConverters(EventConverters::class)
data class EventEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val fromTime: Long,
    val toTime: Long,
    val remindAt: Long,
    val hostId: String,
    // Stored as JSON via EventConverters. The serializable DTOs double as the storage format.
    val attendees: List<AttendeeDto>,
    val photos: List<PhotoDto>,
    val updatedAt: Long,
)
