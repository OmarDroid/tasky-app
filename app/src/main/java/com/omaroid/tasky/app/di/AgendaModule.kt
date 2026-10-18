package com.omaroid.tasky.app.di

import com.omaroid.tasky.core.data.agenda.AgendaSyncRepositoryImpl
import com.omaroid.tasky.core.data.agenda.AttendeeRepositoryImpl
import com.omaroid.tasky.core.data.agenda.EventRepositoryImpl
import com.omaroid.tasky.core.data.agenda.ReminderRepositoryImpl
import com.omaroid.tasky.core.data.agenda.TaskRepositoryImpl
import com.omaroid.tasky.core.data.agenda.remote.AgendaRemoteDataSource
import com.omaroid.tasky.core.data.agenda.remote.AttendeeRemoteDataSource
import com.omaroid.tasky.core.data.agenda.remote.EventRemoteDataSource
import com.omaroid.tasky.core.data.agenda.remote.KtorAgendaRemoteDataSource
import com.omaroid.tasky.core.data.agenda.remote.KtorAttendeeRemoteDataSource
import com.omaroid.tasky.core.data.agenda.remote.KtorEventRemoteDataSource
import com.omaroid.tasky.core.data.agenda.remote.KtorReminderRemoteDataSource
import com.omaroid.tasky.core.data.agenda.remote.KtorTaskRemoteDataSource
import com.omaroid.tasky.core.data.agenda.remote.ReminderRemoteDataSource
import com.omaroid.tasky.core.data.agenda.remote.TaskRemoteDataSource
import com.omaroid.tasky.core.data.connectivity.AndroidConnectivityMonitor
import com.omaroid.tasky.core.data.sync.EventSyncer
import com.omaroid.tasky.core.data.notification.AndroidAgendaNotifier
import com.omaroid.tasky.core.data.photo.AndroidLocalPhotoStorage
import com.omaroid.tasky.core.data.photo.AndroidPhotoCompressor
import com.omaroid.tasky.core.domain.agenda.repository.AgendaSyncRepository
import com.omaroid.tasky.core.domain.agenda.repository.AttendeeRepository
import com.omaroid.tasky.core.domain.agenda.repository.EventRepository
import com.omaroid.tasky.core.domain.agenda.repository.ReminderRepository
import com.omaroid.tasky.core.domain.agenda.repository.TaskRepository
import com.omaroid.tasky.core.domain.connectivity.ConnectivityMonitor
import com.omaroid.tasky.core.domain.notification.AgendaNotifier
import com.omaroid.tasky.core.domain.photo.LocalPhotoStorage
import com.omaroid.tasky.core.domain.photo.PhotoCompressor
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.core.qualifier.named
import org.koin.dsl.module


val agendaModule = module {
    single<AgendaRemoteDataSource> {
        KtorAgendaRemoteDataSource(httpClient = get(named("authenticated")))
    }
    single<TaskRemoteDataSource> {
        KtorTaskRemoteDataSource(httpClient = get(named("authenticated")))
    }
    single<ReminderRemoteDataSource> {
        KtorReminderRemoteDataSource(httpClient = get(named("authenticated")))
    }
    single<EventRemoteDataSource> {
        // uploadClient = the unqualified (no-bearer) client — pre-signed S3 urls reject a
        // conflicting Authorization header.
        KtorEventRemoteDataSource(httpClient = get(named("authenticated")), uploadClient = get())
    }
    single<AttendeeRemoteDataSource> {
        KtorAttendeeRemoteDataSource(httpClient = get(named("authenticated")))
    }
    single<ConnectivityMonitor> { AndroidConnectivityMonitor(androidContext(), applicationScope = get()) }
    single<PhotoCompressor> { AndroidPhotoCompressor(androidContext()) }
    single<LocalPhotoStorage> { AndroidLocalPhotoStorage(androidContext()) }
    single<AgendaNotifier> { AndroidAgendaNotifier(androidContext()) }
    singleOf(::AgendaSyncRepositoryImpl) { bind<AgendaSyncRepository>() }
    singleOf(::AttendeeRepositoryImpl) { bind<AttendeeRepository>() }
    // Bind BOTH interfaces to the same instance so the worker's EventSyncer and the ViewModel's
    // EventRepository share one object (and thus the same two-phase upload logic).
    singleOf(::EventRepositoryImpl) { bind<EventRepository>(); bind<EventSyncer>() }
    singleOf(::TaskRepositoryImpl) { bind<TaskRepository>() }
    singleOf(::ReminderRepositoryImpl) { bind<ReminderRepository>() }
}
