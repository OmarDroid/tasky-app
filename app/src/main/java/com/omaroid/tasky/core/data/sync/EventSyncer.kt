package com.omaroid.tasky.core.data.sync

import com.omaroid.tasky.core.data.agenda.EventSyncPayload
import com.omaroid.tasky.core.domain.agenda.models.AgendaItem

/**
 * A DATA-LAYER port (keeps the domain `EventRepository` clean of sync concerns) that performs ONLY the
 * remote two-phase event push and THROWS on failure. Implemented by `EventRepositoryImpl` and injected
 * into [SyncWorker], so the S3 upload logic lives in exactly one place — reused by both the live UI
 * write (via `Outbox`) and the background replay (via the worker).
 */
interface EventSyncer {
    suspend fun pushEvent(intent: SyncIntent, event: AgendaItem.Event, payload: EventSyncPayload)
    suspend fun pushDelete(eventId: String)
}
