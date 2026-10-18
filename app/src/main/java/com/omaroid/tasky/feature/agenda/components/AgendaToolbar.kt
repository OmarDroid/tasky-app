package com.omaroid.tasky.feature.agenda.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.omaroid.tasky.core.presentation.designsystem.theme.TaskyDarkBackground
import com.omaroid.tasky.core.presentation.designsystem.theme.TaskyDarkOnSurface
import com.omaroid.tasky.core.presentation.designsystem.theme.TaskyDarkSurfaceVariant
import com.omaroid.tasky.core.domain.preferences.ThemePreference
import com.omaroid.tasky.core.presentation.designsystem.theme.TaskyError
import com.omaroid.tasky.core.presentation.designsystem.theme.TaskyTheme
import com.omaroid.tasky.core.presentation.util.afterMenuExit
import com.omaroid.tasky.feature.agenda.YearMonth
import kotlinx.datetime.Month

@Composable
fun AgendaToolbar(
    displayedMonth: YearMonth,
    userInitials: String,
    isProfileMenuVisible: Boolean,
    isOnline: Boolean,
    theme: ThemePreference,
    onMonthClick: () -> Unit,
    onCalendarClick: () -> Unit,
    onProfileClick: () -> Unit,
    onProfileMenuDismiss: () -> Unit,
    onThemeToggle: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(TaskyDarkBackground)
            // safeDrawingPadding (not statusBarsPadding alone) so a landscape display cutout on the
            // side is also cleared — statusBarsPadding only covers the top status bar.
            .safeDrawingPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.clickable(onClick = onMonthClick),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = displayedMonth.month.name.lowercase().replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = TaskyDarkOnSurface,
            )
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = "Open date picker",
                tint = TaskyDarkOnSurface,
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Offline indicator — requirement: "Offline mode shows offline icon".
            if (!isOnline) {
                Icon(
                    imageVector = Icons.Default.CloudOff,
                    contentDescription = "Offline",
                    tint = MaterialTheme.colorScheme.error,
                )
            }

            // Calendar icon opens the month calendar view (25.2); the month text opens the picker.
            IconButton(onClick = onCalendarClick) {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = "Open calendar",
                    tint = TaskyDarkOnSurface,
                )
            }

            // Theme toggle — one icon showing the CURRENT mode (sun = light, moon = dark,
            // "A" = follow system); tapping cycles System → Light → Dark.
            IconButton(onClick = onThemeToggle) {
                Icon(
                    imageVector = when (theme) {
                        ThemePreference.SYSTEM -> Icons.Default.BrightnessAuto
                        ThemePreference.LIGHT -> Icons.Default.LightMode
                        ThemePreference.DARK -> Icons.Default.DarkMode
                    },
                    contentDescription = "Toggle theme",
                    tint = TaskyDarkOnSurface,
                )
            }

            Box {
                Surface(
                    onClick = onProfileClick,
                    modifier = Modifier.size(34.dp),
                    shape = CircleShape,
                    color = TaskyDarkSurfaceVariant,
                    contentColor = TaskyDarkOnSurface,
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = userInitials,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
                DropdownMenu(
                    expanded = isProfileMenuVisible,
                    onDismissRequest = onProfileMenuDismiss,
                ) {
                    DropdownMenuItem(
                        text = { Text("Log out", color = TaskyError) },
                        // Close the menu first, then defer logout so the menu's exit transition
                        // plays before navigation to the login screen tears this down.
                        onClick = {
                            onProfileMenuDismiss()
                            scope.afterMenuExit(onLogout)
                        },
                        leadingIcon = {
                            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = TaskyError)
                        },
                    )
                }
            }
        }
    }
}

@Preview(name = "Toolbar · Online", showBackground = true)
@Composable
private fun AgendaToolbarPreview() {
    TaskyTheme {
        AgendaToolbar(
            displayedMonth = YearMonth(2026, Month.MAY),
            userInitials = "AB",
            isProfileMenuVisible = false,
            isOnline = true,
            theme = ThemePreference.SYSTEM,
            onMonthClick = {},
            onCalendarClick = {},
            onProfileClick = {},
            onProfileMenuDismiss = {},
            onThemeToggle = {},
            onLogout = {},
        )
    }
}

@Preview(name = "Toolbar · Offline + profile menu open", showBackground = true)
@Composable
private fun AgendaToolbarOfflinePreview() {
    TaskyTheme {
        AgendaToolbar(
            displayedMonth = YearMonth(2026, Month.MAY),
            userInitials = "AB",
            isProfileMenuVisible = true,
            isOnline = false,
            theme = ThemePreference.DARK,
            onMonthClick = {},
            onCalendarClick = {},
            onProfileClick = {},
            onProfileMenuDismiss = {},
            onThemeToggle = {},
            onLogout = {},
        )
    }
}
