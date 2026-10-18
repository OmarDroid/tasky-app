package com.omaroid.tasky.core.data.agenda

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import com.omaroid.tasky.core.data.agenda.remote.FakeReminderRemoteDataSource
import com.omaroid.tasky.core.data.sync.Outbox
import com.omaroid.tasky.core.domain.agenda.models.AgendaItem
import com.omaroid.tasky.core.util.DatabaseTest
import com.omaroid.tasky.core.util.FakeConnectivityMonitor
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Before
import org.junit.Test
import kotlin.time.Clock
import kotlin.time.Instant

class ReminderRepositoryImplTest : DatabaseTest() {

    private lateinit var fakeRemote: FakeReminderRemoteDataSource
    private lateinit var repo: ReminderRepositoryImpl

    @Before
    fun setUp() {
        fakeRemote = FakeReminderRemoteDataSource()
        val outbox = Outbox(db.pendingOperationDao(), FakeConnectivityMonitor(online = true), Json, Clock.System)
        repo = ReminderRepositoryImpl(db.reminderDao(), fakeRemote, outbox)
    }

    private fun reminder(id: String = "r1") = AgendaItem.Reminder(
        id = id, title = "R", description = "D",
        time = Instant.fromEpochMilliseconds(1_000L),
        remindAt = Instant.fromEpochMilliseconds(900L),
    )

    @Test
    fun createReminder_writesToLocalAndCallsCreateRemote() = runTest {
        repo.createReminder(reminder())
        repo.observeReminder("r1").test {
            assertThat(awaitItem()?.id).isEqualTo("r1")
            cancelAndIgnoreRemainingEvents()
        }
        assertThat(fakeRemote.created).contains(reminder())
    }

    @Test
    fun createReminder_stillWritesLocallyWhenRemoteThrows() = runTest {
        fakeRemote.shouldThrow = true
        repo.createReminder(reminder())
        repo.observeReminder("r1").test {
            assertThat(awaitItem()?.id).isEqualTo("r1")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun updateReminder_writesToLocalAndCallsUpdateRemote() = runTest {
        repo.updateReminder(reminder())
        repo.observeReminder("r1").test {
            assertThat(awaitItem()?.id).isEqualTo("r1")
            cancelAndIgnoreRemainingEvents()
        }
        assertThat(fakeRemote.updated).contains(reminder())
    }

    @Test
    fun deleteReminder_removesFromLocalAndCallsRemote() = runTest {
        repo.createReminder(reminder())
        repo.deleteReminder("r1")
        repo.observeReminder("r1").test {
            assertThat(awaitItem()).isNull()
            cancelAndIgnoreRemainingEvents()
        }
        assertThat(fakeRemote.deleted).contains("r1")
    }
}
