package com.omaroid.tasky.core.data.agenda

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.hasSize
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import com.omaroid.tasky.core.data.agenda.remote.FakeTaskRemoteDataSource
import com.omaroid.tasky.core.data.sync.Outbox
import com.omaroid.tasky.core.data.sync.SyncIntent
import com.omaroid.tasky.core.domain.agenda.models.AgendaItem
import com.omaroid.tasky.core.util.DatabaseTest
import com.omaroid.tasky.core.util.FakeConnectivityMonitor
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Before
import org.junit.Test
import kotlin.time.Clock
import kotlin.time.Instant

class TaskRepositoryImplTest : DatabaseTest() {

    private lateinit var fakeRemote: FakeTaskRemoteDataSource
    private lateinit var connectivity: FakeConnectivityMonitor
    private lateinit var repo: TaskRepositoryImpl

    @Before
    fun setUp() {
        fakeRemote = FakeTaskRemoteDataSource()
        connectivity = FakeConnectivityMonitor(online = true)
        val outbox = Outbox(db.pendingOperationDao(), connectivity, Json, Clock.System)
        repo = TaskRepositoryImpl(db.taskDao(), fakeRemote, outbox)
    }

    private fun task(id: String = "t1") = AgendaItem.Task(
        id = id, title = "T", description = "D",
        time = Instant.fromEpochMilliseconds(1_000L),
        remindAt = Instant.fromEpochMilliseconds(900L),
        isDone = false,
    )

    @Test
    fun createTask_writesToLocalAndCallsCreateRemote() = runTest {
        repo.createTask(task())
        repo.observeTask("t1").test {
            assertThat(awaitItem()?.id).isEqualTo("t1")
            cancelAndIgnoreRemainingEvents()
        }
        assertThat(fakeRemote.created).contains(task())
    }

    @Test
    fun createTask_stillWritesLocallyWhenRemoteThrows() = runTest {
        fakeRemote.shouldThrow = true
        repo.createTask(task())
        repo.observeTask("t1").test {
            assertThat(awaitItem()?.id).isEqualTo("t1")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun updateTask_writesToLocalAndCallsUpdateRemote() = runTest {
        repo.updateTask(task())
        repo.observeTask("t1").test {
            assertThat(awaitItem()?.id).isEqualTo("t1")
            cancelAndIgnoreRemainingEvents()
        }
        assertThat(fakeRemote.updated).contains(task())
    }

    @Test
    fun deleteTask_removesFromLocalAndCallsRemote() = runTest {
        repo.createTask(task())
        repo.deleteTask("t1")
        repo.observeTask("t1").test {
            assertThat(awaitItem()).isNull()
            cancelAndIgnoreRemainingEvents()
        }
        assertThat(fakeRemote.deleted).contains("t1")
    }

    // ── Outbox behaviour ──────────────────────────────────────────────────

    @Test
    fun createTask_whenOffline_enqueuesExactlyOneRow() = runTest {
        connectivity.setOnline(false)
        repo.createTask(task())

        val ops = db.pendingOperationDao().getAll()
        assertThat(ops).hasSize(1)
        assertThat(ops.single().id).isEqualTo("TASK:t1")
        assertThat(ops.single().intent).isEqualTo(SyncIntent.CREATE)
        // Offline: the remote must NOT have been called.
        assertThat(fakeRemote.created).isEmpty()
    }

    @Test
    fun createTask_whenOnlineAndPushSucceeds_enqueuesNothing() = runTest {
        repo.createTask(task())
        assertThat(db.pendingOperationDao().getAll()).isEmpty()
    }

    @Test
    fun createThenDelete_whileOffline_coalescesToEmptyQueue() = runTest {
        connectivity.setOnline(false)
        repo.createTask(task())
        repo.deleteTask("t1")
        // CREATE still pending + DELETE → item never reached the server: both cancelled.
        assertThat(db.pendingOperationDao().getAll()).isEmpty()
    }
}
