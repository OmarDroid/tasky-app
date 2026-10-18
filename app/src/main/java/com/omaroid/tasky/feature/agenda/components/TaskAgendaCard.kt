package com.omaroid.tasky.feature.agenda.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.omaroid.tasky.core.presentation.designsystem.theme.extendedColors
import com.omaroid.tasky.core.presentation.util.toAgendaCardLabel
import com.omaroid.tasky.feature.agenda.model.AgendaItemUi
import kotlinx.datetime.LocalDateTime

@Composable
fun TaskAgendaCard(
    task: AgendaItemUi.Task,
    onToggleDone: () -> Unit,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var menuVisible by rememberSaveable { mutableStateOf(false) }
    val dateTimeLabel = remember(task.displayDate, task.startTime) {
        LocalDateTime(task.displayDate, task.startTime).toAgendaCardLabel()
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = extendedColors.taskyGreen),
        onClick = onOpen,
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(if (task.isDone) Color.White else Color.Transparent)
                        .border(
                            2.dp,
                            if (task.isDone) Color.White else Color.White.copy(alpha = 0.7f),
                            CircleShape,
                        )
                        .clickable(onClick = onToggleDone),
                    contentAlignment = Alignment.Center,
                ) {
                    if (task.isDone) {
                        Icon(
                            Icons.Default.Check, null,
                            tint = extendedColors.taskyGreen,
                            modifier = Modifier.size(12.dp),
                        )
                    }
                }
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    textDecoration = if (task.isDone) TextDecoration.LineThrough else null,
                    modifier = Modifier.weight(1f),
                )
                Box {
                    IconButton(onClick = { menuVisible = true }) {
                        Icon(Icons.Default.MoreHoriz, null, tint = Color.White)
                    }
                    AgendaItemMenu(
                        expanded = menuVisible,
                        onAction = { action ->
                            menuVisible = false
                            when (action) {
                                AgendaItemMenuAction.Open -> onOpen()
                                AgendaItemMenuAction.Edit -> onEdit()
                                AgendaItemMenuAction.Delete -> onDelete()
                                AgendaItemMenuAction.Dismiss -> Unit
                            }
                        },
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            if (task.description.isNotBlank()) {
                Text(
                    text = task.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.85f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(12.dp))
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                Text(
                    text = dateTimeLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.75f),
                )
            }
        }
    }
}
