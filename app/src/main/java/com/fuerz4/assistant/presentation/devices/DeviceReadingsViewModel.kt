package com.fuerz4.assistant.presentation.devices

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fuerz4.assistant.R
import com.fuerz4.assistant.data.connectivity.NetworkMonitor
import com.fuerz4.assistant.data.remote.NanoApiError
import com.fuerz4.assistant.domain.model.DeviceHistoryRange
import com.fuerz4.assistant.domain.model.DeviceHistoryRangeCalculator
import com.fuerz4.assistant.domain.model.DeviceReading
import com.fuerz4.assistant.domain.model.DeviceType
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

enum class ReadingValue { VOLTS, AMPS, POWER, FREQUENCY, COS_PHI, TEMP, HUM }

data class DeviceReadingsUiState(
    val deviceName: String = "",
    val type: DeviceType = DeviceType.ENERGY,
    val latest: DeviceReading? = null,
    val isLoadingLatest: Boolean = false,
    val selectedValue: ReadingValue = ReadingValue.VOLTS,
    val range: DeviceHistoryRange = DeviceHistoryRange.DAY,
    val selectedDateMillis: Long = System.currentTimeMillis(),
    val historyPoints: List<DeviceReading> = emptyList(),
    val isLoadingHistory: Boolean = false,
    val error: UiText? = null
)

@HiltViewModel
class DeviceReadingsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val deviceRepository: DeviceRepository,
    private val networkMonitor: NetworkMonitor
) : ViewModel() {

    private val uuid: String = savedStateHandle.get<String>("uuid").orEmpty()
    private val initialType = DeviceType.fromWireValue(savedStateHandle.get<String>("deviceType")) ?: DeviceType.ENERGY

    private val _uiState = MutableStateFlow(
        DeviceReadingsUiState(type = initialType, selectedValue = defaultValueFor(initialType))
    )
    val uiState: StateFlow<DeviceReadingsUiState> = _uiState.asStateFlow()

    init {
        loadDevice()
        loadLatest()
        loadHistory()
    }

    private fun loadDevice() {
        viewModelScope.launch {
            deviceRepository.listDevices()
                .onSuccess { devices ->
                    val device = devices.firstOrNull { it.uuid == uuid } ?: return@onSuccess
                    val type = device.type ?: initialType
                    _uiState.update {
                        it.copy(deviceName = device.name, type = type, selectedValue = defaultValueFor(type))
                    }
                }
        }
    }

    private fun loadLatest() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingLatest = true) }
            deviceRepository.getLatestReading(uuid)
                .onSuccess { reading -> _uiState.update { it.copy(isLoadingLatest = false, latest = reading) } }
                .onFailure { throwable -> _uiState.update { it.copy(isLoadingLatest = false, error = throwable.toDisplayError()) } }
        }
    }

    private fun loadHistory() {
        if (!networkMonitor.isOnline.value) {
            _uiState.update { it.copy(error = UiText.Resource(R.string.common_offline_message)) }
            return
        }

        val state = _uiState.value
        val bounds = DeviceHistoryRangeCalculator.bounds(state.range, state.selectedDateMillis)

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingHistory = true, error = null) }
            deviceRepository.getHistory(uuid, bounds.from, bounds.until, state.range.viewType)
                .onSuccess { points -> _uiState.update { it.copy(isLoadingHistory = false, historyPoints = points) } }
                .onFailure { throwable -> _uiState.update { it.copy(isLoadingHistory = false, error = throwable.toDisplayError()) } }
        }
    }

    fun onValueSelected(value: ReadingValue) = _uiState.update { it.copy(selectedValue = value) }

    fun refreshLatest() = loadLatest()

    fun onRangeSelected(range: DeviceHistoryRange) {
        _uiState.update { it.copy(range = range, selectedDateMillis = System.currentTimeMillis()) }
        loadHistory()
    }

    fun onDateSelected(dateMillis: Long) {
        _uiState.update { it.copy(selectedDateMillis = dateMillis) }
        loadHistory()
    }

    private fun defaultValueFor(type: DeviceType) =
        if (type == DeviceType.ENERGY) ReadingValue.VOLTS else ReadingValue.TEMP

    private fun Throwable.toDisplayError(): UiText =
        (this as? NanoApiError)?.toUiText() ?: UiText.Resource(R.string.common_error_general)
}
