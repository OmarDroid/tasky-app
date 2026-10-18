package com.omaroid.tasky.core.data.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.omaroid.tasky.core.domain.connectivity.ConnectivityMonitor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.util.concurrent.TimeUnit

/**
 * Wires the three ways the [SyncWorker] is woken up: an offline → online transition (immediate drain),
 * a 30-minute periodic full sync, and — implicitly — WorkManager's own retry backoff. Started once from
 * `TaskyApp.onCreate()`.
 */
class SyncScheduler(
    private val context: Context,
    private val connectivityMonitor: ConnectivityMonitor,
    private val applicationScope: CoroutineScope,
) {
    // A CONNECTED constraint is required — without it the worker can run offline, fail, and burn a retry.
    private val networkConstraint =
        Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

    fun start() {
        // Trigger immediately whenever connectivity is (re)gained.
        connectivityMonitor.isOnline
            .filter { it }
            .onEach {
                val request = OneTimeWorkRequestBuilder<SyncWorker>()
                    .setConstraints(networkConstraint)
                    .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                    .build()
                WorkManager.getInstance(context).enqueueUniqueWork(
                    "sync",
                    // KEEP (not REPLACE): don't cancel an in-flight drain when connectivity flaps —
                    // let the running sync finish; it already reads the latest outbox.
                    ExistingWorkPolicy.KEEP,
                    request,
                )
            }.launchIn(applicationScope)

        // Also periodic (every 30 min minimum), same network constraint.
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "fullSync",
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<SyncWorker>(30, TimeUnit.MINUTES)
                .setConstraints(networkConstraint)
                .build(),
        )
    }
}
