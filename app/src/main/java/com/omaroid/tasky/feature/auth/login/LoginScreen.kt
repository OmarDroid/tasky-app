package com.omaroid.tasky.feature.auth.login

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.omaroid.tasky.R
import com.omaroid.tasky.core.presentation.designsystem.theme.TaskyTheme
import com.omaroid.tasky.core.presentation.util.ObserveAsEvents
import com.omaroid.tasky.core.presentation.util.UiText
import com.omaroid.tasky.feature.auth.components.AuthFooterLinkRow
import com.omaroid.tasky.feature.auth.components.AuthGlobalError
import com.omaroid.tasky.feature.auth.components.AuthPasswordField
import com.omaroid.tasky.feature.auth.components.AuthPrimaryButton
import com.omaroid.tasky.feature.auth.components.AuthScreenScaffold
import com.omaroid.tasky.feature.auth.components.AuthTextField
import org.koin.androidx.compose.koinViewModel

@Composable
fun LoginRoot(
    onNavigateToRegister: () -> Unit,
    onNavigateToAgenda: () -> Unit,
    viewModel: LoginViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.onAction(LoginAction.OnScreenEntered) }

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            LoginEvent.NavigateToRegister -> onNavigateToRegister()
            LoginEvent.NavigateToAgenda -> onNavigateToAgenda()
        }
    }

    LoginScreen(
        state = state,
        onAction = viewModel::onAction,
    )
}

@Composable
fun LoginScreen(
    state: LoginState,
    onAction: (LoginAction) -> Unit,
) {
    AuthScreenScaffold(
        title = UiText.StringResource(R.string.login_title),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            AuthTextField(
                value = state.email,
                onValueChange = { onAction(LoginAction.OnEmailChanged(it)) },
                placeholder = stringResource(R.string.login_email_placeholder),
                isValid = state.emailError == null && state.email.isNotBlank(),
                keyboardType = KeyboardType.Email,
            )
            AuthPasswordField(
                value = state.password,
                onValueChange = { onAction(LoginAction.OnPasswordChanged(it)) },
                placeholder = stringResource(R.string.login_password_placeholder),
                isPasswordVisible = state.isPasswordVisible,
                onPasswordVisibilityClick = { onAction(LoginAction.OnPasswordVisibilityClicked) },
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = state.rememberMe,
                    onCheckedChange = { onAction(LoginAction.OnRememberMeToggled) },
                )
                Text(
                    text = stringResource(R.string.login_remember_me),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }

        AnimatedVisibility(visible = state.globalError != null) {
            state.globalError?.let { error ->
                AuthGlobalError(message = error.asString())
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            AuthPrimaryButton(
                text = if (state.isSubmitting) stringResource(R.string.loading)
                else stringResource(R.string.login),
                enabled = state.isValid && !state.isSubmitting,
                onClick = { onAction(LoginAction.OnLoginClicked) },
            )
            AuthFooterLinkRow(
                text = stringResource(R.string.login_dont_have_account),
                linkText = stringResource(R.string.login_sign_up),
                onClick = { onAction(LoginAction.OnRegisterClicked) },
            )
        }
    }
}

@Preview(
    name = "Login - Light Portrait",
    device = "spec:width=360dp,height=800dp,dpi=420",
    showBackground = true
)
@Composable
private fun LoginScreenLightPortraitPreview() {
    TaskyTheme(darkTheme = false) {
        LoginScreen(
            state = LoginState(),
            onAction = {},
        )
    }
}

@Preview(
    name = "Login - Dark Portrait",
    device = "spec:width=360dp,height=800dp,dpi=420",
    showBackground = true
)
@Composable
private fun LoginScreenDarkPortraitPreview() {
    TaskyTheme(darkTheme = true) {
        LoginScreen(
            state = LoginState(),
            onAction = {},
        )
    }
}

@Preview(
    name = "Login - Mobile Landscape",
    device = "spec:width=800dp,height=360dp,dpi=320",
    showBackground = true
)
@Composable
private fun LoginScreenLandscapePreview() {
    TaskyTheme(darkTheme = false) {
        LoginScreen(
            state = LoginState(),
            onAction = {},
        )
    }
}

@Preview(
    name = "Login - Tablet Landscape",
    device = "spec:width=1280dp,height=800dp,dpi=240",
    showBackground = true
)
@Composable
private fun LoginScreenTabletLandscapePreview() {
    TaskyTheme(darkTheme = false) {
        LoginScreen(
            state = LoginState(),
            onAction = {},
        )
    }
}

@Preview(
    name = "Login - Tablet Portrait",
    device = "spec:width=800dp,height=1280dp,dpi=240",
    showBackground = true
)
@Composable
private fun LoginScreenTabletPortraitPreview() {
    TaskyTheme(darkTheme = false) {
        LoginScreen(
            state = LoginState(),
            onAction = {},
        )
    }
}

@Preview(
    name = "Login - Tablet Portrait Dark",
    device = "spec:width=800dp,height=1280dp,dpi=240",
    showBackground = true
)
@Composable
private fun LoginScreenTabletPortraitDarkPreview() {
    TaskyTheme(darkTheme = true) {
        LoginScreen(
            state = LoginState(),
            onAction = {},
        )
    }
}
