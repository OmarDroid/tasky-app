package com.omaroid.tasky.core.util

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.omaroid.tasky.core.data.agenda.local.TaskyDatabase
import com.omaroid.tasky.core.data.agenda.local.entities.EventConverters
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Before

abstract class DatabaseTest {
    protected lateinit var db: TaskyDatabase

    @Before
    fun createDb() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            TaskyDatabase::class.java,
        ).allowMainThreadQueries()
            .addTypeConverter(EventConverters(Json))
            .build()
    }

    @After
    fun closeDb() = db.close()
}
