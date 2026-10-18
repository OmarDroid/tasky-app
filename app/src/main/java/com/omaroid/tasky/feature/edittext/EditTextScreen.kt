package com.omaroid.tasky.feature.edittext

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.omaroid.tasky.core.presentation.designsystem.theme.TaskyTheme
import com.omaroid.tasky.core.presentation.designsystem.theme.extendedColors

@Composable
fun EditTextScreen(
    type: EditTextType,
    initialValue: String,
    onSave: (String) -> Unit,
    onBack: () -> Unit,
) {
    var text by rememberSaveable { mutableStateOf(initialValue) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .statusBarsPadding()
            .imePadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onBack) {
                Text(
                    "Cancel",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
            Text(
                text = when (type) {
                    EditTextType.TITLE -> "EDIT TITLE"
                    EditTextType.DESCRIPTION -> "EDIT DESCRIPTION"
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
            )
            TextButton(onClick = { onSave(text) }, enabled = text.isNotBlank()) {
                Text(
                    "Save",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (text.isNotBlank()) {
                        extendedColors.taskyGreen
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
        }

        HorizontalDivider()

        // Auto-open keyboard when the screen is first shown
        val focusRequester = remember { FocusRequester() }
        LaunchedEffect(Unit) { focusRequester.requestFocus() }

        BasicTextField(
            value = text,
            onValueChange = { text = it },
            modifier = Modifier
                .fillMaxSize()
                .focusRequester(focusRequester)
                .padding(horizontal = 16.dp, vertical = 24.dp),
            textStyle = when (type) {
                EditTextType.TITLE -> MaterialTheme.typography.headlineSmall.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                )
                EditTextType.DESCRIPTION -> MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                )
            },
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            singleLine = type == EditTextType.TITLE,
            decorationBox = { inner ->
                if (text.isEmpty()) {
                    Text(
                        text = when (type) {
                            EditTextType.TITLE -> "Enter title"
                            EditTextType.DESCRIPTION -> "Enter description"
                        },
                        style = when (type) {
                            EditTextType.TITLE -> MaterialTheme.typography.headlineSmall
                            EditTextType.DESCRIPTION -> MaterialTheme.typography.bodyLarge
                        },
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                inner()
            },
        )
    }
}

@Preview(name = "EditText – Title · Light", device = "spec:width=360dp,height=800dp,dpi=420", showBackground = true)
@Composable
private fun EditTextTitlePreview() {
    TaskyTheme(darkTheme = false) { EditTextScreen(EditTextType.TITLE, "My event", {}, {}) }
}

@Preview(name = "EditText – Description · Dark", device = "spec:width=360dp,height=800dp,dpi=420", showBackground = true)
@Composable
private fun EditTextDescriptionDarkPreview() {
    TaskyTheme(darkTheme = true) { EditTextScreen(EditTextType.DESCRIPTION, "", {}, {}) }
}

@Preview(name = "EditText – Tablet Landscape", device = "spec:width=1280dp,height=800dp,dpi=240", showBackground = true)
@Composable
private fun EditTextTabletPreview() {
    TaskyTheme { EditTextScreen(EditTextType.TITLE, "My event title", {}, {}) }
}
