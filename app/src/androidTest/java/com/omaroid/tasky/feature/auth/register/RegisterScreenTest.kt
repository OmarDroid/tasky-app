package com.omaroid.tasky.feature.auth.register

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.omaroid.tasky.core.presentation.designsystem.theme.TaskyTheme
import junit.framework.TestCase.assertTrue
import org.junit.Rule
import org.junit.Test

class RegisterScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun launchScreen(
        state: RegisterState = RegisterState(),
        onAction: (RegisterAction) -> Unit = {},
    ) {
        composeTestRule.setContent {
            TaskyTheme {
                RegisterScreen(state = state, onAction = onAction)
            }
        }
    }

    @Test
    fun nameFieldIsDisplayed() {
        launchScreen()
        composeTestRule.onNodeWithText("Name").assertIsDisplayed()
    }

    @Test
    fun emailFieldIsDisplayed() {
        launchScreen()
        composeTestRule.onNodeWithText("Email").assertIsDisplayed()
    }

    @Test
    fun passwordFieldIsDisplayed() {
        launchScreen()
        composeTestRule.onNodeWithText("Password").assertIsDisplayed()
    }

    @Test
    fun getStartedButtonDisabledWhenFormInvalid() {
        launchScreen(state = RegisterState())
        composeTestRule.onNodeWithText("GET STARTED").assertIsNotEnabled()
    }

    @Test
    fun getStartedButtonEnabledWhenFormValid() {
        val validState = RegisterState(
            name = "Alice",
            email = "alice@example.com",
            password = "Password1!",
        )
        launchScreen(state = validState)
        composeTestRule.onNodeWithText("GET STARTED").assertIsEnabled()
    }

    @Test
    fun clickingGetStartedFiresOnRegisterClicked() {
        var fired = false
        val validState = RegisterState(
            name = "Alice",
            email = "alice@example.com",
            password = "Password1!",
        )
        launchScreen(
            state = validState,
            onAction = { if (it is RegisterAction.OnRegisterClicked) fired = true },
        )
        composeTestRule.onNodeWithText("GET STARTED").performClick()
        assertTrue(fired)
    }

    @Test
    fun clickingLogInLinkFiresOnLoginClicked() {
        var fired = false
        launchScreen(onAction = { if (it is RegisterAction.OnLoginClicked) fired = true })
        composeTestRule.onNodeWithText("LOG IN").performClick()
        assertTrue(fired)
    }
}
