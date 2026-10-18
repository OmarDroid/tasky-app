package com.omaroid.tasky.feature.agenda.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.omaroid.tasky.core.presentation.designsystem.theme.extendedColors
import com.omaroid.tasky.core.presentation.util.afterMenuExit
import com.omaroid.tasky.core.presentation.util.toAgendaCardLabel
import com.omaroid.tasky.feature.agenda.model.AgendaItemUi

sealed interface AgendaItemMenuAction {
    data object Open : AgendaItemMenuAction
    data object Edit : AgendaItemMenuAction
    data object Delete : AgendaItemMenuAction
    data object Dismiss : AgendaItemMenuAction
}

@Composable
fun AgendaItemMenu(
    expanded: Boolean,
    onAction: (AgendaItemMenuAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()

    // Close the menu first (Dismiss flips `expanded` to false, starting the exit transition),
    // then defer the real action so the menu animates away before navigation/deletion tears
    // this card out of composition. Without the delay the menu blinks out with no transition.
    fun selectThenDismiss(action: AgendaItemMenuAction) {
        onAction(AgendaItemMenuAction.Dismiss)
        scope.afterMenuExit { onAction(action) }
    }

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = { onAction(AgendaItemMenuAction.Dismiss) },
        modifier = modifier,
    ) {
        DropdownMenuItem(
            text = { Text("Open") },
            onClick = { selectThenDismiss(AgendaItemMenuAction.Open) })
        DropdownMenuItem(
            text = { Text("Edit") },
            onClick = { selectThenDismiss(AgendaItemMenuAction.Edit) })
        DropdownMenuItem(
            text = { Text("Delete", color = extendedColors.taskyError) },
            onClick = { selectThenDismiss(AgendaItemMenuAction.Delete) })
    }
}

@Composable
fun EventAgendaCard(
    event: AgendaItemUi.Event,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var menuVisible by rememberSaveable { mutableStateOf(false) }
    val dateTimeLabel = remember(event.from) { event.from.toAgendaCardLabel() }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = extendedColors.taskyLime),
        onClick = onOpen,
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Text(
                    text = event.title,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondary,
                    modifier = Modifier.weight(1f),
                )
                Box {
                    IconButton(onClick = { menuVisible = true }) {
                        Icon(Icons.Default.MoreHoriz, null, tint = MaterialTheme.colorScheme.onSecondary)
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
            if (event.description.isNotBlank()) {
                Text(
                    text = event.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.7f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(12.dp))
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
            ) {
                if (!event.isCreator) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color.Transparent,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.5f)),
                    ) {
                        Text(
                            text = "Attendee",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.7f),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        )
                    }
                } else {
                    Spacer(Modifier)
                }
                Text(
                    text = dateTimeLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.6f),
                )
            }
        }
    }
}
