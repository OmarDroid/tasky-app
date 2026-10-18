package com.omaroid.tasky.feature.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.omaroid.tasky.R
import com.omaroid.tasky.core.presentation.designsystem.theme.TaskyTheme
import com.omaroid.tasky.core.presentation.designsystem.theme.extendedColors

@Composable
fun DetailScreenScaffold(
    modifier: Modifier = Modifier,
    title: String,
    typeLabel: String,
    accentColor: Color,
    // When true the type swatch is an outlined (bordered) box instead of a filled one — the
    // reminder design uses a hollow grey square, while Task/Event use filled colour swatches.
    accentOutlined: Boolean = false,
    isEditMode: Boolean,
    formattedDate: String,
    showEditControls: Boolean = true,
    titleLeadingContent: (@Composable () -> Unit)? = null,
    // When non-null, replaces the default Text+chevron in the title row.
    // The row-level clickable is also suppressed so the slot owns its own interaction.
    titleContent: (@Composable RowScope.() -> Unit)? = null,
    onClose: () -> Unit,
    onEdit: () -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    onTitleClick: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    // Background on outer column ensures rounded corners of the body Surface
    // are visible against the dark header in both light and dark mode.
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(extendedColors.headerBackground),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (isEditMode) {
                TextButton(onClick = onClose) {
                    Text(
                        stringResource(R.string.detail_cancel),
                        style = MaterialTheme.typography.labelLarge,
                        color = extendedColors.headerOnBackground,
                    )
                }
                Text(
                    text = stringResource(R.string.detail_edit_label, typeLabel),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = extendedColors.headerOnBackground,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                )
                TextButton(onClick = onSave) {
                    Text(
                        stringResource(R.string.detail_save),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = extendedColors.taskyGreen,
                    )
                }
            } else {
                IconButton(onClick = onClose) {
                    Icon(
                        Icons.Default.Close, "Close",
                        tint = extendedColors.headerOnBackground,
                    )
                }
                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = extendedColors.headerOnBackground,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                )
                if (showEditControls) {
                    IconButton(onClick = onEdit) {
                        Icon(
                            Icons.Default.Edit, "Edit",
                            tint = extendedColors.headerOnBackground,
                        )
                    }
                } else {
                    Spacer(Modifier.size(48.dp))
                }
            }
        }

        Surface(
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
            color = MaterialTheme.colorScheme.surface,
        ) {
            // Inner Column is split: scrollable content (weight=1f) + sticky DELETE at bottom.
            // weight(1f) gives the scroll area a fixed height so DELETE never scrolls away.
            Column(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(top = 24.dp, bottom = 16.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .then(
                                    if (accentOutlined)
                                        Modifier.border(2.dp, accentColor, RoundedCornerShape(4.dp))
                                    else Modifier.background(accentColor)
                                ),
                        )
                        Text(
                            text = typeLabel,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.height(20.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .then(
                                if (isEditMode && titleContent == null)
                                    Modifier.clickable(onClick = onTitleClick)
                                else Modifier
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        titleLeadingContent?.invoke()
                        if (titleContent != null) {
                            titleContent()
                        } else {
                            Text(
                                text = title.ifBlank { "Title" },
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (title.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant
                                else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f),
                            )
                            if (isEditMode) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowForwardIos, "Edit title",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(20.dp))

                    content()
                }

                HorizontalDivider()
                TextButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(vertical = 16.dp),
                ) {
                    Text(
                        stringResource(R.string.detail_delete_label, typeLabel),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

@Preview(name = "Scaffold · Edit · Light", showBackground = true, device = "spec:width=360dp,height=800dp,dpi=420")
@Composable
private fun ScaffoldEditLightPreview() {
    TaskyTheme(darkTheme = false) {
        DetailScreenScaffold(
            title = "Project X",
            typeLabel = "TASK",
            accentColor = Color.Green,
            isEditMode = true,
            formattedDate = "01 MARCH 2022",
            onClose = {}, onEdit = {}, onSave = {}, onDelete = {}, onTitleClick = {},
        ) { Text("Content") }
    }
}

@Preview(name = "Scaffold · View · Dark", showBackground = true, device = "spec:width=360dp,height=800dp,dpi=420")
@Composable
private fun ScaffoldViewDarkPreview() {
    TaskyTheme(darkTheme = true) {
        DetailScreenScaffold(
            title = "Project X",
            typeLabel = "TASK",
            accentColor = Color.Green,
            isEditMode = false,
            formattedDate = "01 MARCH 2022",
            onClose = {}, onEdit = {}, onSave = {}, onDelete = {}, onTitleClick = {},
        ) { Text("Content") }
    }
}
