package com.omaroid.tasky.feature.edittext

import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isTrue
import com.omaroid.tasky.core.presentation.designsystem.theme.TaskyTheme
import org.junit.Rule
import org.junit.Test

class EditTextScreenTest {

    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun saveButton_callsOnSaveWithTypedText() {
        var savedText = ""
        composeTestRule.setContent {
            TaskyTheme {
                EditTextScreen(
                    type = EditTextType.TITLE,
                    initialValue = "",
                    onSave = { savedText = it },
                    onBack = {},
                )
            }
        }
        composeTestRule.onNode(hasSetTextAction()).performTextInput("Buy milk")
        composeTestRule.onNodeWithText("Save").performClick()
        assertThat(savedText).isEqualTo("Buy milk")
    }

    @Test
    fun cancelButton_callsOnBack() {
        var backCalled = false
        composeTestRule.setContent {
            TaskyTheme {
                EditTextScreen(
                    type = EditTextType.DESCRIPTION,
                    initialValue = "original",
                    onSave = {},
                    onBack = { backCalled = true },
                )
            }
        }
        composeTestRule.onNodeWithText("Cancel").performClick()
        assertThat(backCalled).isTrue()
    }
}
