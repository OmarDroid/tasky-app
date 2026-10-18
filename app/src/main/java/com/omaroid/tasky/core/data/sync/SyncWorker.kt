package com.omaroid.tasky.core.data.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.omaroid.tasky.core.data.agenda.EventSyncPayload
import com.omaroid.tasky.core.data.agenda.local.dao.EventDao
import com.omaroid.tasky.core.data.agenda.local.dao.PendingOperationDao
import com.omaroid.tasky.core.data.agenda.local.dao.ReminderDao
import com.omaroid.tasky.core.data.agenda.local.dao.TaskDao
import com.omaroid.tasky.core.data.agenda.local.entities.PendingOperationEntity
import com.omaroid.tasky.core.data.agenda.remote.AgendaRemoteDataSource
import com.omaroid.tasky.core.data.agenda.remote.ReminderRemoteDataSource
import com.omaroid.tasky.core.data.agenda.remote.TaskRemoteDataSource
import com.omaroid.tasky.core.data.agenda.remote.mappers.toDomain
import com.omaroid.tasky.core.domain.agenda.repository.AgendaSyncRepository
import io.ktor.client.plugins.ClientRequestException
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json

/**
 * Drains the outbox, then does the periodic pull. Follows the API's **two-tier sync strategy**:
 * CREATE/UPDATE ops are pushed **individually** through their CRUD endpoints (each in its own
 * try/catch, reading the freshest body from its DAO); DELETE ops are collected across all types and
 * sent as **one** `POST /syncAgenda` batch (`syncDeletedItems`), which the API mandates for deletions.
 *
 * Failures are classified: a **terminal** (4xx) failure is a poison op — dropped so it can't block the
 * queue forever; a **transient** (network/5xx) failure keeps the op(s) and asks WorkManager to retry.
 */
class SyncWorker(
    appContext: Context,
    params: WorkerParameters,
    private val pendingDao: PendingOperationDao,
    private val taskDao: TaskDao,
    private val reminderDao: ReminderDao,
    private val eventDao: EventDao,
    private val taskRemote: TaskRemoteDataSource,
    private val reminderRemote: ReminderRemoteDataSource,
    private val eventSyncer: EventSyncer,                   // the two-phase event push (throws on failure)
    private val agendaRemote: AgendaRemoteDataSource,       // POST /syncAgenda — the bulk-delete channel
    private val agendaSyncRepository: AgendaSyncRepository, // the single pull path (Room + notify)
    private val json: Json,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        var retryNeeded = false
        // Oldest-first so a CREATE precedes its later UPDATE for the same item. The outbox keeps at most
        // one row per item, so an item is never queued as both a write and a delete at the same time.
        val ops = pendingDao.getAll().sortedBy { it.createdAt }
        val (deletes, writes) = ops.partition { it.intent == SyncIntent.DELETE }

        // Tier 1 — individual CREATE/UPDATE pushes.
        for (op in writes) {
            try {
                pushWrite(op)
                pendingDao.delete(op.id)                   // success → drop the op
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (e.isTerminal()) pendingDao.delete(op.id) // 4xx poison → drop; never block the queue
                else retryNeeded = true                      // transient → keep op, retry the run later
            }
        }

        // Tier 2 — ONE batch delete for everything queued, across all three types (POST /syncAgenda).
        if (deletes.isNotEmpty()) {
            try {
                agendaRemote.syncDeletedItems(
                    deletedEventsId = deletes.filter { it.itemType == SyncItemType.EVENT }.map { it.itemId },
                    deletedTasksId = deletes.filter { it.itemType == SyncItemType.TASK }.map { it.itemId },
                    deletedReminderIds = deletes.filter { it.itemType == SyncItemType.REMINDER }.map { it.itemId },
                )
                deletes.forEach { pendingDao.delete(it.id) } // whole batch acknowledged → drop them all
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (e.isTerminal()) deletes.forEach { pendingDao.delete(it.id) } // poison batch → drop
                else retryNeeded = true                                          // transient → keep, retry
            }
        }

        return try {
            agendaSyncRepository.syncAll()                // pull: writes Room (and surfaces invites)
            if (retryNeeded) Result.retry() else Result.success()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.retry()
        }
    }

    // One CREATE/UPDATE push per op. The body is read fresh from the DAO so the latest edit is what syncs.
    private suspend fun pushWrite(op: PendingOperationEntity) = when (op.itemType) {
        SyncItemType.TASK -> taskDao.getById(op.itemId)?.toDomain()?.let {
            if (op.intent == SyncIntent.CREATE) taskRemote.createTask(it) else taskRemote.updateTask(it)
        } ?: Unit                                        // row gone locally → nothing to push

        SyncItemType.REMINDER -> reminderDao.getById(op.itemId)?.toDomain()?.let {
            if (op.intent == SyncIntent.CREATE) reminderRemote.createReminder(it)
            else reminderRemote.updateReminder(it)
        } ?: Unit

        SyncItemType.EVENT -> eventDao.getById(op.itemId)?.toDomain()?.let { event ->
            val payload = op.payloadJson
                ?.let { json.decodeFromString<EventSyncPayload>(it) } ?: EventSyncPayload()
            eventSyncer.pushEvent(op.intent, event, payload)
        } ?: Unit
    }

    // 4xx (except 408 Timeout / 429 Too-Many-Requests) = permanent rejection; retrying won't help, so
    // drop the op. This intentionally also drops a 409 Conflict — for an idempotent CREATE replay that
    // means "already exists" (success); for an UPDATE/DELETE it means the server already diverged and a
    // blind retry can't fix it, so the next full sync reconciles instead.
    private fun Throwable.isTerminal(): Boolean =
        this is ClientRequestException &&
            response.status.value.let { it in 400..499 && it != 408 && it != 429 }
}
