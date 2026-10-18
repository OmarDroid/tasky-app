package com.omaroid.tasky.feature.auth.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.omaroid.tasky.core.presentation.designsystem.theme.AuthInputTextStyle
import com.omaroid.tasky.core.presentation.designsystem.theme.TaskyGreen

@Composable
fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    isValid: Boolean = false,
    error: String? = null,
    forceShowError: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    AuthBaseTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = placeholder,
        isValid = isValid,
        error = error,
        forceShowError = forceShowError,
        keyboardType = keyboardType,
        modifier = modifier,
    )
}

@Composable
fun AuthPasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    isPasswordVisible: Boolean,
    onPasswordVisibilityClick: () -> Unit,
    modifier: Modifier = Modifier,
    error: String? = null,
    forceShowError: Boolean = false,
    onDone: (() -> Unit)? = null,
) {
    AuthBaseTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = placeholder,
        isPasswordField = true,
        isPasswordVisible = isPasswordVisible,
        onPasswordVisibilityClick = onPasswordVisibilityClick,
        keyboardType = KeyboardType.Password,
        imeAction = if (onDone != null) ImeAction.Done else ImeAction.Default,
        onDone = onDone,
        error = error,
        forceShowError = forceShowError,
        modifier = modifier,
    )
}

@Composable
private fun AuthBaseTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    isValid: Boolean = false,
    error: String? = null,
    forceShowError: Boolean = false,
    isPasswordField: Boolean = false,
    isPasswordVisible: Boolean = false,
    onPasswordVisibilityClick: () -> Unit = {},
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Default,
    onDone: (() -> Unit)? = null,
) {
    val inputColor = MaterialTheme.colorScheme.onSurfaceVariant
    var isFocused by remember { mutableStateOf(false) }
    var isTouched by remember { mutableStateOf(false) }
    val showError = (isTouched || forceShowError) && error != null

    Column(modifier = modifier) {
        TextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = AuthInputTextStyle.copy(color = inputColor),
            keyboardOptions = KeyboardOptions(
                keyboardType = keyboardType,
                imeAction = imeAction,
            ),
            keyboardActions = KeyboardActions(
                onDone = { onDone?.invoke() },
            ),
            visualTransformation = if (isPasswordField && !isPasswordVisible)
                PasswordVisualTransformation() else VisualTransformation.None,
            trailingIcon = when {
                isPasswordField -> {
                    {
                        IconButton(onClick = onPasswordVisibilityClick) {
                            Icon(
                                imageVector = if (isPasswordVisible)
                                    Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = null,
                                tint = inputColor,
                            )
                        }
                    }
                }
                isValid -> {
                    {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = TaskyGreen,
                        )
                    }
                }
                else -> null
            },
            placeholder = {
                Text(text = placeholder, style = AuthInputTextStyle, color = inputColor)
            },
            shape = RoundedCornerShape(10.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                errorContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
                errorIndicatorColor = Color.Transparent,
                focusedTextColor = inputColor,
                unfocusedTextColor = inputColor,
            ),
            isError = showError,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .then(
                    if (showError) Modifier.border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.error,
                        shape = RoundedCornerShape(10.dp),
                    ) else Modifier
                )
                .onFocusChanged { focusState ->
                    if (isFocused && !focusState.isFocused) isTouched = true
                    isFocused = focusState.isFocused
                },
        )
        AnimatedVisibility(visible = showError) {
            Text(
                text = error ?: "",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp),
            )
        }
    }
}


@Preview
@Composable
private fun AuthTextFieldEmailPreview() {
    AuthTextField(
        value = "test@email.com",
        onValueChange = { },
        placeholder = "Email address",
        isValid = true,
    )
}

@Preview
@Composable
private fun AuthTextFieldErrorPreview() {
    AuthTextField(
        value = "Al",
        onValueChange = { },
        placeholder = "Name",
    )
}

@Preview
@Composable
private fun AuthTextFieldPasswordPreview() {
    AuthPasswordField(
        value = "123123",
        onValueChange = { },
        placeholder = "Password",
        isPasswordVisible = false,
        onPasswordVisibilityClick = {},
        error = "Password must be at least 9 characters long and contain at least one uppercase, one lowercase and one digit",
        forceShowError = true
    )
}
