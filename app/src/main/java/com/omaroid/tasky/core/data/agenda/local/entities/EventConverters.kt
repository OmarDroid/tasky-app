package com.omaroid.tasky.core.data.agenda.local.entities

import androidx.room.ProvidedTypeConverter
import androidx.room.TypeConverter
import com.omaroid.tasky.core.data.agenda.remote.dto.AttendeeDto
import com.omaroid.tasky.core.data.agenda.remote.dto.PhotoDto
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@ProvidedTypeConverter
class EventConverters(private val json: Json) {
    @TypeConverter fun attendeesToJson(value: List<AttendeeDto>): String = json.encodeToString(value)
    @TypeConverter fun jsonToAttendees(value: String): List<AttendeeDto> = json.decodeFromString(value)
    @TypeConverter fun photosToJson(value: List<PhotoDto>): String = json.encodeToString(value)
    @TypeConverter fun jsonToPhotos(value: String): List<PhotoDto> = json.decodeFromString(value)
}
