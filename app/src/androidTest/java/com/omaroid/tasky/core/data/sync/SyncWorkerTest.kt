package com.omaroid.tasky.core.data.sync

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.hasSize
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import com.omaroid.tasky.core.data.agenda.EventSyncPayload
import com.omaroid.tasky.core.data.agenda.local.dao.EventDao
import com.omaroid.tasky.core.data.agenda.local.dao.PendingOperationDao
import com.omaroid.tasky.core.data.agenda.local.dao.ReminderDao
import com.omaroid.tasky.core.data.agenda.local.dao.TaskDao
import com.omaroid.tasky.core.data.agenda.local.entities.PendingOperationEntity
import com.omaroid.tasky.core.data.agenda.remote.AgendaRemoteDataSource
import com.omaroid.tasky.core.data.agenda.remote.ReminderRemoteDataSource
import com.omaroid.tasky.core.data.agenda.remote.TaskRemoteDataSource
import com.omaroid.tasky.core.data.agenda.remote.dto.DailyAgendaResponseDto
import com.omaroid.tasky.core.domain.agenda.models.AgendaItem
import com.omaroid.tasky.core.domain.agenda.repository.AgendaSyncRepository
import com.omaroid.tasky.core.util.DatabaseTest
import com.omaroid.tasky.core.util.eventEntity
import com.omaroid.tasky.core.util.taskEntity
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Before
import org.junit.Test

class SyncWorkerTest : DatabaseTest() {

    private lateinit var pendingDao: PendingOperationDao
    private lateinit var taskDao: TaskDao
    private lateinit var reminderDao: ReminderDao
    private lateinit var eventDao: EventDao
    private lateinit var fakeTaskRemote: FakeSyncTaskRemoteDataSource
    private lateinit var fakeReminderRemote: FakeSyncReminderRemoteDataSource
    private lateinit var fakeEventSyncer: FakeEventSyncer
    private lateinit var fakeAgendaRemote: FakeAgendaRemoteDataSource
    private lateinit var fakeAgendaSync: FakeAgendaSyncRepository
    private val json = Json

    @Before
    fun setup() {
        pendingDao = db.pendingOperationDao()
        taskDao = db.taskDao()
        reminderDao = db.reminderDao()
        eventDao = db.eventDao()
        fakeTaskRemote = FakeSyncTaskRemoteDataSource()
        fakeReminderRemote = FakeSyncReminderRemoteDataSource()
        fakeEventSyncer = FakeEventSyncer()
        fakeAgendaRemote = FakeAgendaRemoteDataSource()
        fakeAgendaSync = FakeAgendaSyncRepository()
        // No WorkManager init needed — TestListenableWorkerBuilder instantiates the worker directly and
        // we drive doWork() ourselves. (TaskyApp already initializes WorkManager for instrumented runs.)
    }

    private fun buildWorker(): SyncWorker {
        val context = ApplicationProvider.getApplicationContext<Context>()
        return TestListenableWorkerBuilder<SyncWorker>(context)
            .setWorkerFactory(object : WorkerFactory() {
                override fun createWorker(
                    appContext: Context,
                    workerClassName: String,
                    workerParameters: WorkerParameters,
                ) = SyncWorker(
                    appContext, workerParameters,
                    pendingDao, taskDao, reminderDao, eventDao,
                    fakeTaskRemote, fakeReminderRemote, fakeEventSyncer,
                    fakeAgendaRemote, fakeAgendaSync, json,
                )
            }).build()
    }

    @Test
    fun doWork_processesCreateTask_fromDao_andClearsQueue() = runTest {
        taskDao.upsert(taskEntity(id = "task1", title = "Test"))
        pendingDao.upsert(
            PendingOperationEntity(
                id = "TASK:task1", itemType = SyncItemType.TASK, itemId = "task1",
                intent = SyncIntent.CREATE, createdAt = 1000L,
            )
        )

        val result = buildWorker().doWork()

        assertThat(result).isEqualTo(ListenableWorker.Result.success())
        assertThat(fakeTaskRemote.createdTasks.map { it.id }).contains("task1")
        assertThat(fakeAgendaSync.syncAllCalls).isEqualTo(1) // pull path ran
        assertThat(pendingDao.getAll()).isEmpty()
    }

    // Deletions go through the batch POST /syncAgenda channel (the API's two-tier bulk-delete tier),
    // NOT the individual DELETE endpoints.
    @Test
    fun doWork_batchesDeletesAcrossTypes_viaSyncAgenda_andClearsQueue() = runTest {
        pendingDao.upsert(
            PendingOperationEntity("REMINDER:rem1", SyncItemType.REMINDER, "rem1", SyncIntent.DELETE, createdAt = 2000L),
        )
        pendingDao.upsert(
            PendingOperationEntity("TASK:task9", SyncItemType.TASK, "task9", SyncIntent.DELETE, createdAt = 2100L),
        )
        pendingDao.upsert(
            PendingOperationEntity("EVENT:ev9", SyncItemType.EVENT, "ev9", SyncIntent.DELETE, createdAt = 2200L),
        )

        val result = buildWorker().doWork()

        assertThat(result).isEqualTo(ListenableWorker.Result.success())
        // Exactly ONE batch call carrying all three ids grouped by type.
        assertThat(fakeAgendaRemote.syncCalls).hasSize(1)
        val call = fakeAgendaRemote.syncCalls.single()
        assertThat(call.reminders).contains("rem1")
        assertThat(call.tasks).contains("task9")
        assertThat(call.events).contains("ev9")
        // Individual DELETE endpoints must NOT have been used.
        assertThat(fakeReminderRemote.deletedIds).isEmpty()
        assertThat(fakeEventSyncer.deleted).isEmpty()
        assertThat(pendingDao.getAll()).isEmpty()
    }

    @Test
    fun doWork_processesCreateEvent_pushesEventAndPhotosViaSyncer() = runTest {
        eventDao.upsert(eventEntity(id = "e1", title = "Kickoff"))
        pendingDao.upsert(
            PendingOperationEntity(
                id = "EVENT:e1", itemType = SyncItemType.EVENT, itemId = "e1", intent = SyncIntent.CREATE,
                payloadJson = json.encodeToString(EventSyncPayload(localPhotoUris = listOf("file://p1"))),
                createdAt = 1500L,
            )
        )

        val result = buildWorker().doWork()

        assertThat(result).isEqualTo(ListenableWorker.Result.success())
        val pushed = fakeEventSyncer.pushed.single()
        assertThat(pushed.eventId).isEqualTo("e1")                       // event replayed
        assertThat(pushed.payload.localPhotoUris).contains("file://p1")  // photos forwarded
        assertThat(pendingDao.getAll()).isEmpty()                        // queue drained
    }
}

class FakeSyncTaskRemoteDataSource : TaskRemoteDataSource {
    val createdTasks = mutableListOf<AgendaItem.Task>()
    val updatedTasks = mutableListOf<AgendaItem.Task>()
    val deletedIds = mutableListOf<String>()

    override suspend fun createTask(task: AgendaItem.Task) { createdTasks += task }
    override suspend fun updateTask(task: AgendaItem.Task) { updatedTasks += task }
    override suspend fun deleteTask(taskId: String, deletedAt: String) { deletedIds += taskId }
}

class FakeSyncReminderRemoteDataSource : ReminderRemoteDataSource {
    val createdReminders = mutableListOf<AgendaItem.Reminder>()
    val updatedReminders = mutableListOf<AgendaItem.Reminder>()
    val deletedIds = mutableListOf<String>()

    override suspend fun createReminder(reminder: AgendaItem.Reminder) { createdReminders += reminder }
    override suspend fun updateReminder(reminder: AgendaItem.Reminder) { updatedReminders += reminder }
    override suspend fun deleteReminder(reminderId: String, deletedAt: String) { deletedIds += reminderId }
}

class FakeEventSyncer : EventSyncer {
    data class Pushed(val intent: SyncIntent, val eventId: String, val payload: EventSyncPayload)
    val pushed = mutableListOf<Pushed>()
    val deleted = mutableListOf<String>()
    override suspend fun pushEvent(intent: SyncIntent, event: AgendaItem.Event, payload: EventSyncPayload) {
        pushed += Pushed(intent, event.id, payload)
    }
    override suspend fun pushDelete(eventId: String) { deleted += eventId }
}

class FakeAgendaSyncRepository : AgendaSyncRepository {
    var syncAllCalls = 0; private set
    override suspend fun syncAll() { syncAllCalls++ }
}

class FakeAgendaRemoteDataSource : AgendaRemoteDataSource {
    data class SyncCall(val events: List<String>, val tasks: List<String>, val reminders: List<String>)
    val syncCalls = mutableListOf<SyncCall>()

    override suspend fun getDailyAgenda(epochMs: Long): DailyAgendaResponseDto = error("unused in SyncWorkerTest")
    override suspend fun getFullAgenda(): DailyAgendaResponseDto = error("unused in SyncWorkerTest")
    override suspend fun syncDeletedItems(
        deletedEventsId: List<String>,
        deletedTasksId: List<String>,
        deletedReminderIds: List<String>,
    ) {
        syncCalls += SyncCall(deletedEventsId, deletedTasksId, deletedReminderIds)
    }
}
