package com.fuerz4.assistant.presentation.common

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import com.fuerz4.assistant.R

/**
 * Password field with a show/hide ("eye") toggle — shared so every password entry point looks
 * and behaves the same. Visibility state resets whenever [resetVisibilityKey] changes: Compose
 * Navigation keeps a destination's composition alive while it's merely covered by another screen
 * pushed on top (not popped), so plain internal `remember` alone survives a "back" return and
 * would leave the field toggled visible — callers that need a hard reset on re-entry (e.g. Login)
 * pass a key that changes on each `ON_RESUME`.
 */
@Composable
fun PasswordTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    resetVisibilityKey: Any? = null
) {
    var isVisible by remember(resetVisibilityKey) { mutableStateOf(false) }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        visualTransformation = if (isVisible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            IconButton(onClick = { isVisible = !isVisible }) {
                Icon(
                    imageVector = if (isVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                    contentDescription = stringResource(
                        if (isVisible) R.string.common_hide_password else R.string.common_show_password
                    )
                )
            }
        },
        modifier = modifier
    )
}
