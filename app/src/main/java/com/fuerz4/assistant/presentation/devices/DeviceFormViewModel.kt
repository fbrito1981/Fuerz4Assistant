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
import com.fuerz4.assistant.domain.model.Device
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
    data class Success(val device: Device) : ProvisioningStep()
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
    val step: ProvisioningStep = ProvisioningStep.Form,
    val validationError: UiText? = null
) {
    val isProvisioning: Boolean
        get() = step !is ProvisioningStep.Form && step !is ProvisioningStep.Success && step !is ProvisioningStep.Error
}

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

    private val _uiState = MutableStateFlow(DeviceFormUiState(type = initialType))
    val uiState: StateFlow<DeviceFormUiState> = _uiState.asStateFlow()

    fun onNameChange(value: String) = _uiState.update { it.copy(name = value, validationError = null) }
    fun onHomeSsidChange(value: String) = _uiState.update { it.copy(homeSsid = value, validationError = null) }
    fun onHomePasswordChange(value: String) = _uiState.update { it.copy(homePassword = value, validationError = null) }
    fun onVoltsChange(value: String) = _uiState.update { it.copy(volts = value) }
    fun onAmpsChange(value: String) = _uiState.update { it.copy(amps = value) }
    fun onTempChange(value: String) = _uiState.update { it.copy(temp = value) }
    fun onHumChange(value: String) = _uiState.update { it.copy(hum = value) }

    fun startProvisioning() {
        val state = _uiState.value

        if (state.name.isBlank() || state.homeSsid.isBlank() || state.homePassword.isBlank()) {
            _uiState.update { it.copy(validationError = UiText.Resource(R.string.device_form_validation_error)) }
            return
        }

        if (!networkMonitor.isWifiConnected()) {
            _uiState.update {
                it.copy(step = ProvisioningStep.Error(UiText.Resource(R.string.device_form_offline_required)))
            }
            return
        }

        viewModelScope.launch {
            val deviceId = deviceIdGenerator.generate(state.type)

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
                    deviceId = deviceId,
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
            val modelLabelRes = if (state.type == DeviceType.ENERGY) {
                R.string.device_type_energy
            } else {
                R.string.device_type_environment
            }
            val modelLabel = context.getString(modelLabelRes)

            deviceRepository.createDevice(uuid = deviceId, name = state.name.trim(), model = modelLabel)
                .onSuccess { device ->
                    _uiState.update { it.copy(step = ProvisioningStep.Success(device)) }
                }
                .onFailure { throwable ->
                    val uiText = (throwable as? NanoApiError)?.toUiText()
                        ?: UiText.Resource(R.string.common_error_general)
                    _uiState.update { it.copy(step = ProvisioningStep.Error(uiText)) }
                }
        }
    }
}
