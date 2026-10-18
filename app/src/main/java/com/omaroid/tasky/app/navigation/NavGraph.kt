package com.omaroid.tasky.app.navigation

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navigation
import androidx.navigation.toRoute
import com.omaroid.tasky.feature.agenda.AgendaRoot
import com.omaroid.tasky.feature.calendar.CalendarRoot
import kotlinx.datetime.LocalDate
import com.omaroid.tasky.feature.auth.login.LoginRoot
import com.omaroid.tasky.feature.auth.register.RegisterRoot
import com.omaroid.tasky.feature.detail.AgendaDetailAction
import com.omaroid.tasky.feature.detail.AgendaDetailRoot
import com.omaroid.tasky.feature.detail.AgendaDetailType
import com.omaroid.tasky.feature.detail.AgendaDetailViewModel
import com.omaroid.tasky.feature.detail.PhotoDetailScreen
import com.omaroid.tasky.feature.detail.PhotoDetailViewModel
import com.omaroid.tasky.feature.edittext.EditTextScreen
import com.omaroid.tasky.feature.edittext.EditTextType
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AppNavGraph(
    startDestination: Any = AuthGraphRoute,
    navController: NavHostController = rememberNavController(),
    deepLink: AgendaDetailRoute? = null, // pre-resolved by DeepLinkResolver; null = nothing pending
    onDeepLinkConsumed: () -> Unit = {},
) {
    // Navigation is driven from ONE place, off an already-parsed destination (DeepLinkResolver owns
    // the extras contract). The link is CONSUMED after navigating — MainActivity clears its pending
    // state and strips the intent extras — so a rotation or process-death restore can't replay it
    // and stack a duplicate detail screen. launchSingleTop is the backstop if it somehow does.
    LaunchedEffect(deepLink) {
        if (deepLink != null) {
            navController.navigate(deepLink) { launchSingleTop = true }
            onDeepLinkConsumed()
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
    ) {
        navigation<AuthGraphRoute>(startDestination = LoginRoute) {
            composable<LoginRoute> {
                LoginRoot(
                    onNavigateToRegister = { navController.navigate(RegisterRoute) },
                    onNavigateToAgenda = {
                        navController.navigate(AgendaRoute) {
                            popUpTo<AuthGraphRoute> { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                )
            }
            composable<RegisterRoute> {
                RegisterRoot(
                    onNavigateToLogin = { navController.popBackStack() },
                    onNavigateToAgenda = {
                        navController.navigate(AgendaRoute) {
                            popUpTo<AuthGraphRoute> { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                )
            }
        }
        composable<AgendaRoute> { backStackEntry ->
            // Returned by the Calendar screen's back button (see composable<CalendarRoute> below).
            // -1L sentinel = "no jump pending" — mirrors the "" sentinel already used for
            // editedTitle/editedDescription/deletedPhotoKey below (SavedStateHandle's Bundle-backed
            // getStateFlow doesn't take a nullable-generic default cleanly).
            val jumpedDateEpochDay by backStackEntry.savedStateHandle
                .getStateFlow("jumpedDateEpochDay", -1L)
                .collectAsStateWithLifecycle()
            AgendaRoot(
                onNavigateToLogin = {
                    navController.navigate(LoginRoute) {
                        popUpTo(AgendaRoute) { inclusive = true }
                    }
                },
                onNavigateToCalendar = { date ->
                    navController.navigate(CalendarRoute(date.toEpochDays().toLong()))
                },
                jumpedDate = jumpedDateEpochDay.takeIf { it >= 0 }?.let { LocalDate.fromEpochDays(it.toInt()) },
                onNavigateToTask = { id, editing, date ->
                    navController.navigate(
                        AgendaDetailRoute(
                            type = AgendaDetailType.TASK, itemId = id, isEditing = editing,
                            initialDateEpochDay = date.toEpochDays().toLong(),
                        )
                    )
                },
                onNavigateToReminder = { id, editing, date ->
                    navController.navigate(
                        AgendaDetailRoute(
                            type = AgendaDetailType.REMINDER, itemId = id, isEditing = editing,
                            initialDateEpochDay = date.toEpochDays().toLong(),
                        )
                    )
                },
                onNavigateToEvent = { id, editing, date ->
                    navController.navigate(
                        AgendaDetailRoute(
                            type = AgendaDetailType.EVENT, itemId = id, isEditing = editing,
                            initialDateEpochDay = date.toEpochDays().toLong(),
                        )
                    )
                },
            )
        }
        // One destination for Task, Reminder, and Event. The route's `type` arg is read by the
        // single AgendaDetailViewModel from its SavedStateHandle. Event-only wiring (the photo
        // back-result, the photo picker, and onNavigateToPhotoDetail) is inert for Task/Reminder.
        composable<AgendaDetailRoute> { backStackEntry ->
            val editedTitle by backStackEntry.savedStateHandle
                .getStateFlow("editedTitle", "")
                .collectAsStateWithLifecycle()
            val editedDescription by backStackEntry.savedStateHandle
                .getStateFlow("editedDescription", "")
                .collectAsStateWithLifecycle()
            val deletedPhotoKey by backStackEntry.savedStateHandle
                .getStateFlow("deletedPhotoKey", "")
                .collectAsStateWithLifecycle()

            val viewModel: AgendaDetailViewModel = koinViewModel()

            LaunchedEffect(editedTitle) {
                if (editedTitle.isNotEmpty()) viewModel.onAction(AgendaDetailAction.OnTitleEdited(editedTitle))
            }
            LaunchedEffect(editedDescription) {
                if (editedDescription.isNotEmpty()) viewModel.onAction(AgendaDetailAction.OnDescriptionEdited(editedDescription))
            }
            LaunchedEffect(deletedPhotoKey) {
                if (deletedPhotoKey.isNotEmpty()) viewModel.onAction(AgendaDetailAction.OnDeletedPhotoKeyReceived(deletedPhotoKey))
            }

            val photoPickerLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.GetContent(),
            ) { uri ->
                uri?.toString()?.let { viewModel.onAction(AgendaDetailAction.OnLocalPhotoAdded(it)) }
            }

            AgendaDetailRoot(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToEditTitle = { title -> navController.navigate(EditTextRoute(EditTextType.TITLE, title)) },
                onNavigateToEditDescription = { desc -> navController.navigate(EditTextRoute(EditTextType.DESCRIPTION, desc)) },
                onNavigateToPhotoDetail = { key, url, eventId, isCreator ->
                    navController.navigate(PhotoDetailRoute(key, url, eventId, isCreator))
                },
                onOpenPhotoPicker = { photoPickerLauncher.launch("image/*") },
                viewModel = viewModel,
            )
        }
        composable<CalendarRoute> {
            CalendarRoot(
                onNavigateBack = { pickedDate ->
                    // Carry the currently-selected day back to the agenda (replaces the old
                    // DatePickerDialog jump — see 25.1's guidance rewrite).
                    navController.previousBackStackEntry?.savedStateHandle
                        ?.set("jumpedDateEpochDay", pickedDate.toEpochDays().toLong())
                    navController.popBackStack()
                },
                onNavigateToDetail = { type, id ->
                    navController.navigate(AgendaDetailRoute(type = type, itemId = id, isEditing = false))
                },
            )
        }

        composable<EditTextRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<EditTextRoute>()
            EditTextScreen(
                type = route.type,
                initialValue = route.initialText,
                onSave = { newText ->
                    val previousEntry = navController.previousBackStackEntry
                    previousEntry?.savedStateHandle?.set(
                        if (route.type == EditTextType.TITLE) "editedTitle" else "editedDescription",
                        newText,
                    )
                    navController.popBackStack()
                },
                onBack = { navController.popBackStack() },
            )
        }
        composable<PhotoDetailRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<PhotoDetailRoute>()
            // Live connectivity via the screen's own ViewModel (state holder), consistent with
            // every other screen: offline swaps the delete button for the "Unavailable offline" badge.
            val viewModel: PhotoDetailViewModel = koinViewModel()
            val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()
            PhotoDetailScreen(
                photoUrl = route.photoUrl,
                isOnline = isOnline,
                isUserCreator = route.isUserCreator,
                onClose = { navController.popBackStack() },
                onDelete = {
                    // Return the deleted photo's key (remote key or local uri) to the event screen.
                    navController.previousBackStackEntry?.savedStateHandle?.set("deletedPhotoKey", route.photoKey)
                    navController.popBackStack()
                },
            )
        }
    }
}
