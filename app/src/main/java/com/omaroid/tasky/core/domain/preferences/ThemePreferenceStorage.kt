package com.omaroid.tasky.core.domain.preferences

import kotlinx.coroutines.flow.Flow

/** Persists the user's theme choice across app sessions (requirement: theme preference persistence). */
interface ThemePreferenceStorage {
    val theme: Flow<ThemePreference>
    suspend fun set(theme: ThemePreference)
}
