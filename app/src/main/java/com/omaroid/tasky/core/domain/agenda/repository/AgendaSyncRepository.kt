package com.omaroid.tasky.core.domain.agenda.repository

/**
 * Synchronises the full server agenda into local storage.
 *
 * Writing remote data through the individual [TaskRepository.createTask] /
 * [ReminderRepository.createReminder] methods would re-upload every item as a create
 * request. [syncAll] writes directly to Room without touching the remote, preventing
 * circular API calls.
 */
interface AgendaSyncRepository {
    /** Fetch the full agenda from the server and persist all items to local storage only. */
    suspend fun syncAll()
}
