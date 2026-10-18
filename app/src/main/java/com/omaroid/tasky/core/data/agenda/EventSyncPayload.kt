package com.omaroid.tasky.core.data.agenda

import kotlinx.serialization.Serializable

/**
 * The event-only deltas that aren't reconstructable from the Room row: which local `file://` picks to
 * upload, which remote photo keys to delete, and going/not-going. Carried in
 * [com.omaroid.tasky.core.data.agenda.local.entities.PendingOperationEntity.payloadJson] and merged
 * (union of photo lists, latest `isGoing`) across offline edits by the `Outbox`.
 */
@Serializable
data class EventSyncPayload(
    val localPhotoUris: List<String> = emptyList(),  // file:// picks to upload
    val deletedPhotoKeys: List<String> = emptyList(),
    val isGoing: Boolean = true,
)
