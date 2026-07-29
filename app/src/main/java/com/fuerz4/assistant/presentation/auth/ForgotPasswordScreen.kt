package com.fuerz4.assistant.presentation.auth

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fuerz4.assistant.R

@Composable
fun ForgotPasswordScreen(
    onResetSuccess: () -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: ForgotPasswordViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Text(stringResource(R.string.forgot_title), style = MaterialTheme.typography.titleLarge)

        when (uiState.step) {
            ForgotPasswordStep.REQUEST_CODE -> {
                Text(
                    stringResource(R.string.forgot_step_request_title),
                    modifier = Modifier.padding(top = 16.dp)
                )
                OutlinedTextField(
                    value = uiState.email,
                    onValueChange = viewModel::onEmailChange,
                    label = { Text(stringResource(R.string.login_email_label)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                )
                ErrorText(uiState)
                LoadingButton(
                    text = stringResource(R.string.forgot_send_code),
                    isLoading = uiState.isLoading,
                    onClick = viewModel::sendCode
                )
            }

            ForgotPasswordStep.VALIDATE_CODE -> {
                Text(
                    stringResource(R.string.forgot_step_validate_title),
                    modifier = Modifier.padding(top = 16.dp)
                )
                OutlinedTextField(
                    value = uiState.code,
                    onValueChange = viewModel::onCodeChange,
                    label = { Text(stringResource(R.string.forgot_code_label)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                )
                ErrorText(uiState)
                LoadingButton(
                    text = stringResource(R.string.forgot_validate_code),
                    isLoading = uiState.isLoading,
                    onClick = viewModel::validateCode
                )
            }

            ForgotPasswordStep.RESET_PASSWORD -> {
                Text(
                    stringResource(R.string.forgot_step_reset_title),
                    modifier = Modifier.padding(top = 16.dp)
                )
                OutlinedTextField(
                    value = uiState.newPassword,
                    onValueChange = viewModel::onNewPasswordChange,
                    label = { Text(stringResource(R.string.forgot_new_password_label)) },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                )
                ErrorText(uiState)
                LoadingButton(
                    text = stringResource(R.string.forgot_reset_password),
                    isLoading = uiState.isLoading,
                    onClick = { viewModel.resetPassword(onResetSuccess) }
                )
            }
        }

        TextButton(onClick = onNavigateBack, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.common_back))
        }
    }
}

@Composable
private fun ErrorText(uiState: ForgotPasswordUiState) {
    uiState.error?.let { error ->
        Text(
            text = error.asString(),
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
private fun LoadingButton(text: String, isLoading: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = !isLoading,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp)
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = MaterialTheme.colorScheme.onPrimary
            )
        } else {
            Text(text)
        }
    }
}
