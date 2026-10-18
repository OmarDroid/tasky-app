package com.omaroid.tasky.core.util

import com.omaroid.tasky.core.domain.preferences.ThemePreference
import com.omaroid.tasky.core.domain.preferences.ThemePreferenceStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeThemePreferenceStorage : ThemePreferenceStorage {
    val stored = MutableStateFlow(ThemePreference.SYSTEM)

    override val theme: Flow<ThemePreference> = stored

    override suspend fun set(theme: ThemePreference) {
        stored.value = theme
    }
}
