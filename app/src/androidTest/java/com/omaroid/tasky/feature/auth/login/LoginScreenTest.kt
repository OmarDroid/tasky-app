package com.omaroid.tasky.feature.auth.login

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

class LoginScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun launchScreen(
        state: LoginState = LoginState(),
        onAction: (LoginAction) -> Unit = {},
    ) {
        composeTestRule.setContent {
            TaskyTheme {
                LoginScreen(state = state, onAction = onAction)
            }
        }
    }

    @Test
    fun emailFieldIsDisplayed() {
        launchScreen()
        composeTestRule.onNodeWithText("Email address").assertIsDisplayed()
    }

    @Test
    fun passwordFieldIsDisplayed() {
        launchScreen()
        composeTestRule.onNodeWithText("Password").assertIsDisplayed()
    }

    @Test
    fun loginButtonIsDisabledWhenFormInvalid() {
        launchScreen(state = LoginState(email = "", password = ""))
        composeTestRule.onNodeWithText("LOG IN").assertIsNotEnabled()
    }

    @Test
    fun loginButtonIsEnabledWhenFormValid() {
        launchScreen(state = LoginState(email = "user@example.com", password = "Secret1!"))
        composeTestRule.onNodeWithText("LOG IN").assertIsEnabled()
    }

    @Test
    fun clickingLoginButtonFiresOnLoginClicked() {
        var fired = false
        launchScreen(
            state = LoginState(email = "user@example.com", password = "Secret1!"),
            onAction = { if (it is LoginAction.OnLoginClicked) fired = true },
        )
        composeTestRule.onNodeWithText("LOG IN").performClick()
        assertTrue(fired)
    }

    @Test
    fun clickingSignUpLinkFiresOnRegisterClicked() {
        var fired = false
        launchScreen(onAction = { if (it is LoginAction.OnRegisterClicked) fired = true })
        composeTestRule.onNodeWithText("SIGN UP").performClick()
        assertTrue(fired)
    }
}
