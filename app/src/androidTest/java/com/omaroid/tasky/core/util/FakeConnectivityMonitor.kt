package com.omaroid.tasky.core.util

import com.omaroid.tasky.core.domain.connectivity.ConnectivityMonitor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class FakeConnectivityMonitor(online: Boolean = true) : ConnectivityMonitor {
    private val _isOnline = MutableStateFlow(online)
    override val isOnline: StateFlow<Boolean> = _isOnline
    fun setOnline(value: Boolean) { _isOnline.value = value }
}
