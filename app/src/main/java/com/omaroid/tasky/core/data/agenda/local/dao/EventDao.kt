package com.omaroid.tasky.core.data.agenda.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.omaroid.tasky.core.data.agenda.local.entities.EventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EventDao {
    @Query("SELECT * FROM events WHERE fromTime < :endMs AND toTime >= :startMs ORDER BY fromTime ASC")
    fun getEventsForDay(startMs: Long, endMs: Long): Flow<List<EventEntity>>

    @Query("SELECT * FROM events WHERE id = :id")
    fun observeEvent(id: String): Flow<EventEntity?>

    @Query("SELECT * FROM events")
    suspend fun getAll(): List<EventEntity>

    // One-shot read for the SyncWorker (it isn't collecting a Flow) — don't observeEvent(id).first().
    @Query("SELECT * FROM events WHERE id = :id")
    suspend fun getById(id: String): EventEntity?

    @Upsert
    suspend fun upsert(event: EventEntity)

    @Query("DELETE FROM events WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM events")
    suspend fun clearAll()
}