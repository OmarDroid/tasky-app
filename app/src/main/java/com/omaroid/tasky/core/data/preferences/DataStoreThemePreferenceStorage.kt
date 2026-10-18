package com.omaroid.tasky.core.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.omaroid.tasky.core.domain.preferences.ThemePreference
import com.omaroid.tasky.core.domain.preferences.ThemePreferenceStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

/**
 * Reuses the app's existing [DataStore] (the session file) under its own key — safe because
 * `clearSession()` removes only the session key, so the theme survives logout.
 */
class DataStoreThemePreferenceStorage(
    private val dataStore: DataStore<Preferences>,
) : ThemePreferenceStorage {

    // DataStore.data throws IOException on a read failure (corrupt file, disk error) instead of
    // just emitting stale/empty data — catch it here and fall back to empty preferences so a read
    // glitch degrades to the SYSTEM default rather than crashing the collector.
    override val theme: Flow<ThemePreference> = dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { preferences ->
            preferences[THEME_KEY]
                ?.let { stored -> ThemePreference.entries.firstOrNull { it.name == stored } }
                ?: ThemePreference.SYSTEM
        }

    override suspend fun set(theme: ThemePreference) {
        dataStore.edit { preferences -> preferences[THEME_KEY] = theme.name }
    }

    private companion object {
        val THEME_KEY = stringPreferencesKey("theme_preference")
    }
}
