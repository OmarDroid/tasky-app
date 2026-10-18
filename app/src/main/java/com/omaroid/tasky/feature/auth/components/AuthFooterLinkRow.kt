package com.omaroid.tasky.feature.auth.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.omaroid.tasky.core.presentation.designsystem.theme.AuthFooterTextStyle
import com.omaroid.tasky.core.presentation.designsystem.theme.TaskyLink

@Composable
fun AuthFooterLinkRow(
    text: String,
    linkText: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            style = AuthFooterTextStyle,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = " $linkText",
            style = AuthFooterTextStyle,
            color = TaskyLink,
            modifier = Modifier.clickable(onClick = onClick),
        )
    }
}
