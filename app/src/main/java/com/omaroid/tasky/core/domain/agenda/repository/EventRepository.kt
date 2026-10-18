package com.omaroid.tasky.core.domain.agenda.repository

import com.omaroid.tasky.core.domain.agenda.models.AgendaItem
import kotlinx.coroutines.flow.Flow

interface EventRepository {
    fun observeEvent(eventId: String): Flow<AgendaItem.Event?>
    fun observeEventsForDay(startOfDayMs: Long, endOfDayMs: Long): Flow<List<AgendaItem.Event>>

    // localPhotoUris are stable file:// uris of newly-picked photos to upload (multipart photo parts).
    suspend fun createEvent(event: AgendaItem.Event, localPhotoUris: List<String> = emptyList())

    // deletedPhotoKeys are remote photo keys removed in this edit. isGoing is the current user's own
    // going status sent on PUT /event (creators are always going; attendees toggle it via join/leave).
    suspend fun updateEvent(
        event: AgendaItem.Event,
        localPhotoUris: List<String> = emptyList(),
        deletedPhotoKeys: List<String> = emptyList(),
        isGoing: Boolean = true,
    )

    suspend fun deleteEvent(eventId: String)
    suspend fun clearAll()
}
