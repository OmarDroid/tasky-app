package com.omaroid.tasky.feature.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun DetailDateTimeRow(
    label: String,
    dateText: String,
    timeText: String,
    isEditable: Boolean,
    modifier: Modifier = Modifier,
    onDateClick: () -> Unit,
    onTimeClick: () -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            label, style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ClickableFormChip(timeText, isEditable, showArrow = isEditable, onClick = onTimeClick)
            ClickableFormChip(dateText, isEditable, showArrow = isEditable, onClick = onDateClick)
        }
    }
}

@Preview
@Composable
private fun DetailDateTimeRowPreview() {
    DetailDateTimeRow(
        label = "Date",
        dateText = "12/12/2022",
        timeText = "12:00 PM",
        isEditable = true,
        onDateClick = {},
        onTimeClick = {}
    )
}