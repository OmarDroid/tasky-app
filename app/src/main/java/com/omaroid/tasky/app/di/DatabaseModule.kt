package com.omaroid.tasky.app.di

import androidx.room.Room
import com.omaroid.tasky.core.data.agenda.local.TaskyDatabase
import com.omaroid.tasky.core.data.agenda.local.entities.EventConverters
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val databaseModule = module {
    single { EventConverters(get()) }
    single {
        Room.databaseBuilder(
                androidContext(),
                TaskyDatabase::class.java,
                "tasky.db",
            ).fallbackToDestructiveMigration(false)
            .addTypeConverter(get<EventConverters>())
            .build()
    }
    single { get<TaskyDatabase>().taskDao() }
    single { get<TaskyDatabase>().reminderDao() }
    single { get<TaskyDatabase>().eventDao() }
    single { get<TaskyDatabase>().notifiedEventDao() }
    single { get<TaskyDatabase>().pendingOperationDao() }
}
