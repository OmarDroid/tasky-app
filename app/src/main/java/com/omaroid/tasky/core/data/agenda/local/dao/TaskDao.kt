package com.omaroid.tasky.core.data.agenda.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.omaroid.tasky.core.data.agenda.local.entities.TaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks WHERE time >= :startMs AND time < :endMs ORDER BY time ASC")
    fun getTasksForDay(startMs: Long, endMs: Long): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :id")
    fun observeTask(id: String): Flow<TaskEntity?>

    @Query("SELECT * FROM tasks")
    suspend fun getAll(): List<TaskEntity>

    // One-shot read for the SyncWorker (it isn't collecting a Flow) — don't observeTask(id).first().
    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getById(id: String): TaskEntity?

    @Upsert
    suspend fun upsert(task: TaskEntity)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM tasks")
    suspend fun clearAll()
}