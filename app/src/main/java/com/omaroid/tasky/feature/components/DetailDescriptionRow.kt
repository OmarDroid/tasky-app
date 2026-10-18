package com.omaroid.tasky.feature.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun DetailDescriptionRow(
    description: String,
    isEditable: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .then(if (isEditable) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = description.ifBlank { "Description" },
            style = MaterialTheme.typography.bodyMedium,
            color = if (description.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant
            else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        if (isEditable) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowForwardIos,
                null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DetailDescriptionRowEditPreview() {
    DetailDescriptionRow(
        description = "Weekly plan\nRole distribution",
        isEditable = true,
        onClick = {},
    )
}

@Preview(showBackground = true)
@Composable
private fun DetailDescriptionRowViewPreview() {
    DetailDescriptionRow(
        description = "Weekly plan\nRole distribution",
        isEditable = false,
        onClick = {},
    )
}
