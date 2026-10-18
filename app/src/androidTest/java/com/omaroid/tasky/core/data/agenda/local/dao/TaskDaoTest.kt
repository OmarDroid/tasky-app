package com.omaroid.tasky.core.data.agenda.local.dao

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import com.omaroid.tasky.core.data.agenda.local.entities.TaskEntity
import com.omaroid.tasky.core.util.DatabaseTest
import kotlinx.coroutines.test.runTest
import org.junit.Test

class TaskDaoTest : DatabaseTest() {

    private val dao by lazy { db.taskDao() }

    @Test
    fun upsertAndObserveTaskById() = runTest {
        val entity = TaskEntity("t1", "Title", "Desc", 1_000L, 900L, false, 0L)
        dao.upsert(entity)
        dao.observeTask("t1").test {
            assertThat(awaitItem()).isEqualTo(entity)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun observeTaskReturnsNullForUnknownId() = runTest {
        dao.observeTask("unknown").test {
            assertThat(awaitItem()).isNull()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun getTasksForDayReturnsOnlyTasksInRange() = runTest {
        dao.upsert(TaskEntity("t1", "In range",  "D", 500L,   400L,  false, 0L))
        dao.upsert(TaskEntity("t2", "Out range", "D", 2_000L, 1_900L, false, 0L))
        dao.getTasksForDay(0L, 1_000L).test {
            val items = awaitItem()
            assertThat(items).hasSize(1)
            assertThat(items.first().id).isEqualTo("t1")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun getTasksForDayIncludesTaskAtExactRangeStart() = runTest {
        dao.upsert(TaskEntity("startEdge", "At start",  "D", 0L,     0L,   false, 0L)) // exactly startMs — included
        dao.upsert(TaskEntity("justAfter", "Just after","D", 1_000L, 900L, false, 0L)) // exactly endMs — excluded
        dao.getTasksForDay(0L, 1_000L).test {
            val items = awaitItem()
            assertThat(items).hasSize(1)
            assertThat(items.first().id).isEqualTo("startEdge")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun getTasksForDayExcludesTaskAtExactRangeEnd() = runTest {
        dao.upsert(TaskEntity("endEdge",   "End edge",  "D", 999L,   899L, false, 0L)) // last ms in range — included
        dao.upsert(TaskEntity("justAfter", "Just after","D", 1_000L, 900L, false, 0L)) // exactly endMs — excluded
        dao.getTasksForDay(0L, 1_000L).test {
            val items = awaitItem()
            assertThat(items).hasSize(1)
            assertThat(items.first().id).isEqualTo("endEdge")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun deleteByIdRemovesEntity() = runTest {
        dao.upsert(TaskEntity("t1", "Title", "Desc", 1_000L, 900L, false, 0L))
        dao.deleteById("t1")
        dao.observeTask("t1").test {
            assertThat(awaitItem()).isNull()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun clearAllRemovesAllEntities() = runTest {
        dao.upsert(TaskEntity("t1", "A", "D", 100L, 90L, false, 0L))
        dao.upsert(TaskEntity("t2", "B", "D", 200L, 190L, false, 0L))
        dao.clearAll()
        assertThat(dao.getAll()).isEmpty()
    }
}
