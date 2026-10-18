package com.omaroid.tasky.feature.agenda.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.omaroid.tasky.core.presentation.designsystem.theme.extendedColors
import com.omaroid.tasky.feature.agenda.model.AgendaItemUi
import com.omaroid.tasky.feature.agenda.model.AgendaListEntry

@Composable
fun AgendaItemList(
    entries: List<AgendaListEntry>,
    onItemOpen: (AgendaItemUi) -> Unit,
    onItemEdit: (AgendaItemUi) -> Unit,
    onItemDeleteRequest: (AgendaItemUi) -> Unit,
    onTaskToggle: (AgendaItemUi.Task) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(
            items = entries,
            key = { entry ->
                when (entry) {
                    is AgendaListEntry.Item -> "item-${entry.item.id}"
                    is AgendaListEntry.Needle -> "needle"
                }
            },
            contentType = { entry ->
                when (entry) {
                    is AgendaListEntry.Item -> entry.item::class
                    is AgendaListEntry.Needle -> "needle"
                }
            },
        ) { entry ->
            when (entry) {
                is AgendaListEntry.Needle -> TimeNeedle()
                is AgendaListEntry.Item -> when (val item = entry.item) {
                    is AgendaItemUi.Event -> EventAgendaCard(
                        event = item,
                        onOpen = { onItemOpen(item) },
                        onEdit = { onItemEdit(item) },
                        onDelete = { onItemDeleteRequest(item) },
                    )
                    is AgendaItemUi.Task -> TaskAgendaCard(
                        task = item,
                        onToggleDone = { onTaskToggle(item) },
                        onOpen = { onItemOpen(item) },
                        onEdit = { onItemEdit(item) },
                        onDelete = { onItemDeleteRequest(item) },
                    )
                    is AgendaItemUi.Reminder -> ReminderAgendaCard(
                        reminder = item,
                        onOpen = { onItemOpen(item) },
                        onEdit = { onItemEdit(item) },
                        onDelete = { onItemDeleteRequest(item) },
                    )
                }
            }
        }
    }
}

@Composable
private fun TimeNeedle(modifier: Modifier = Modifier) {
    val color = extendedColors.needle
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(color),
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(2.dp)
                .background(color, shape = RoundedCornerShape(1.dp)),
        )
    }
}
