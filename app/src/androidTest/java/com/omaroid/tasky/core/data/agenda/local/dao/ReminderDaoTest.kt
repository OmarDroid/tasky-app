package com.omaroid.tasky.core.data.agenda.local.dao

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import com.omaroid.tasky.core.data.agenda.local.entities.ReminderEntity
import com.omaroid.tasky.core.util.DatabaseTest
import kotlinx.coroutines.test.runTest
import org.junit.Test

class ReminderDaoTest : DatabaseTest() {

    private val dao by lazy { db.reminderDao() }

    @Test
    fun upsertAndObserveReminderById() = runTest {
        val entity = ReminderEntity("r1", "Title", "Desc", 1_000L, 900L, 0L)
        dao.upsert(entity)
        dao.observeReminder("r1").test {
            assertThat(awaitItem()).isEqualTo(entity)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun observeReminderReturnsNullForUnknownId() = runTest {
        dao.observeReminder("unknown").test {
            assertThat(awaitItem()).isNull()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun getRemindersForDayReturnsOnlyRemindersInRange() = runTest {
        dao.upsert(ReminderEntity("r1", "In range",  "D", 500L,   400L,  0L))
        dao.upsert(ReminderEntity("r2", "Out range", "D", 2_000L, 1_900L, 0L))
        dao.getRemindersForDay(0L, 1_000L).test {
            val items = awaitItem()
            assertThat(items).hasSize(1)
            assertThat(items.first().id).isEqualTo("r1")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun getRemindersForDayIncludesReminderAtExactRangeStart() = runTest {
        dao.upsert(ReminderEntity("startEdge", "At start",  "D", 0L,     0L,   0L)) // exactly startMs — included
        dao.upsert(ReminderEntity("justAfter", "Just after","D", 1_000L, 900L, 0L)) // exactly endMs — excluded
        dao.getRemindersForDay(0L, 1_000L).test {
            val items = awaitItem()
            assertThat(items).hasSize(1)
            assertThat(items.first().id).isEqualTo("startEdge")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun getRemindersForDayExcludesReminderAtExactRangeEnd() = runTest {
        dao.upsert(ReminderEntity("endEdge",   "End edge",  "D", 999L,   899L, 0L)) // last ms in range — included
        dao.upsert(ReminderEntity("justAfter", "Just after","D", 1_000L, 900L, 0L)) // exactly endMs — excluded
        dao.getRemindersForDay(0L, 1_000L).test {
            val items = awaitItem()
            assertThat(items).hasSize(1)
            assertThat(items.first().id).isEqualTo("endEdge")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun deleteByIdRemovesReminder() = runTest {
        dao.upsert(ReminderEntity("r1", "Title", "Desc", 1_000L, 900L, 0L))
        dao.deleteById("r1")
        dao.observeReminder("r1").test {
            assertThat(awaitItem()).isNull()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun clearAllRemovesAllReminders() = runTest {
        dao.upsert(ReminderEntity("r1", "A", "D", 100L, 90L, 0L))
        dao.upsert(ReminderEntity("r2", "B", "D", 200L, 190L, 0L))
        dao.clearAll()
        assertThat(dao.getAll()).isEmpty()
    }
}
