package com.omaroid.tasky.app.di

import com.omaroid.tasky.core.data.auth.DataStoreCredentialsStorage
import com.omaroid.tasky.core.domain.auth.CredentialsStorage
import com.omaroid.tasky.core.data.auth.AuthRemoteDataSource
import com.omaroid.tasky.core.data.auth.KtorAuthRemoteDataSource
import com.omaroid.tasky.core.data.auth.RemoteAuthRepository
import com.omaroid.tasky.core.domain.auth.AuthRepository
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val authModule = module {
    singleOf(::KtorAuthRemoteDataSource) { bind<AuthRemoteDataSource>() }
    // Remember-me credential store — reuses the DataStore + Cryptography singles from coreModule.
    singleOf(::DataStoreCredentialsStorage) { bind<CredentialsStorage>() }
    singleOf(::RemoteAuthRepository) { bind<AuthRepository>() }
}