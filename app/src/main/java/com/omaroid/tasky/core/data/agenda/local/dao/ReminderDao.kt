package com.omaroid.tasky.core.data.agenda.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.omaroid.tasky.core.data.agenda.local.entities.ReminderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminders WHERE time >= :startMs AND time < :endMs ORDER BY time ASC")
    fun getRemindersForDay(startMs: Long, endMs: Long): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE id = :id")
    fun observeReminder(id: String): Flow<ReminderEntity?>

    @Query("SELECT * FROM reminders")
    suspend fun getAll(): List<ReminderEntity>

    // One-shot read for the SyncWorker (it isn't collecting a Flow) — don't observeReminder(id).first().
    @Query("SELECT * FROM reminders WHERE id = :id")
    suspend fun getById(id: String): ReminderEntity?

    @Upsert
    suspend fun upsert(reminder: ReminderEntity)

    @Query("DELETE FROM reminders WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM reminders")
    suspend fun clearAll()
}