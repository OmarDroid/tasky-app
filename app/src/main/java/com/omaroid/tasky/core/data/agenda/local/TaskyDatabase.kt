package com.omaroid.tasky.core.data.agenda.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.omaroid.tasky.core.data.agenda.local.dao.EventDao
import com.omaroid.tasky.core.data.agenda.local.dao.NotifiedEventDao
import com.omaroid.tasky.core.data.agenda.local.dao.PendingOperationDao
import com.omaroid.tasky.core.data.agenda.local.dao.ReminderDao
import com.omaroid.tasky.core.data.agenda.local.dao.TaskDao
import com.omaroid.tasky.core.data.agenda.local.entities.EventEntity
import com.omaroid.tasky.core.data.agenda.local.entities.NotifiedEventEntity
import com.omaroid.tasky.core.data.agenda.local.entities.PendingOperationEntity
import com.omaroid.tasky.core.data.agenda.local.entities.ReminderEntity
import com.omaroid.tasky.core.data.agenda.local.entities.TaskEntity

@Database(
    entities = [
        TaskEntity::class, ReminderEntity::class, EventEntity::class,
        NotifiedEventEntity::class, PendingOperationEntity::class,
    ],
    // v6: adds the pending_operations table (offline outbox). Destructive migration recreates.
    // v7: pending_operations.payloadJson becomes nullable (null = no payload, replacing the "" sentinel).
    // SyncEnumConverters is declared via @TypeConverters on PendingOperationEntity (no-arg, so Room
    // instantiates it); EventConverters stays provided on EventEntity at build time.
    version = 7,
    exportSchema = false,
)
abstract class TaskyDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun reminderDao(): ReminderDao
    abstract fun eventDao(): EventDao
    abstract fun notifiedEventDao(): NotifiedEventDao
    abstract fun pendingOperationDao(): PendingOperationDao
}