package com.fuerz4.assistant.presentation.devices

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fuerz4.assistant.R
import com.fuerz4.assistant.data.connectivity.NetworkMonitor
import com.fuerz4.assistant.data.provisioning.DeviceIdGenerator
import com.fuerz4.assistant.data.provisioning.ProvisioningResult
import com.fuerz4.assistant.data.provisioning.UdpProvisioningClient
import com.fuerz4.assistant.data.provisioning.WifiConnectResult
import com.fuerz4.assistant.data.provisioning.WifiProvisioningManager
import com.fuerz4.assistant.data.remote.NanoApiError
import com.fuerz4.assistant.domain.model.DeviceType
import com.fuerz4.assistant.domain.repository.DeviceRepository
import com.fuerz4.assistant.presentation.common.UiText
import com.fuerz4.assistant.presentation.common.toUiText
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val DEVICE_AP_SSID = "nUdpWiFi"
private const val DEVICE_AP_PASSWORD = "\$n=f16e7r81"

sealed class ProvisioningStep {
    data object Form : ProvisioningStep()
    data object ConnectingToDeviceAp : ProvisioningStep()
    data object SendingConfig : ProvisioningStep()
    data object RestoringNetwork : ProvisioningStep()
    data object RegisteringDevice : ProvisioningStep()
    data object Success : ProvisioningStep()
    data class Error(val message: UiText) : ProvisioningStep()
}

data class DeviceFormUiState(
    val type: DeviceType = DeviceType.ENERGY,
    val name: String = "",
    val homeSsid: String = "",
    val homePassword: String = "",
    val volts: String = "",
    val amps: String = "",
    val temp: String = "",
    val hum: String = "",
    val isEditMode: Boolean = false,
    val isLoadingExisting: Boolean = false,
    val loadError: UiText? = null,
    val step: ProvisioningStep = ProvisioningStep.Form,
    val validationError: UiText? = null
)

@HiltViewModel
class DeviceFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    @ApplicationContext private val context: Context,
    private val networkMonitor: NetworkMonitor,
    private val wifiProvisioningManager: WifiProvisioningManager,
    private val udpProvisioningClient: UdpProvisioningClient,
    private val deviceIdGenerator: DeviceIdGenerator,
    private val deviceRepository: DeviceRepository
) : ViewModel() {

    private val initialType = DeviceType.fromWireValue(savedStateHandle.get<String>("deviceType")) ?: DeviceType.ENERGY
    private val existingUuid: String? = savedStateHandle.get<String>("uuid")
    private var originalName: String = ""

    private val _uiState = MutableStateFlow(
        DeviceFormUiState(
            type = initialType,
            isEditMode = existingUuid != null,
            isLoadingExisting = existingUuid != null
        )
    )
    val uiState: StateFlow<DeviceFormUiState> = _uiState.asStateFlow()

    init {
        existingUuid?.let { loadExistingDevice(it) }
    }

    private fun loadExistingDevice(uuid: String) {
        viewModelScope.launch {
            deviceRepository.listDevices()
                .onSuccess { devices ->
                    val device = devices.firstOrNull { it.uuid == uuid }
                    if (device == null) {
                        _uiState.update {
                            it.copy(isLoadingExisting = false, loadError = UiText.Resource(R.string.device_form_load_error))
                        }
                        return@onSuccess
                    }
                    originalName = device.name
                    _uiState.update {
                        it.copy(
                            type = device.type ?: it.type,
                            name = device.name,
                            isLoadingExisting = false
                        )
                    }
                }
                .onFailure {
                    _uiState.update {
                        it.copy(isLoadingExisting = false, loadError = UiText.Resource(R.string.device_form_load_error))
                    }
                }
        }
    }

    fun retryLoad() {
        val uuid = existingUuid ?: return
        _uiState.update { it.copy(isLoadingExisting = true, loadError = null) }
        loadExistingDevice(uuid)
    }

    fun onNameChange(value: String) = _uiState.update { it.copy(name = value, validationError = null) }
    fun onHomeSsidChange(value: String) = _uiState.update { it.copy(homeSsid = value, validationError = null) }
    fun onHomePasswordChange(value: String) = _uiState.update { it.copy(homePassword = value, validationError = null) }
    fun onVoltsChange(value: String) = _uiState.update { it.copy(volts = value) }
    fun onAmpsChange(value: String) = _uiState.update { it.copy(amps = value) }
    fun onTempChange(value: String) = _uiState.update { it.copy(temp = value) }
    fun onHumChange(value: String) = _uiState.update { it.copy(hum = value) }

    fun submit() {
        val state = _uiState.value
        if (state.isEditMode) submitEdit(state) else submitCreate(state)
    }

    private fun submitEdit(state: DeviceFormUiState) {
        val uuid = existingUuid ?: return

        if (state.name.isBlank()) {
            _uiState.update { it.copy(validationError = UiText.Resource(R.string.device_form_validation_error)) }
            return
        }

        val hasWifiInput = state.homeSsid.isNotBlank() || state.homePassword.isNotBlank()
        val hasSettingsInput = if (state.type == DeviceType.ENERGY) {
            state.volts.isNotBlank() || state.amps.isNotBlank()
        } else {
            state.temp.isNotBlank() || state.hum.isNotBlank()
        }

        if (hasSettingsInput && !hasWifiInput) {
            _uiState.update { it.copy(validationError = UiText.Resource(R.string.device_form_wifi_required_for_settings)) }
            return
        }

        if (!hasWifiInput) {
            renameOnly(uuid, state)
            return
        }

        if (state.homeSsid.isBlank() || state.homePassword.isBlank()) {
            _uiState.update { it.copy(validationError = UiText.Resource(R.string.device_form_validation_error)) }
            return
        }

        val udpDeviceId = DeviceType.fromUuid(uuid)?.let { uuid.removePrefix(it.prefix) } ?: uuid
        runProvisioningAndRegister(state, dbUuid = uuid, udpDeviceId = udpDeviceId, isNewDevice = false)
    }

    private fun renameOnly(uuid: String, state: DeviceFormUiState) {
        val nameChanged = state.name.trim() != originalName.trim()
        if (!nameChanged) {
            _uiState.update { it.copy(step = ProvisioningStep.Success) }
            return
        }

        if (!networkMonitor.isOnline.value) {
            _uiState.update { it.copy(validationError = UiText.Resource(R.string.common_offline_message)) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(step = ProvisioningStep.RegisteringDevice, validationError = null) }
            deviceRepository.updateDevice(uuid, state.name.trim(), active = null)
                .onSuccess { _uiState.update { it.copy(step = ProvisioningStep.Success) } }
                .onFailure { throwable ->
                    val uiText = (throwable as? NanoApiError)?.toUiText() ?: UiText.Resource(R.string.common_error_general)
                    _uiState.update { it.copy(step = ProvisioningStep.Error(uiText)) }
                }
        }
    }

    private fun submitCreate(state: DeviceFormUiState) {
        if (state.name.isBlank() || state.homeSsid.isBlank() || state.homePassword.isBlank()) {
            _uiState.update { it.copy(validationError = UiText.Resource(R.string.device_form_validation_error)) }
            return
        }

        val newUuid = deviceIdGenerator.generate(state.type)
        runProvisioningAndRegister(state, dbUuid = newUuid, udpDeviceId = newUuid, isNewDevice = true)
    }

    private fun runProvisioningAndRegister(
        state: DeviceFormUiState,
        dbUuid: String,
        udpDeviceId: String,
        isNewDevice: Boolean
    ) {
        if (!networkMonitor.isWifiConnected()) {
            _uiState.update {
                it.copy(step = ProvisioningStep.Error(UiText.Resource(R.string.device_form_offline_required)))
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(step = ProvisioningStep.ConnectingToDeviceAp, validationError = null) }
            val connectResult = wifiProvisioningManager.connectToDeviceAp(DEVICE_AP_SSID, DEVICE_AP_PASSWORD)

            if (connectResult !is WifiConnectResult.Success) {
                _uiState.update {
                    it.copy(step = ProvisioningStep.Error(UiText.Resource(R.string.device_form_error_ap_connect)))
                }
                return@launch
            }

            try {
                _uiState.update { it.copy(step = ProvisioningStep.SendingConfig) }
                val provisionResult = udpProvisioningClient.provision(
                    network = connectResult.network,
                    type = state.type,
                    deviceId = udpDeviceId,
                    homeSsid = state.homeSsid.trim(),
                    homePass = state.homePassword,
                    volts = state.volts.toFloatOrNull(),
                    amps = state.amps.toFloatOrNull(),
                    temp = state.temp.toFloatOrNull(),
                    hum = state.hum.toFloatOrNull()
                )

                if (provisionResult != ProvisioningResult.Success) {
                    _uiState.update {
                        it.copy(step = ProvisioningStep.Error(UiText.Resource(R.string.device_form_error_udp)))
                    }
                    return@launch
                }
            } finally {
                _uiState.update { it.copy(step = ProvisioningStep.RestoringNetwork) }
                wifiProvisioningManager.releaseAndRestore()
            }

            _uiState.update { it.copy(step = ProvisioningStep.RegisteringDevice) }

            val registerResult = if (isNewDevice) {
                val modelLabelRes = if (state.type == DeviceType.ENERGY) {
                    R.string.device_type_energy
                } else {
                    R.string.device_type_environment
                }
                deviceRepository.createDevice(uuid = dbUuid, name = state.name.trim(), model = context.getString(modelLabelRes))
                    .map { }
            } else if (state.name.trim() != originalName.trim()) {
                deviceRepository.updateDevice(uuid = dbUuid, name = state.name.trim(), active = null).map { }
            } else {
                Result.success(Unit)
            }

            registerResult
                .onSuccess { _uiState.update { it.copy(step = ProvisioningStep.Success) } }
                .onFailure { throwable ->
                    val uiText = (throwable as? NanoApiError)?.toUiText() ?: UiText.Resource(R.string.common_error_general)
                    _uiState.update { it.copy(step = ProvisioningStep.Error(uiText)) }
                }
        }
    }
}
