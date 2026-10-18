package com.omaroid.tasky.app.di

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import com.omaroid.tasky.BuildConfig
import com.omaroid.tasky.app.TaskyApp
import com.omaroid.tasky.core.data.auth.TokenRefreshHandler
import com.omaroid.tasky.core.data.crypto.AndroidKeyStoreCryptography
import com.omaroid.tasky.core.data.session.DataStoreSessionLocalDataSource
import com.omaroid.tasky.core.data.util.EmailPatternValidator
import com.omaroid.tasky.core.domain.crypto.Cryptography
import com.omaroid.tasky.core.domain.session.CheckSessionValidUseCase
import com.omaroid.tasky.core.domain.session.SessionLocalDataSource
import com.omaroid.tasky.core.domain.util.PatternValidator
import com.omaroid.tasky.core.domain.validation.EmailValidator
import com.omaroid.tasky.core.domain.validation.NameValidator
import com.omaroid.tasky.core.domain.validation.PasswordValidator
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.header
import io.ktor.serialization.kotlinx.json.json
import kotlin.time.Clock
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.json.Json
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.core.qualifier.named
import org.koin.dsl.module

val coreModule = module {

    single<Clock> { Clock.System }

    single<CoroutineScope> { (androidContext() as TaskyApp).applicationScope }

    single {
        Json {
            ignoreUnknownKeys = true
        }
    }

    single {
        HttpClient(Android) {
            install(ContentNegotiation) {
                json(get<Json>())
            }
            install(Logging) {
                level = if (BuildConfig.DEBUG) LogLevel.ALL else LogLevel.NONE
            }
            defaultRequest {
                url(BuildConfig.BASE_URL)
                header("x-api-key", BuildConfig.API_KEY)
            }
        }
    }

    // Explicit form required: two HttpClient bindings exist (qualified + unqualified).
    // get() without a qualifier resolves the unauthenticated client — intentional.
    single { TokenRefreshHandler(sessionSource = get(), httpClient = get()) }

    single(qualifier = named("authenticated")) {
        val sessionSource: SessionLocalDataSource = get()
        val tokenRefresher: TokenRefreshHandler = get()
        HttpClient(Android) {
            install(ContentNegotiation) { json(get<Json>()) }
            install(Logging) { level = if (BuildConfig.DEBUG) LogLevel.ALL else LogLevel.NONE }
            install(Auth) {
                bearer {
                    loadTokens {
                        val s = sessionSource.getSession() ?: return@loadTokens null
                        BearerTokens(accessToken = s.accessToken, refreshToken = s.refreshToken)
                    }
                    refreshTokens { tokenRefresher.refresh() }
                }
            }
            defaultRequest {
                url(BuildConfig.BASE_URL)
                header("x-api-key", BuildConfig.API_KEY)
            }
        }
    }

    single {
        PreferenceDataStoreFactory.create {
            androidContext().preferencesDataStoreFile("tasky_session.preferences_pb")
        }
    }

    single<Cryptography> { AndroidKeyStoreCryptography() }

    singleOf(::DataStoreSessionLocalDataSource) {
        bind<SessionLocalDataSource>()
    }

    singleOf(::CheckSessionValidUseCase)

    single<PatternValidator> { EmailPatternValidator() }
    singleOf(::EmailValidator)
    singleOf(::NameValidator)
    singleOf(::PasswordValidator)
}