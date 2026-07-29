package com.fuerz4.assistant.presentation.devices

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fuerz4.assistant.R
import com.fuerz4.assistant.domain.model.DeviceType

@Composable
fun DeviceFormScreen(
    deviceType: String,
    onProvisioned: () -> Unit,
    viewModel: DeviceFormViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.step) {
        if (uiState.step is ProvisioningStep.Success) {
            onProvisioned()
        }
    }

    when {
        uiState.isLoadingExisting -> ProvisioningProgress(ProvisioningStep.Form)
        uiState.loadError != null -> {
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(uiState.loadError!!.asString(), color = MaterialTheme.colorScheme.error)
                Button(onClick = viewModel::retryLoad, modifier = Modifier.padding(top = 16.dp)) {
                    Text(stringResource(R.string.common_retry))
                }
            }
        }
        else -> when (val step = uiState.step) {
            is ProvisioningStep.Form -> DeviceFormContent(uiState, viewModel)
            is ProvisioningStep.Error -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(step.message.asString(), color = MaterialTheme.colorScheme.error)
                    Button(onClick = viewModel::submit, modifier = Modifier.padding(top = 16.dp)) {
                        Text(stringResource(R.string.common_retry))
                    }
                }
            }
            is ProvisioningStep.Success -> Unit // handled by LaunchedEffect above
            else -> ProvisioningProgress(step)
        }
    }
}

@Composable
private fun DeviceFormContent(uiState: DeviceFormUiState, viewModel: DeviceFormViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        val titleRes = if (uiState.isEditMode) R.string.device_form_edit_title else R.string.device_form_title
        Text(stringResource(titleRes), style = MaterialTheme.typography.titleLarge)

        OutlinedTextField(
            value = uiState.name,
            onValueChange = viewModel::onNameChange,
            label = { Text(stringResource(R.string.device_form_name_label)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
        )

        Text(
            stringResource(R.string.device_form_wifi_section),
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(top = 24.dp)
        )
        OutlinedTextField(
            value = uiState.homeSsid,
            onValueChange = viewModel::onHomeSsidChange,
            label = { Text(stringResource(R.string.device_form_wifi_ssid_label)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        )
        OutlinedTextField(
            value = uiState.homePassword,
            onValueChange = viewModel::onHomePasswordChange,
            label = { Text(stringResource(R.string.device_form_wifi_password_label)) },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        )

        Text(
            stringResource(R.string.device_form_optional_section),
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(top = 24.dp)
        )

        if (uiState.type == DeviceType.ENERGY) {
            OutlinedTextField(
                value = uiState.volts,
                onValueChange = viewModel::onVoltsChange,
                label = { Text(stringResource(R.string.device_form_volts_label)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            )
            OutlinedTextField(
                value = uiState.amps,
                onValueChange = viewModel::onAmpsChange,
                label = { Text(stringResource(R.string.device_form_amps_label)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            )
        } else {
            OutlinedTextField(
                value = uiState.temp,
                onValueChange = viewModel::onTempChange,
                label = { Text(stringResource(R.string.device_form_temp_label)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            )
            OutlinedTextField(
                value = uiState.hum,
                onValueChange = viewModel::onHumChange,
                label = { Text(stringResource(R.string.device_form_hum_label)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            )
        }

        val reconfigureHintRes = if (uiState.isEditMode) {
            R.string.device_form_edit_reconfigure_hint
        } else {
            R.string.device_form_create_reconfigure_hint
        }
        Text(
            stringResource(reconfigureHintRes),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp)
        )

        uiState.validationError?.let { error ->
            Text(
                text = error.asString(),
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        Button(
            onClick = viewModel::submit,
            modifier = Modifier.fillMaxWidth().padding(top = 24.dp)
        ) {
            val submitLabelRes = if (uiState.isEditMode) R.string.common_save else R.string.device_form_submit
            Text(stringResource(submitLabelRes))
        }
    }
}

@Composable
private fun ProvisioningProgress(step: ProvisioningStep) {
    val labelRes = when (step) {
        ProvisioningStep.ConnectingToDeviceAp -> R.string.device_form_step_connecting
        ProvisioningStep.SendingConfig -> R.string.device_form_step_sending
        ProvisioningStep.RestoringNetwork -> R.string.device_form_step_restoring
        ProvisioningStep.RegisteringDevice -> R.string.device_form_step_registering
        else -> R.string.common_loading
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(modifier = Modifier.size(48.dp))
        Text(stringResource(labelRes), modifier = Modifier.padding(top = 16.dp))
    }
}
