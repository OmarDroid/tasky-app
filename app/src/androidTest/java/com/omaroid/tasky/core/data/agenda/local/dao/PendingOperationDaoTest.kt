package com.omaroid.tasky.core.data.agenda.local.dao

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.hasSize
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import com.omaroid.tasky.core.data.agenda.local.entities.PendingOperationEntity
import com.omaroid.tasky.core.data.sync.SyncIntent
import com.omaroid.tasky.core.data.sync.SyncItemType
import com.omaroid.tasky.core.util.DatabaseTest
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class PendingOperationDaoTest : DatabaseTest() {

    private lateinit var dao: PendingOperationDao

    @Before
    fun setup() {
        dao = db.pendingOperationDao()
    }

    @Test
    fun upsert_andGetAll_returnsInsertedOperations() = runTest {
        val op = PendingOperationEntity(
            id = "TASK:task1", itemType = SyncItemType.TASK, itemId = "task1",
            intent = SyncIntent.CREATE, createdAt = 1000L,
        )
        dao.upsert(op)
        assertThat(dao.getAll()).containsExactly(op)
    }

    @Test
    fun upsert_replacesByIdentityKey() = runTest {
        // Same item edited twice offline → still ONE row (the outbox is keyed by identity).
        dao.upsert(PendingOperationEntity("TASK:t1", SyncItemType.TASK, "t1", SyncIntent.CREATE, createdAt = 1000L))
        dao.upsert(PendingOperationEntity("TASK:t1", SyncItemType.TASK, "t1", SyncIntent.UPDATE, createdAt = 1000L))
        val all = dao.getAll()
        assertThat(all).hasSize(1)
        assertThat(all.single().intent).isEqualTo(SyncIntent.UPDATE)
    }

    @Test
    fun delete_removesOperationById() = runTest {
        val op = PendingOperationEntity(
            id = "REMINDER:rem1", itemType = SyncItemType.REMINDER, itemId = "rem1",
            intent = SyncIntent.DELETE, createdAt = 2000L,
        )
        dao.upsert(op)
        dao.delete("REMINDER:rem1")
        assertThat(dao.getAll()).isEmpty()
    }

    @Test
    fun getAll_returnsOperationsInCreatedAtOrder() = runTest {
        val ops = (1..3).map { i ->
            PendingOperationEntity(
                id = "TASK:task$i", itemType = SyncItemType.TASK, itemId = "task$i",
                intent = SyncIntent.CREATE, createdAt = i * 1000L,
            )
        }
        ops.forEach { dao.upsert(it) }
        val result = dao.getAll().sortedBy { it.createdAt }
        assertThat(result.map { it.id }).containsExactly("TASK:task1", "TASK:task2", "TASK:task3")
    }
}
