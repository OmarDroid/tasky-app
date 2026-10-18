package com.omaroid.tasky.app

import android.app.Application
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationManagerCompat
import com.omaroid.tasky.app.di.agendaDetailModule
import com.omaroid.tasky.app.di.agendaModule
import com.omaroid.tasky.app.di.agendaPresentationModule
import com.omaroid.tasky.app.di.authModule
import com.omaroid.tasky.app.di.authPresentationModule
import com.omaroid.tasky.app.di.coreModule
import com.omaroid.tasky.app.di.databaseModule
import com.omaroid.tasky.app.di.notificationModule
import com.omaroid.tasky.app.di.preferencesModule
import com.omaroid.tasky.app.di.startupPresentationModule
import com.omaroid.tasky.app.di.syncModule
import com.omaroid.tasky.core.data.sync.SyncScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.android.ext.android.getKoin
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.workmanager.koin.workManagerFactory
import org.koin.core.context.GlobalContext.startKoin

class TaskyApp : Application() {
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        // Reminder-notifications channel (Compat builder no-ops below API 26).
        NotificationManagerCompat.from(this).createNotificationChannel(
            NotificationChannelCompat.Builder("reminders", NotificationManagerCompat.IMPORTANCE_HIGH)
                .setName("Reminders")
                .setDescription("Agenda item reminders")
                .build(),
        )
        startKoin {
            androidContext(this@TaskyApp)
            // Initializes WorkManager with a Koin-backed WorkerFactory so SyncWorker/ReminderWorker
            // (workerOf in syncModule/notificationModule) get their constructor deps; requires the
            // default WorkManagerInitializer to be removed in the manifest.
            workManagerFactory()
            modules(
                coreModule, startupPresentationModule, authPresentationModule, authModule,
                databaseModule, agendaModule, agendaPresentationModule, agendaDetailModule, syncModule,
                notificationModule, preferencesModule,
            )
        }
        // Wire the offline outbox drain: reconnect trigger + 30-min periodic full sync.
        getKoin().get<SyncScheduler>().start()
    }
}
