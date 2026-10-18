package com.omaroid.tasky.app.di

import com.omaroid.tasky.core.data.sync.Outbox
import com.omaroid.tasky.core.data.sync.SyncScheduler
import com.omaroid.tasky.core.data.sync.SyncWorker
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.workmanager.dsl.workerOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val syncModule = module {
    // Outbox(dao, connectivity, json, clock) — all resolvable from core/database modules.
    singleOf(::Outbox)
    // Registers SyncWorker with Koin's WorkerFactory (installed via workManagerFactory() in TaskyApp),
    // so WorkManager can construct it with its DAO/remote/repository dependencies.
    workerOf(::SyncWorker)
    single { SyncScheduler(androidContext(), connectivityMonitor = get(), applicationScope = get()) }
}
