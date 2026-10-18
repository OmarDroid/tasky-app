package com.omaroid.tasky.feature.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.omaroid.tasky.R
import com.omaroid.tasky.core.presentation.adaptive.DeviceConfiguration
import com.omaroid.tasky.core.presentation.adaptive.currentDeviceConfiguration
import com.omaroid.tasky.core.presentation.designsystem.theme.TaskyDarkBackground
import com.omaroid.tasky.core.presentation.designsystem.theme.TaskyDarkOnSurface
import com.omaroid.tasky.core.presentation.designsystem.theme.TaskyTheme
import com.omaroid.tasky.core.presentation.designsystem.theme.extendedColors
import com.omaroid.tasky.core.presentation.util.formatForAgenda
import com.omaroid.tasky.feature.agenda.YearMonth
import com.omaroid.tasky.feature.agenda.model.AgendaItemUi
import com.omaroid.tasky.feature.agenda.yearMonth
import com.omaroid.tasky.feature.detail.AgendaDetailType
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun CalendarRoot(
    onNavigateBack: (LocalDate) -> Unit, // carries the currently-selected day back to the agenda
    onNavigateToDetail: (AgendaDetailType, String) -> Unit,
    viewModel: CalendarViewModel = koinViewModel(),
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    CalendarScreen(
        state = state,
        onAction = viewModel::onAction,
        onNavigateBack = onNavigateBack,
        onNavigateToDetail = onNavigateToDetail,
        modifier = modifier,
    )
}

// Same chrome as AgendaScreen: dark edge-to-edge Scaffold, a safe-drawing-padded dark header (the
// established always-dark-header exception for the raw TaskyDark* tokens), and the content in a
// top-rounded Surface — so the calendar reads as a sibling of the main screen, not a foreign one.
// Portrait stacks grid-then-list (the 6-row grid needs vertical room); landscape is short on
// height, so it splits into a side-by-side Row instead — the same currentDeviceConfiguration()
// pattern AuthScreenScaffold already uses for its four layouts.
@Composable
private fun CalendarScreen(
    state: CalendarState,
    onAction: (CalendarAction) -> Unit,
    onNavigateBack: (LocalDate) -> Unit,
    onNavigateToDetail: (AgendaDetailType, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        containerColor = TaskyDarkBackground,
        contentWindowInsets = WindowInsets(0),
        modifier = modifier,
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            CalendarHeader(
                yearMonth = state.yearMonth,
                onPreviousMonth = { onAction(CalendarAction.OnPreviousMonth) },
                onNextMonth = { onAction(CalendarAction.OnNextMonth) },
                onTodayClick = { onAction(CalendarAction.OnTodayClicked) },
                onBack = { onNavigateBack(state.selectedDate) },
            )
            Surface(
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                color = MaterialTheme.colorScheme.background,
            ) {
                when (currentDeviceConfiguration()) {
                    DeviceConfiguration.MOBILE_LANDSCAPE,
                    DeviceConfiguration.TABLET_LANDSCAPE,
                    DeviceConfiguration.DESKTOP,
                    ->
                        CalendarLandscapeContent(state, onAction, onNavigateToDetail)
                    DeviceConfiguration.MOBILE_PORTRAIT,
                    DeviceConfiguration.TABLET_PORTRAIT,
                    ->
                        CalendarPortraitContent(state, onAction, onNavigateToDetail)
                }
            }
        }
    }
}

// Stacked: weekday row + grid on top (needs the full width for 7 columns), day list below.
@Composable
private fun CalendarPortraitContent(
    state: CalendarState,
    onAction: (CalendarAction) -> Unit,
    onNavigateToDetail: (AgendaDetailType, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize().padding(top = 16.dp)) {
        WeekdayRow(modifier = Modifier.padding(horizontal = 8.dp))
        CalendarGrid(
            cells = state.cells,
            yearMonth = state.yearMonth,
            today = state.today,
            selectedDate = state.selectedDate,
            markers = state.markers,
            onDaySelected = { onAction(CalendarAction.OnDaySelected(it)) },
            modifier = Modifier.padding(horizontal = 8.dp),
        )
        CalendarDayList(
            items = state.dayItems,
            onNavigateToDetail = onNavigateToDetail,
            modifier = Modifier
                .weight(1f)
                .navigationBarsPadding(), // edge-to-edge: don't draw under the nav bar
        )
    }
}

// Side-by-side: a short landscape height can't fit 6 grid rows AND a day list stacked below them,
// so the grid gets its own pane (fixed width, full height) and the day list gets the rest.
@Composable
private fun CalendarLandscapeContent(
    state: CalendarState,
    onAction: (CalendarAction) -> Unit,
    onNavigateToDetail: (AgendaDetailType, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(0.55f)
                .fillMaxHeight()
                .padding(top = 16.dp),
        ) {
            WeekdayRow(modifier = Modifier.padding(horizontal = 8.dp))
            CalendarGrid(
                cells = state.cells,
                yearMonth = state.yearMonth,
                today = state.today,
                selectedDate = state.selectedDate,
                markers = state.markers,
                onDaySelected = { onAction(CalendarAction.OnDaySelected(it)) },
                modifier = Modifier.padding(horizontal = 8.dp),
            )
        }
        CalendarDayList(
            items = state.dayItems,
            onNavigateToDetail = onNavigateToDetail,
            modifier = Modifier
                .weight(0.45f)
                .fillMaxHeight()
                .padding(top = 16.dp)
                .navigationBarsPadding(),
        )
    }
}

// Selected day's items — a read-only browser (tap → detail; no swipe/menu, which is why the
// agenda's menu-cards are NOT reused). key + contentType per the heterogeneous-list rule. Shared by
// both layouts so the row/click behavior lives in exactly one place.
@Composable
private fun CalendarDayList(
    items: List<AgendaItemUi>,
    onNavigateToDetail: (AgendaDetailType, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier) {
        items(items, key = { it.id }, contentType = { it.detailType }) { item ->
            CalendarItemRow(
                item = item,
                onClick = { onNavigateToDetail(item.detailType, item.id) },
            )
        }
    }
}

// Mirrors AgendaToolbar's header: dark background, statusBarsPadding, 16/12 padding, light tint.
@Composable
private fun CalendarHeader(
    yearMonth: YearMonth,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onTodayClick: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(TaskyDarkBackground)
            // safeDrawingPadding (not statusBarsPadding alone) so a landscape display cutout on the
            // side is also cleared — statusBarsPadding only covers the top status bar.
            .safeDrawingPadding()
            .padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = TaskyDarkOnSurface,
            )
        }
        TextButton(onClick = onTodayClick) {
            Text(stringResource(R.string.today), color = TaskyDarkOnSurface)
        }
        // Formatted AT THE RENDER SITE from the structured value (never a frozen label field);
        // enum name normalized per the display rule.
        val title = remember(yearMonth) {
            yearMonth.month.name.lowercase().replaceFirstChar { it.uppercase() } + " " + yearMonth.year
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = TaskyDarkOnSurface,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onPreviousMonth) {
            Icon(Icons.Default.ChevronLeft, contentDescription = "Previous month", tint = TaskyDarkOnSurface)
        }
        IconButton(onClick = onNextMonth) {
            Icon(Icons.Default.ChevronRight, contentDescription = "Next month", tint = TaskyDarkOnSurface)
        }
    }
}

@Composable
private fun WeekdayRow(modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth()) {
        DayOfWeek.entries.forEach { day ->
            Text(
                // Enum display rule: never raw .name — normalize (MONDAY → Mon).
                text = day.name.take(3).lowercase().replaceFirstChar { it.uppercase() },
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun CalendarGrid(
    cells: List<LocalDate>,
    yearMonth: YearMonth,
    today: LocalDate,
    selectedDate: LocalDate,
    markers: Map<LocalDate, Set<AgendaDetailType>>,
    onDaySelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    // 42 fixed cells — plain rows, no lazy machinery needed.
    Column(modifier = modifier.fillMaxWidth()) {
        cells.chunked(7).forEach { week ->
            Row(modifier = Modifier.fillMaxWidth()) {
                week.forEach { date ->
                    DayCell(
                        date = date,
                        inMonth = date.yearMonth == yearMonth,
                        isToday = date == today,
                        isSelected = date == selectedDate,
                        markers = markers[date].orEmpty(),
                        onClick = { onDaySelected(date) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    inMonth: Boolean,
    isToday: Boolean,
    isSelected: Boolean,
    markers: Set<AgendaDetailType>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .padding(2.dp)
            .clip(CircleShape)
            .then(
                if (isSelected) Modifier.border(1.dp, MaterialTheme.colorScheme.primary, CircleShape)
                else Modifier,
            )
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
    ) {
        Text(
            text = date.day.toString(), // day(), not the deprecated dayOfMonth
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
            color = when {
                isToday -> MaterialTheme.colorScheme.primary
                inMonth -> MaterialTheme.colorScheme.onSurface
                else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f) // dimmed
            },
        )
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            // Dot colors mirror the agenda cards, via the theme — never raw Tasky* tokens here.
            markers.take(3).forEach { type ->
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(typeColor(type)),
                )
            }
        }
    }
}

@Composable
private fun typeColor(type: AgendaDetailType) = when (type) {
    AgendaDetailType.TASK -> extendedColors.taskyGreen
    AgendaDetailType.EVENT -> extendedColors.taskyLime
    AgendaDetailType.REMINDER -> MaterialTheme.colorScheme.outline
}

@Composable
private fun CalendarItemRow(
    item: AgendaItemUi,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(typeColor(item.detailType)),
        )
        Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
            Text(item.title, style = MaterialTheme.typography.titleMedium)
            if (item.description.isNotBlank()) {
                Text(
                    item.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
        // Established render-site exception for agenda times.
        Text(
            item.startTime.formatForAgenda(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private val previewCalendarState = run {
    val today = LocalDate(2026, 5, 15)
    CalendarState(
        today = today,
        selectedDate = today,
        yearMonth = today.yearMonth,
        cells = generateCalendarCells(today.yearMonth),
        markers = mapOf(today to setOf(AgendaDetailType.TASK, AgendaDetailType.REMINDER)),
        dayItems = listOf(
            AgendaItemUi.Task("t1", "Buy groceries", "Milk, eggs", LocalTime(9, 0), today, false),
            AgendaItemUi.Reminder("r1", "Doctor appointment", "Building C", LocalTime(11, 30), today),
        ),
    )
}

@Preview(name = "Calendar · Portrait (stacked)", device = "spec:width=360dp,height=800dp,dpi=420", showBackground = true)
@Composable
private fun CalendarScreenPortraitPreview() {
    TaskyTheme {
        CalendarScreen(
            state = previewCalendarState,
            onAction = {},
            onNavigateBack = {},
            onNavigateToDetail = { _, _ -> },
        )
    }
}

@Preview(name = "Calendar · Landscape (side-by-side)", device = "spec:width=800dp,height=360dp,dpi=420", showBackground = true)
@Composable
private fun CalendarScreenLandscapePreview() {
    TaskyTheme {
        CalendarScreen(
            state = previewCalendarState,
            onAction = {},
            onNavigateBack = {},
            onNavigateToDetail = { _, _ -> },
        )
    }
}
