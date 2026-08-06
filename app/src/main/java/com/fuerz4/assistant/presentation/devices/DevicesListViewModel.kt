package com.fuerz4.assistant.presentation.devices

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fuerz4.assistant.R
import com.fuerz4.assistant.data.connectivity.NetworkMonitor
import com.fuerz4.assistant.data.remote.NanoApiError
import com.fuerz4.assistant.domain.model.Device
import com.fuerz4.assistant.domain.repository.DeviceRepository
import com.fuerz4.assistant.presentation.common.UiText
import com.fuerz4.assistant.presentation.common.toUiText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DevicesListUiState(
    val devices: List<Device> = emptyList(),
    val isLoading: Boolean = false,
    val error: UiText? = null
)

@HiltViewModel
class DevicesListViewModel @Inject constructor(
    private val deviceRepository: DeviceRepository,
    private val networkMonitor: NetworkMonitor
) : ViewModel() {

    private val _uiState = MutableStateFlow(DevicesListUiState())
    val uiState: StateFlow<DevicesListUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        if (!networkMonitor.isOnline.value) {
            _uiState.update { it.copy(error = UiText.Resource(R.string.common_offline_message)) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            deviceRepository.listDevices()
                .onSuccess { devices ->
                    _uiState.update { it.copy(isLoading = false, devices = devices) }
                }
                .onFailure { throwable ->
                    val uiText = (throwable as? NanoApiError)?.toUiText()
                        ?: UiText.Resource(R.string.devices_load_error)
                    _uiState.update { it.copy(isLoading = false, error = uiText) }
                }
        }
    }
}
