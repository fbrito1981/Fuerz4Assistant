package com.fuerz4.assistant.presentation.devices

import android.content.pm.ActivityInfo
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fuerz4.assistant.R
import com.fuerz4.assistant.domain.model.DeviceHistoryRange
import com.fuerz4.assistant.domain.model.DeviceReading
import com.fuerz4.assistant.domain.model.DeviceType
import com.fuerz4.assistant.presentation.common.DropdownSelector
import com.fuerz4.assistant.presentation.common.LockScreenOrientation
import com.fuerz4.assistant.presentation.common.SegmentedOptions
import com.fuerz4.assistant.presentation.theme.Blanco
import com.fuerz4.assistant.presentation.theme.NaranjaOscuro
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.common.ProvideVicoTheme
import com.patrykandpatrick.vico.compose.m3.common.rememberM3VicoTheme
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

private val SPANISH = Locale("es", "AR")

private const val UNIT_VOLTS = "V"
private const val UNIT_AMPS = "A"
private const val UNIT_POWER = "W"
private const val UNIT_TEMP = "°C"
private const val UNIT_HUM = "%"
private const val UNIT_ENERGY = "kWh"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceReadingsScreen(
    onBack: () -> Unit,
    viewModel: DeviceReadingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showFullscreenChart by remember { mutableStateOf(false) }

    if (showFullscreenChart) {
        FullscreenChart(
            points = uiState.historyPoints,
            selectedValue = uiState.selectedValue,
            range = uiState.range,
            onDismiss = { showFullscreenChart = false }
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.deviceName) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            LatestValuesCard(uiState, onRefresh = viewModel::refreshLatest)

            if (uiState.selectedValue == ReadingValue.POWER) {
                AccumulatedEnergyCard(
                    energyKwh = remember(uiState.historyPoints) { computeAccumulatedEnergyKwh(uiState.historyPoints) },
                    modifier = Modifier.padding(top = 16.dp)
                )
            }

            ReadingsChart(
                points = uiState.historyPoints,
                selectedValue = uiState.selectedValue,
                range = uiState.range,
                onOpenFullscreen = { showFullscreenChart = true },
                modifier = Modifier.padding(top = 16.dp)
            )

            val valueOptions = if (uiState.type == DeviceType.ENERGY) {
                listOf(ReadingValue.VOLTS, ReadingValue.AMPS, ReadingValue.POWER, ReadingValue.FREQUENCY, ReadingValue.COS_PHI)
            } else {
                listOf(ReadingValue.TEMP, ReadingValue.HUM)
            }
            DropdownSelector(
                selected = uiState.selectedValue,
                options = valueOptions,
                optionLabel = { value -> stringResource(valueLabelRes(value)) },
                onSelect = viewModel::onValueSelected,
                modifier = Modifier.padding(top = 20.dp)
            )

            SegmentedOptions(
                options = DeviceHistoryRange.entries,
                selected = uiState.range,
                label = { range -> stringResource(rangeLabelRes(range)) },
                onSelect = viewModel::onRangeSelected,
                modifier = Modifier.padding(top = 12.dp)
            )

            if (uiState.range != DeviceHistoryRange.ALL) {
                DateSelector(
                    selectedDateMillis = uiState.selectedDateMillis,
                    range = uiState.range,
                    onDateSelected = viewModel::onDateSelected,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }

            uiState.error?.let { error ->
                Text(
                    text = error.asString(),
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }
        }
    }
}

@Composable
private fun LatestValuesCard(state: DeviceReadingsUiState, onRefresh: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Blanco),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.device_readings_latest_title), style = MaterialTheme.typography.labelLarge)
                IconButton(onClick = onRefresh) {
                    Icon(
                        Icons.Filled.Refresh,
                        contentDescription = stringResource(R.string.device_readings_refresh),
                        tint = NaranjaOscuro
                    )
                }
            }

            val latest = state.latest
            if (latest == null) {
                Text(
                    stringResource(R.string.device_readings_empty_latest),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp)
                )
            } else {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
                ) {
                    if (state.type == DeviceType.ENERGY) {
                        ReadingStat(R.string.device_readings_value_volts, latest.volts, UNIT_VOLTS)
                        ReadingStat(R.string.device_readings_value_amps, latest.amps, UNIT_AMPS)
                        ReadingStat(R.string.device_readings_value_power, latest.activePower, UNIT_POWER)
                    } else {
                        ReadingStat(R.string.device_readings_value_temp, latest.temp, UNIT_TEMP)
                        ReadingStat(R.string.device_readings_value_hum, latest.hum, UNIT_HUM)
                    }
                }
            }
        }
    }
}

@Composable
private fun ReadingStat(labelRes: Int, value: Double?, unit: String) {
    Column {
        Text(
            stringResource(labelRes),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(value?.let { "${formatReadingValue(it)} $unit" } ?: "—", style = MaterialTheme.typography.titleLarge)
    }
}

@Composable
private fun AccumulatedEnergyCard(energyKwh: Double?, modifier: Modifier = Modifier) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Blanco),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
            Text(
                stringResource(R.string.device_readings_accumulated_energy_title),
                style = MaterialTheme.typography.labelLarge
            )
            Text(
                energyKwh?.let { "${formatReadingValue(it)} $UNIT_ENERGY" } ?: "—",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun ReadingsChart(
    points: List<DeviceReading>,
    selectedValue: ReadingValue,
    range: DeviceHistoryRange,
    onOpenFullscreen: () -> Unit,
    modifier: Modifier = Modifier
) {
    val chartPoints = remember(points, selectedValue) { computeChartPoints(points, selectedValue) }

    if (chartPoints.isEmpty()) {
        Box(
            modifier = modifier.fillMaxWidth().height(200.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                stringResource(R.string.device_readings_empty_chart),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    VicoChartHost(
        chartPoints = chartPoints,
        range = range,
        modifier = modifier.fillMaxWidth().height(220.dp).clickable { onOpenFullscreen() }
    )
}

/**
 * Rendered in place of [DeviceReadingsScreen]'s whole Scaffold (not a separate [Dialog] window) so
 * it always measures against the Activity's real, current window size — a secondary Dialog window
 * was found to report stale (pre-rotation) bounds right after [LockScreenOrientation] flips the
 * orientation, rendering a narrow letterboxed chart instead of filling the landscape screen.
 */
@Composable
private fun FullscreenChart(
    points: List<DeviceReading>,
    selectedValue: ReadingValue,
    range: DeviceHistoryRange,
    onDismiss: () -> Unit
) {
    LockScreenOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
    val chartPoints = remember(points, selectedValue) { computeChartPoints(points, selectedValue) }

    Surface(modifier = Modifier.fillMaxSize(), color = Blanco) {
        Box(modifier = Modifier.fillMaxSize()) {
            VicoChartHost(
                chartPoints = chartPoints,
                range = range,
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth(0.94f)
                    .aspectRatio(16f / 7f)
                    .padding(vertical = 8.dp)
            )
            IconButton(onClick = onDismiss, modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)) {
                Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.common_back))
            }
        }
    }
}

@Composable
private fun VicoChartHost(chartPoints: List<Pair<Long, Double>>, range: DeviceHistoryRange, modifier: Modifier) {
    val modelProducer = remember { CartesianChartModelProducer() }
    LaunchedEffect(chartPoints) {
        modelProducer.runTransaction {
            lineSeries { series(chartPoints.map { it.second }) }
        }
    }

    ProvideVicoTheme(rememberM3VicoTheme()) {
        CartesianChartHost(
            chart = rememberCartesianChart(
                rememberLineCartesianLayer(),
                startAxis = VerticalAxis.rememberStart(),
                bottomAxis = HorizontalAxis.rememberBottom(
                    valueFormatter = CartesianValueFormatter { _, value, _ ->
                        // Vico may probe x positions beyond the exact data indices (e.g. for label-width
                        // measurement); it throws if this ever returns "", so clamp instead of nulling out.
                        val index = value.roundToInt().coerceIn(0, chartPoints.lastIndex)
                        formatAxisLabel(chartPoints[index].first, range)
                    }
                )
            ),
            modelProducer = modelProducer,
            modifier = modifier
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateSelector(
    selectedDateMillis: Long,
    range: DeviceHistoryRange,
    onDateSelected: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDatePicker by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(50),
        color = Blanco,
        border = BorderStroke(1.5.dp, NaranjaOscuro),
        modifier = modifier
            .fillMaxWidth()
            .clickable { showDatePicker = true }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp)
        ) {
            Icon(
                Icons.Filled.CalendarMonth,
                contentDescription = null,
                tint = NaranjaOscuro,
                modifier = Modifier.padding(end = 8.dp)
            )
            Text(
                text = formatSelectedDateLabel(selectedDateMillis, range),
                color = NaranjaOscuro,
                style = MaterialTheme.typography.labelLarge
            )
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDateMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { utcMillis ->
                        val localDate = Instant.ofEpochMilli(utcMillis).atZone(ZoneOffset.UTC).toLocalDate()
                        onDateSelected(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli())
                    }
                    showDatePicker = false
                }) { Text(stringResource(R.string.common_accept)) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text(stringResource(R.string.common_cancel)) }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

/** Sorted ascending by timestamp so the X axis always reads oldest (left) to newest (right), regardless of API order. */
private fun computeChartPoints(points: List<DeviceReading>, selectedValue: ReadingValue): List<Pair<Long, Double>> =
    points.mapNotNull { point -> point.valueFor(selectedValue)?.let { point.timestamp to it } }
        .sortedBy { it.first }

/**
 * Net energy (import minus export) for the period, in kWh — the trapezoidal integral of active
 * power (W) over time. Signed on purpose: [latest.activePower] can go negative (see
 * `EnergyLogDto`'s power-factor calc — a branch with a zero-export solar inverter can briefly
 * backfeed past this device's CT), so a negative total here is a real "net exported" reading, not
 * an error. `null` when there are fewer than two power samples to integrate between.
 */
private fun computeAccumulatedEnergyKwh(points: List<DeviceReading>): Double? {
    val powerPoints = computeChartPoints(points, ReadingValue.POWER)
    if (powerPoints.size < 2) return null

    var wattHours = 0.0
    for (i in 0 until powerPoints.size - 1) {
        val (t1, p1) = powerPoints[i]
        val (t2, p2) = powerPoints[i + 1]
        val hours = (t2 - t1) / 3_600_000.0
        wattHours += (p1 + p2) / 2.0 * hours
    }
    return wattHours / 1000.0
}

private fun DeviceReading.valueFor(selected: ReadingValue): Double? = when (selected) {
    ReadingValue.VOLTS -> volts
    ReadingValue.AMPS -> amps
    ReadingValue.POWER -> activePower
    ReadingValue.FREQUENCY -> frequency
    ReadingValue.COS_PHI -> cosPhi
    ReadingValue.TEMP -> temp
    ReadingValue.HUM -> hum
}

private fun valueLabelRes(value: ReadingValue) = when (value) {
    ReadingValue.VOLTS -> R.string.device_readings_value_volts
    ReadingValue.AMPS -> R.string.device_readings_value_amps
    ReadingValue.POWER -> R.string.device_readings_value_power
    ReadingValue.FREQUENCY -> R.string.device_readings_value_frequency
    ReadingValue.COS_PHI -> R.string.device_readings_value_cos_phi
    ReadingValue.TEMP -> R.string.device_readings_value_temp
    ReadingValue.HUM -> R.string.device_readings_value_hum
}

private fun rangeLabelRes(range: DeviceHistoryRange) = when (range) {
    DeviceHistoryRange.DAY -> R.string.device_readings_range_day
    DeviceHistoryRange.MONTH -> R.string.device_readings_range_month
    DeviceHistoryRange.YEAR -> R.string.device_readings_range_year
    DeviceHistoryRange.ALL -> R.string.device_readings_range_all
}

private fun formatReadingValue(value: Double): String = String.format(SPANISH, "%.1f", value)

private fun formatAxisLabel(timestamp: Long, range: DeviceHistoryRange): String {
    val zonedDateTime = Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault())
    val pattern = when (range) {
        DeviceHistoryRange.DAY -> "HH:mm"
        DeviceHistoryRange.MONTH -> "d"
        DeviceHistoryRange.YEAR -> "MMM"
        DeviceHistoryRange.ALL -> "yyyy"
    }
    return zonedDateTime.format(DateTimeFormatter.ofPattern(pattern, SPANISH))
}

private fun formatSelectedDateLabel(millis: Long, range: DeviceHistoryRange): String {
    val zonedDateTime = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault())
    val pattern = when (range) {
        DeviceHistoryRange.DAY -> "d 'de' MMMM 'de' yyyy"
        DeviceHistoryRange.MONTH -> "MMMM yyyy"
        DeviceHistoryRange.YEAR -> "yyyy"
        DeviceHistoryRange.ALL -> ""
    }
    return zonedDateTime.format(DateTimeFormatter.ofPattern(pattern, SPANISH))
}
