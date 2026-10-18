package com.omaroid.tasky.core.data.agenda.local.dao

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import com.omaroid.tasky.core.data.agenda.local.entities.EventEntity
import com.omaroid.tasky.core.util.DatabaseTest
import kotlinx.coroutines.test.runTest
import org.junit.Test

class EventDaoTest : DatabaseTest() {

    private val dao by lazy { db.eventDao() }

    private fun event(id: String, fromTime: Long, toTime: Long) = EventEntity(
        id = id, title = "T", description = "D",
        fromTime = fromTime, toTime = toTime,
        remindAt = fromTime - 100L, hostId = "host1",
        attendees = emptyList(), photos = emptyList(),
        updatedAt = 0L,
    )

    @Test
    fun upsertAndObserveEventById() = runTest {
        val entity = event("e1", 1_000L, 2_000L)
        dao.upsert(entity)
        dao.observeEvent("e1").test {
            assertThat(awaitItem()).isEqualTo(entity)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun observeEventReturnsNullForUnknownId() = runTest {
        dao.observeEvent("unknown").test {
            assertThat(awaitItem()).isNull()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun getEventsForDayReturnsEventsThatOverlapTheRange() = runTest {
        // overlaps [0, 1000): fromTime=200 < 1000 AND toTime=800 >= 0
        dao.upsert(event("e1", fromTime = 200L,   toTime = 800L))
        // starts after range end: fromTime=1500 is not < 1000
        dao.upsert(event("e2", fromTime = 1_500L, toTime = 2_000L))
        dao.getEventsForDay(startMs = 0L, endMs = 1_000L).test {
            val items = awaitItem()
            assertThat(items).hasSize(1)
            assertThat(items.first().id).isEqualTo("e1")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun getEventsForDayIncludesEventStartingExactlyAtRangeStart() = runTest {
        dao.upsert(event("startEdge", fromTime = 0L,     toTime = 100L))  // exactly startMs — included
        dao.upsert(event("justAfter", fromTime = 1_000L, toTime = 1_100L)) // exactly endMs — excluded
        dao.getEventsForDay(startMs = 0L, endMs = 1_000L).test {
            val items = awaitItem()
            assertThat(items).hasSize(1)
            assertThat(items.first().id).isEqualTo("startEdge")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun getEventsForDayExcludesEventStartingExactlyAtRangeEnd() = runTest {
        dao.upsert(event("endEdge",   fromTime = 999L,   toTime = 1_000L)) // last ms in range — included
        dao.upsert(event("justAfter", fromTime = 1_000L, toTime = 1_100L)) // exactly endMs — excluded
        dao.getEventsForDay(startMs = 0L, endMs = 1_000L).test {
            val items = awaitItem()
            assertThat(items).hasSize(1)
            assertThat(items.first().id).isEqualTo("endEdge")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun deleteByIdRemovesEvent() = runTest {
        dao.upsert(event("e1", 1_000L, 2_000L))
        dao.deleteById("e1")
        dao.observeEvent("e1").test {
            assertThat(awaitItem()).isNull()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun clearAllRemovesAllEvents() = runTest {
        dao.upsert(event("e1", 100L, 200L))
        dao.upsert(event("e2", 300L, 400L))
        dao.clearAll()
        assertThat(dao.getAll()).isEmpty()
    }
}
