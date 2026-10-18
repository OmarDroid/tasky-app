package com.omaroid.tasky.core.data.connectivity

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import com.omaroid.tasky.core.domain.connectivity.ConnectivityMonitor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn

class AndroidConnectivityMonitor(
    context: Context,
    applicationScope: CoroutineScope,
) : ConnectivityMonitor {

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    override val isOnline: StateFlow<Boolean> =
        callbackFlow {
            val callback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) { trySend(true) }
                override fun onLost(network: Network) { trySend(currentlyOnline()) }
                override fun onUnavailable() { trySend(currentlyOnline()) }
            }
            trySend(currentlyOnline()) // seed with the current state before any callback fires
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            connectivityManager.registerNetworkCallback(request, callback)
            // Unregister when the flow is cancelled — no leaked callback.
            awaitClose { connectivityManager.unregisterNetworkCallback(callback) }
        }
            .distinctUntilChanged()
            // Eagerly (not WhileSubscribed): EventRepositoryImpl reads isOnline.value synchronously
            // without collecting, so the upstream must stay active and .value must stay current even
            // when nothing is collecting the flow.
            .stateIn(
                scope = applicationScope,
                started = SharingStarted.Eagerly,
                initialValue = currentlyOnline(),
            )

    private fun currentlyOnline(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val caps = connectivityManager.getNetworkCapabilities(network) ?: return false
        // INTERNET = the network claims it can reach the internet; VALIDATED = the OS actually
        // confirmed reachability. Requiring BOTH avoids false "online" on emulators and on
        // "connected but no internet" networks (Wi-Fi with a captive portal / dropped uplink),
        // which is what let offline-only affordances (attendees/photos) stay enabled offline.
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}
