package com.omaroid.tasky.app.di

import com.omaroid.tasky.core.data.preferences.DataStoreThemePreferenceStorage
import com.omaroid.tasky.core.domain.preferences.ThemePreferenceStorage
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val preferencesModule = module {
    // Reuses the DataStore<Preferences> single from coreModule.
    singleOf(::DataStoreThemePreferenceStorage) { bind<ThemePreferenceStorage>() }
}

