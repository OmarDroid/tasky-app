package com.omaroid.tasky.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.omaroid.tasky.app.navigation.AgendaDetailRoute
import com.omaroid.tasky.app.navigation.AgendaRoute
import com.omaroid.tasky.app.navigation.DeepLinkResolver
import com.omaroid.tasky.app.navigation.AppNavGraph
import com.omaroid.tasky.app.navigation.AuthGraphRoute
import com.omaroid.tasky.core.domain.preferences.ThemePreference
import com.omaroid.tasky.core.domain.preferences.ThemePreferenceStorage
import com.omaroid.tasky.core.presentation.designsystem.theme.TaskyTheme
import com.omaroid.tasky.app.startup.StartupAction
import com.omaroid.tasky.app.startup.StartupDestination
import com.omaroid.tasky.app.startup.StartupViewModel
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : ComponentActivity() {

    private val startupViewModel: StartupViewModel by viewModel()
    private val themePreferenceStorage: ThemePreferenceStorage by inject()

    private val deepLinkResolver = DeepLinkResolver()

    // Consumed-once deep link: set from the launch intent (cold tap) and onNewIntent (warm tap),
    // cleared by onDeepLinkConsumed after navigation so restores can't replay it.
    private var pendingDeepLink by mutableStateOf<AgendaDetailRoute?>(null)

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent) // keep getIntent() consistent with the latest delivery
        deepLinkResolver.resolve(intent)?.let { pendingDeepLink = it }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        startupViewModel.onAction(StartupAction.OnAppLaunched)
        pendingDeepLink = deepLinkResolver.resolve(intent)


        splashScreen.setKeepOnScreenCondition {
            startupViewModel.state.value.isLoading
        }

        enableEdgeToEdge()
        setContent {
            // Persisted theme choice; SYSTEM follows the OS setting (the requirement's default).
            val themePreference by themePreferenceStorage.theme
                .collectAsStateWithLifecycle(initialValue = ThemePreference.SYSTEM)
            TaskyTheme(
                darkTheme = when (themePreference) {
                    ThemePreference.SYSTEM -> isSystemInDarkTheme()
                    ThemePreference.LIGHT -> false
                    ThemePreference.DARK -> true
                },
            ) {
                val state by startupViewModel.state.collectAsStateWithLifecycle()

                if (!state.isLoading) {
                    val startDestination = when (state.destination) {
                        StartupDestination.Login -> AuthGraphRoute
                        StartupDestination.Agenda -> AgendaRoute
                    }
                    AppNavGraph(
                        startDestination = startDestination,
                        // Forward only when authenticated — a logged-out tap lands on LoginScreen
                        // and the link is dropped (notifications are user-specific).
                        deepLink = pendingDeepLink.takeIf { state.destination == StartupDestination.Agenda },
                        onDeepLinkConsumed = {
                            pendingDeepLink = null
                            // Strip the extras so a restore that re-reads the intent can't replay it.
                            intent?.removeExtra("itemId")
                            intent?.removeExtra("itemType")
                        },
                    )
                }
            }
        }
    }
}