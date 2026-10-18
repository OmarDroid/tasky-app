package com.omaroid.tasky.app.di

import com.omaroid.tasky.core.data.notification.AlarmManagerReminderScheduler
import com.omaroid.tasky.core.domain.notification.ReminderScheduler
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val notificationModule = module {
    // AlarmManager (setExactAndAllowWhileIdle) — reminders must fire at an exact minute; WorkManager
    // is for deferrable work and Doze-delays it. ReminderReceiver/BootCompletedReceiver are
    // manifest-registered (system-constructed) and pull their deps via KoinComponent, not this module.
    single<ReminderScheduler> {
        AlarmManagerReminderScheduler(
            context = androidContext(),
            clock = get(),
            taskDao = get(),
            reminderDao = get(),
            eventDao = get(),
        )
    }
}
