package com.fuerz4.assistant.presentation.devices

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fuerz4.assistant.R
import com.fuerz4.assistant.domain.model.DeviceType
import com.fuerz4.assistant.presentation.common.PasswordTextField
import com.fuerz4.assistant.presentation.common.RoundedSelectableList

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
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val ssidFocusRequester = remember { FocusRequester() }
    val passwordFocusRequester = remember { FocusRequester() }
    val settingsFirstFocusRequester = remember { FocusRequester() }
    val settingsSecondFocusRequester = remember { FocusRequester() }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) viewModel.refreshWifiNetworks() }

    LaunchedEffect(Unit) {
        val alreadyGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (alreadyGranted) {
            viewModel.refreshWifiNetworks()
        } else {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

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
            shape = RoundedCornerShape(50),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { ssidFocusRequester.requestFocus() }),
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
        )

        Text(
            stringResource(R.string.device_form_wifi_section),
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(top = 24.dp)
        )

        WifiSsidField(
            value = uiState.homeSsid,
            onValueChange = viewModel::onHomeSsidChange,
            availableSsids = uiState.availableSsids,
            label = stringResource(R.string.device_form_wifi_ssid_label),
            focusRequester = ssidFocusRequester,
            keyboardActions = KeyboardActions(onNext = { passwordFocusRequester.requestFocus() }),
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        )

        PasswordTextField(
            value = uiState.homePassword,
            onValueChange = viewModel::onHomePasswordChange,
            label = stringResource(R.string.device_form_wifi_password_label),
            imeAction = ImeAction.Next,
            keyboardActions = KeyboardActions(onNext = { settingsFirstFocusRequester.requestFocus() }),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .focusRequester(passwordFocusRequester)
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
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { settingsSecondFocusRequester.requestFocus() }),
                singleLine = true,
                shape = RoundedCornerShape(50),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .focusRequester(settingsFirstFocusRequester)
            )
            OutlinedTextField(
                value = uiState.amps,
                onValueChange = viewModel::onAmpsChange,
                label = { Text(stringResource(R.string.device_form_amps_label)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() }),
                singleLine = true,
                shape = RoundedCornerShape(50),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .focusRequester(settingsSecondFocusRequester)
            )
        } else {
            OutlinedTextField(
                value = uiState.temp,
                onValueChange = viewModel::onTempChange,
                label = { Text(stringResource(R.string.device_form_temp_label)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { settingsSecondFocusRequester.requestFocus() }),
                singleLine = true,
                shape = RoundedCornerShape(50),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .focusRequester(settingsFirstFocusRequester)
            )
            OutlinedTextField(
                value = uiState.hum,
                onValueChange = viewModel::onHumChange,
                label = { Text(stringResource(R.string.device_form_hum_label)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() }),
                singleLine = true,
                shape = RoundedCornerShape(50),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .focusRequester(settingsSecondFocusRequester)
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

/**
 * Editable autocomplete-style selector for the home WiFi SSID: opens a dropdown of nearby
 * networks ([availableSsids], already filtered to 2.4GHz by [com.fuerz4.assistant.data.provisioning.WifiNetworkScanner]
 * when that info is available) but still accepts free typing, since a scan can miss a hidden or
 * momentarily out-of-range network and the user must always be able to enter the SSID manually.
 */
@Composable
private fun WifiSsidField(
    value: String,
    onValueChange: (String) -> Unit,
    availableSsids: List<String>,
    label: String,
    focusRequester: FocusRequester,
    keyboardActions: KeyboardActions,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = {
                onValueChange(it)
                expanded = true
            },
            label = { Text(label) },
            singleLine = true,
            shape = RoundedCornerShape(50),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions = keyboardActions,
            trailingIcon = {
                if (availableSsids.isNotEmpty()) {
                    val rotation by animateFloatAsState(if (expanded) 180f else 0f, label = "ssidChevron")
                    IconButton(onClick = { expanded = !expanded }) {
                        Icon(
                            Icons.Filled.ArrowDropDown,
                            contentDescription = null,
                            modifier = Modifier.rotate(rotation)
                        )
                    }
                }
            },
            modifier = Modifier
                .focusRequester(focusRequester)
                .onFocusChanged { if (it.isFocused) expanded = true }
                .fillMaxWidth()
        )

        if (expanded && availableSsids.isNotEmpty()) {
            RoundedSelectableList(
                items = availableSsids,
                onItemSelected = { ssid ->
                    onValueChange(ssid)
                    expanded = false
                },
                modifier = Modifier.padding(top = 4.dp)
            )
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
