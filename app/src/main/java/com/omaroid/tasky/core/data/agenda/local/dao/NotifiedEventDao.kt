package com.omaroid.tasky.core.data.agenda.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.omaroid.tasky.core.data.agenda.local.entities.NotifiedEventEntity

@Dao
interface NotifiedEventDao {
    @Query("SELECT eventId FROM notified_event_ids")
    suspend fun getAllIds(): List<String>

    @Upsert
    suspend fun upsert(entity: NotifiedEventEntity)

    /** Keep only the ids still present in the current agenda (prunes left/removed events). */
    @Query("DELETE FROM notified_event_ids WHERE eventId NOT IN (:currentIds)")
    suspend fun retainOnly(currentIds: List<String>)

    @Query("DELETE FROM notified_event_ids")
    suspend fun clearAll()
}
