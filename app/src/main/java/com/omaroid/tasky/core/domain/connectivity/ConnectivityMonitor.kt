package com.omaroid.tasky.core.domain.connectivity

import kotlinx.coroutines.flow.StateFlow

/**
 * Minimal connectivity signal. The event repository reads [isOnline] to decide whether to attempt
 * the remote create/update/upload inline. Step 20 extends this to drive offline UI affordances.
 */
interface ConnectivityMonitor {
    val isOnline: StateFlow<Boolean>
}
