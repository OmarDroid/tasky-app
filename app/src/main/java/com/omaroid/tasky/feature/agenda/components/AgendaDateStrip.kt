package com.omaroid.tasky.feature.agenda.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.omaroid.tasky.core.presentation.designsystem.theme.TaskyLightOnSurface
import com.omaroid.tasky.core.presentation.designsystem.theme.TaskySupplementary
import kotlinx.datetime.LocalDate
import kotlinx.datetime.number

@Composable
fun AgendaDateStrip(
    dates: List<LocalDate>,      // 31-day window: today ± 15
    selectedDate: LocalDate,
    today: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()

    LaunchedEffect(selectedDate, dates) {
        val index = dates.indexOf(selectedDate).coerceAtLeast(0)
        listState.animateScrollToItem((index - 3).coerceAtLeast(0))
    }

    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        state = listState,
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        items(dates, key = { "${it.year}-${it.month.number}-${it.day}" }) { date ->
            DateCell(
                date = date,
                isSelected = date == selectedDate,
                isToday = date == today,
                onClick = { onDateSelected(date) },
            )
        }
    }
}

@Composable
private fun DateCell(
    date: LocalDate,
    isSelected: Boolean,
    isToday: Boolean,
    onClick: () -> Unit,
) {
    val highlighted = remember(isSelected, isToday) { isSelected || isToday }
    Column(
        modifier = Modifier
            .width(48.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(if (highlighted) TaskySupplementary else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = date.dayOfWeek.name.take(1),
            style = MaterialTheme.typography.labelSmall,
            color = if (highlighted) TaskyLightOnSurface.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
        )
        Text(
            text = date.day.toString(),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = if (highlighted) TaskyLightOnSurface else MaterialTheme.colorScheme.onBackground,
        )
    }
}