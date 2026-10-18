package com.omaroid.tasky.core.data.sync

import com.omaroid.tasky.core.data.agenda.EventSyncPayload
import com.omaroid.tasky.core.data.agenda.local.dao.PendingOperationDao
import com.omaroid.tasky.core.data.agenda.local.entities.PendingOperationEntity
import com.omaroid.tasky.core.domain.connectivity.ConnectivityMonitor
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.time.Clock

/**
 * The single place that decides "push now or queue". All three repositories call [sync] on a write, so
 * the online check, queue-on-failure, and identity-keyed coalescing rules live in exactly one class —
 * no repository touches [PendingOperationDao] or [ConnectivityMonitor] directly.
 */
class Outbox(
    private val dao: PendingOperationDao,
    private val connectivity: ConnectivityMonitor,
    private val json: Json,
    private val clock: Clock,
) {
    // enqueue() is read-modify-write on one row (getById → merge → upsert). Serialize it so two
    // concurrent offline writes to the same item can't lose a coalesce/merge (last-REPLACE-wins).
    private val mutex = Mutex()

    /**
     * Push to the server when online; otherwise (or on failure) record the intent. A successful online
     * push also clears any stale queued op for this item. Concurrency: this and the [SyncWorker] may
     * push the same item at once — safe because CREATE replays are idempotent on client id.
     */
    suspend fun sync(
        itemType: SyncItemType,
        itemId: String,
        intent: SyncIntent,
        payload: EventSyncPayload? = null,
        push: suspend () -> Unit,
    ) {
        val id = "$itemType:$itemId"
        if (connectivity.isOnline.value) {
            try {
                push()
                dao.delete(id)
                return
            } catch (e: Exception) {
                if (e is CancellationException) throw e // fall through to queue
            }
        }
        enqueue(id, itemType, itemId, intent, payload)
    }

    private suspend fun enqueue(
        id: String,
        itemType: SyncItemType,
        itemId: String,
        intent: SyncIntent,
        payload: EventSyncPayload?,
    ) = mutex.withLock {
        val existing = dao.getById(id)
        // CREATE still pending + now DELETE → item never reached the server: cancel both (no round-trip).
        if (existing?.intent == SyncIntent.CREATE && intent == SyncIntent.DELETE) {
            dao.delete(id)
            return@withLock
        }
        // A CREATE that is later edited is still a CREATE (the body is read fresh at sync anyway).
        val effectiveIntent =
            if (existing?.intent == SyncIntent.CREATE && intent == SyncIntent.UPDATE) SyncIntent.CREATE
            else intent
        dao.upsert(
            PendingOperationEntity(
                id = id,
                itemType = itemType,
                itemId = itemId,
                intent = effectiveIntent,
                payloadJson = merge(existing, payload)?.let { json.encodeToString(it) },
                createdAt = existing?.createdAt ?: clock.now().toEpochMilliseconds(), // keep original order
            )
        )
    }

    // Event deltas ACCUMULATE across offline edits — a deletion queued in an earlier edit must not be
    // lost when a later edit replaces the row. (localPhotoUris/deletedPhotoKeys union; isGoing = latest.)
    private fun merge(existing: PendingOperationEntity?, incoming: EventSyncPayload?): EventSyncPayload? {
        if (incoming == null) return null
        val prev = existing?.payloadJson
            ?.let { json.decodeFromString<EventSyncPayload>(it) } ?: return incoming
        return EventSyncPayload(
            localPhotoUris = (prev.localPhotoUris + incoming.localPhotoUris).distinct(),
            deletedPhotoKeys = (prev.deletedPhotoKeys + incoming.deletedPhotoKeys).distinct(),
            isGoing = incoming.isGoing,
        )
    }
}
