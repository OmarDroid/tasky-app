package com.omaroid.tasky.core.data.agenda

import com.omaroid.tasky.core.domain.agenda.models.AgendaItem
import com.omaroid.tasky.core.domain.agenda.repository.EventRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class FakeEventRepository : EventRepository {
    private val _events = MutableStateFlow<List<AgendaItem.Event>>(emptyList())
    val events: List<AgendaItem.Event> get() = _events.value

    // Captured arguments so tests can assert what the ViewModel passed to the repo.
    var lastCreateLocalPhotoUris: List<String> = emptyList(); private set
    var lastUpdateLocalPhotoUris: List<String> = emptyList(); private set
    var lastUpdateDeletedPhotoKeys: List<String> = emptyList(); private set
    var lastUpdateIsGoing: Boolean = true; private set

    override fun observeEvent(id: String) = _events.map { it.find { e -> e.id == id } }
    override fun observeEventsForDay(startOfDayMs: Long, endOfDayMs: Long) = _events.map { list ->
        list.filter { it.time.toEpochMilliseconds() in startOfDayMs until endOfDayMs }
    }

    override suspend fun createEvent(event: AgendaItem.Event, localPhotoUris: List<String>) {
        lastCreateLocalPhotoUris = localPhotoUris
        _events.update { it.filterNot { x -> x.id == event.id } + event }
    }

    override suspend fun updateEvent(
        event: AgendaItem.Event,
        localPhotoUris: List<String>,
        deletedPhotoKeys: List<String>,
        isGoing: Boolean,
    ) {
        lastUpdateLocalPhotoUris = localPhotoUris
        lastUpdateDeletedPhotoKeys = deletedPhotoKeys
        lastUpdateIsGoing = isGoing
        _events.update { it.filterNot { x -> x.id == event.id } + event }
    }

    /** Test-only helper — seeds the in-memory store directly without create/update semantics. */
    fun upsertEvent(event: AgendaItem.Event) {
        _events.update { it.filterNot { x -> x.id == event.id } + event }
    }

    override suspend fun deleteEvent(id: String) {
        _events.update { it.filterNot { e -> e.id == id } }
    }

    override suspend fun clearAll() {
        _events.value = emptyList()
    }
}
