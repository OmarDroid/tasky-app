package com.omaroid.tasky.core.data.agenda

import com.omaroid.tasky.core.data.agenda.local.dao.EventDao
import com.omaroid.tasky.core.data.agenda.remote.EventRemoteDataSource
import com.omaroid.tasky.core.data.agenda.remote.dto.EventResponseDto
import com.omaroid.tasky.core.data.agenda.remote.mappers.toCreateDto
import com.omaroid.tasky.core.data.agenda.remote.mappers.toDomain
import com.omaroid.tasky.core.data.agenda.remote.mappers.toEntity
import com.omaroid.tasky.core.data.agenda.remote.mappers.toUpdateDto
import com.omaroid.tasky.core.data.sync.EventSyncer
import com.omaroid.tasky.core.data.sync.Outbox
import com.omaroid.tasky.core.data.sync.SyncIntent
import com.omaroid.tasky.core.data.sync.SyncItemType
import com.omaroid.tasky.core.domain.agenda.models.AgendaItem
import com.omaroid.tasky.core.domain.agenda.repository.EventRepository
import com.omaroid.tasky.core.domain.photo.PhotoCompressor
import kotlinx.coroutines.flow.map
import kotlin.time.Clock

/**
 * Implements both the domain [EventRepository] (used by the UI) and the data-layer [EventSyncer] (used
 * by the SyncWorker). Binding both to the same instance means the two-phase upload logic lives in one
 * place ([pushEvent]) and is shared by the live write path (via [Outbox]) and the background replay.
 */
class EventRepositoryImpl(
    private val eventDao: EventDao,
    private val eventRemote: EventRemoteDataSource,
    private val outbox: Outbox,
    private val photoCompressor: PhotoCompressor,
    private val clock: Clock = Clock.System,
) : EventRepository, EventSyncer {

    override fun observeEvent(eventId: String) =
        eventDao.observeEvent(eventId).map { it?.toDomain() }

    override fun observeEventsForDay(startMs: Long, endMs: Long) =
        eventDao.getEventsForDay(startMs, endMs).map { list -> list.map { it.toDomain() } }

    // Offline-first: the Room write (with the locally-cached file:// photos already on `event`) always
    // succeeds. Outbox pushes when online (and clears any queued op) or records the intent otherwise.
    override suspend fun createEvent(event: AgendaItem.Event, localPhotoUris: List<String>) {
        eventDao.upsert(event.toEntity(updatedAt = clock.now().toEpochMilliseconds()))
        val payload = EventSyncPayload(localPhotoUris = localPhotoUris)
        outbox.sync(SyncItemType.EVENT, event.id, SyncIntent.CREATE, payload) {
            pushEvent(SyncIntent.CREATE, event, payload)
        }
    }

    override suspend fun updateEvent(
        event: AgendaItem.Event,
        localPhotoUris: List<String>,
        deletedPhotoKeys: List<String>,
        isGoing: Boolean,
    ) {
        eventDao.upsert(event.toEntity(updatedAt = clock.now().toEpochMilliseconds()))
        val payload = EventSyncPayload(localPhotoUris, deletedPhotoKeys, isGoing)
        outbox.sync(SyncItemType.EVENT, event.id, SyncIntent.UPDATE, payload) {
            pushEvent(SyncIntent.UPDATE, event, payload)
        }
    }

    override suspend fun deleteEvent(eventId: String) {
        eventDao.deleteById(eventId)
        outbox.sync(SyncItemType.EVENT, eventId, SyncIntent.DELETE) { pushDelete(eventId) }
    }

    // EventSyncer — the two-phase push, in ONE place. Throws on failure so both Outbox (UI path) and
    // SyncWorker (replay path) observe it and decide what to do; neither duplicates the upload logic.
    override suspend fun pushEvent(intent: SyncIntent, event: AgendaItem.Event, payload: EventSyncPayload) {
        val updatedAt = clock.now().toEpochMilliseconds()
        val photoKeys = payload.localPhotoUris.indices.map { "photo$it" }
        val response = if (intent == SyncIntent.CREATE) {
            eventRemote.createEvent(event.toCreateDto(photoKeys, updatedAt))
        } else {
            eventRemote.updateEvent(
                event.id,
                event.toUpdateDto(updatedAt, payload.isGoing, photoKeys, payload.deletedPhotoKeys),
            )
        }
        val confirmed = uploadAndConfirm(event.id, response, payload.localPhotoUris)
        eventDao.upsert((confirmed ?: response.event).toEntity(updatedAt))
    }

    override suspend fun pushDelete(eventId: String) {
        eventRemote.deleteEvent(eventId)
    }

    // Phase 2: compress each new local photo, PUT it to its pre-signed S3 url, then confirm with the
    // server-generated uploadKey. Photos that can't be compressed under 1 MB are skipped. Returns the
    // confirmed event (with accessible photo urls), or null if there was nothing to upload.
    private suspend fun uploadAndConfirm(
        eventId: String,
        response: EventResponseDto,
        localPhotoUris: List<String>,
    ) = run {
        val uploadedKeys = mutableListOf<String>()
        response.uploadUrls.zip(localPhotoUris).forEach { (urlDto, uri) ->
            val bytes = photoCompressor.compress(uri) ?: return@forEach // skip still-too-large
            eventRemote.uploadPhoto(urlDto.url, bytes)
            uploadedKeys += urlDto.uploadKey
        }
        if (uploadedKeys.isNotEmpty()) eventRemote.confirmUpload(eventId, uploadedKeys) else null
    }

    override suspend fun clearAll() = eventDao.clearAll()
}
