package com.omaroid.tasky.feature.auth.register

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.AndroidUiModes.UI_MODE_NIGHT_YES
import androidx.compose.ui.tooling.preview.Devices
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
fun RegisterRoot(
    onNavigateToLogin: () -> Unit,
    onNavigateToAgenda: () -> Unit,
    viewModel: RegisterViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            RegisterEvent.NavigateToLogin -> onNavigateToLogin()
            RegisterEvent.NavigateToAgenda -> onNavigateToAgenda()
        }
    }

    RegisterScreen(
        state = state,
        onAction = viewModel::onAction,
    )
}

@Composable
fun RegisterScreen(
    state: RegisterState,
    onAction: (RegisterAction) -> Unit,
) {
    val focusManager = LocalFocusManager.current

    AuthScreenScaffold(title = UiText.StringResource(R.string.register_title)) {

        // --- Form fields ---
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            AuthTextField(
                value = state.name,
                onValueChange = { onAction(RegisterAction.OnNameChanged(it)) },
                placeholder = stringResource(R.string.register_name_placeholder),
                isValid = state.nameError == null && state.name.isNotBlank(),
                error = state.nameError?.asString(),
                forceShowError = state.hasAttemptedSubmit,
            )
            AuthTextField(
                value = state.email,
                onValueChange = { onAction(RegisterAction.OnEmailChanged(it)) },
                placeholder = stringResource(R.string.register_email_placeholder),
                isValid = state.emailError == null && state.email.isNotBlank(),
                error = state.emailError?.asString(),
                forceShowError = state.hasAttemptedSubmit,
                keyboardType = KeyboardType.Email,
            )
            AuthPasswordField(
                value = state.password,
                onValueChange = { onAction(RegisterAction.OnPasswordChanged(it)) },
                placeholder = stringResource(R.string.register_password_placeholder),
                isPasswordVisible = state.isPasswordVisible,
                onPasswordVisibilityClick = { onAction(RegisterAction.OnPasswordVisibilityClicked) },
                error = state.passwordError?.asString(),
                forceShowError = state.hasAttemptedSubmit,
                onDone = { focusManager.clearFocus() },
            )
        }

        // --- Global error (animated) ---
        AnimatedVisibility(visible = state.globalError != null) {
            state.globalError?.let { error ->
                AuthGlobalError(message = error.asString())
            }
        }

        // --- Gap before actions ---
        Spacer(modifier = Modifier.height(16.dp))

        // --- CTA + footer ---
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            AuthPrimaryButton(
                text = if (state.isSubmitting) stringResource(R.string.loading)
                else stringResource(R.string.register_get_started),
                enabled = !state.isSubmitting,
                onClick = { onAction(RegisterAction.OnRegisterClicked) },
            )
            AuthFooterLinkRow(
                text = stringResource(R.string.register_already_have_account),
                linkText = stringResource(R.string.register_log_in),
                onClick = { onAction(RegisterAction.OnLoginClicked) },
            )
        }
    }
}

@Preview(
    showBackground = true,
)
@Composable
private fun RegistrationScreenLightPreview() {
    TaskyTheme {
        RegisterScreen(
            state = RegisterState(),
            onAction = {}
        )
    }
}


@Preview(
    showBackground = true,
    uiMode = UI_MODE_NIGHT_YES,
)
@Composable
private fun RegistrationScreenDarkPreview() {
    TaskyTheme {
        RegisterScreen(
            state = RegisterState(),
            onAction = {}
        )
    }
}

@Preview(
    showBackground = true,
    device = "spec:parent=pixel_5,orientation=landscape"
)
@Composable
private fun RegistrationScreenLandscapePreview() {
    TaskyTheme {
        RegisterScreen(
            state = RegisterState(),
            onAction = {}
        )
    }
}

@Preview(
    showBackground = true,
    device = "spec:parent=pixel_tablet,orientation=portrait"
)
@Composable
private fun RegistrationScreenTabletPortraitPreview() {
    TaskyTheme {
        RegisterScreen(
            state = RegisterState(),
            onAction = {}
        )
    }
}

@Preview(
    showBackground = true,
    device = Devices.NEXUS_10
)
@Composable
private fun RegistrationScreenTabletLightPreview() {
    TaskyTheme {
        RegisterScreen(
            state = RegisterState(),
            onAction = {}
        )
    }
}

@Preview(
    showBackground = true,
    device = Devices.NEXUS_10,
    uiMode = UI_MODE_NIGHT_YES
)
@Composable
private fun RegistrationScreenTabletDarkPreview() {
    TaskyTheme {
        RegisterScreen(
            state = RegisterState(),
            onAction = {}
        )
    }
}
