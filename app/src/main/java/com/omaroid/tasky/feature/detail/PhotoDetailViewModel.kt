package com.omaroid.tasky.feature.detail

import androidx.lifecycle.ViewModel
import com.omaroid.tasky.core.domain.connectivity.ConnectivityMonitor
import kotlinx.coroutines.flow.StateFlow

/**
 * State holder for the photo-detail screen. Exists so the screen gets its connectivity through a
 * ViewModel like every other screen (consistency + survives config changes), instead of the nav
 * graph injecting ConnectivityMonitor directly with koinInject.
 */
class PhotoDetailViewModel(
    connectivityMonitor: ConnectivityMonitor,
) : ViewModel() {
    val isOnline: StateFlow<Boolean> = connectivityMonitor.isOnline
}
