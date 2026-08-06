package com.fuerz4.assistant.domain.model

import java.util.Calendar

data class DeviceReading(
    val timestamp: Long,
    val volts: Double? = null,
    val amps: Double? = null,
    val temp: Double? = null,
    val hum: Double? = null,
    val frequency: Double? = null,
    val cosPhi: Double? = null,
    val activePower: Double? = null
)

/** UI-facing time range choice for the device readings chart, each mapped to the server's history granularity. */
enum class DeviceHistoryRange(val viewType: String) {
    DAY("byHour"),
    MONTH("byDay"),
    YEAR("byMonth"),
    ALL("byYear")
}

/** From/until bounds for a [DeviceHistoryRange] query. A null [from] means "unbounded" (server treats a missing `fromDate` that way). */
data class DeviceHistoryBounds(val from: Long?, val until: Long?)

object DeviceHistoryRangeCalculator {

    /** [selectedDateMillis] anchors DAY/MONTH/YEAR; ignored for ALL. [now] is injectable for deterministic tests. */
    fun bounds(
        range: DeviceHistoryRange,
        selectedDateMillis: Long,
        now: Long = System.currentTimeMillis()
    ): DeviceHistoryBounds {
        val calendar = Calendar.getInstance().apply { timeInMillis = selectedDateMillis }

        return when (range) {
            DeviceHistoryRange.DAY -> {
                val from = calendar.clone() as Calendar
                from.set(Calendar.HOUR_OF_DAY, 0)
                from.set(Calendar.MINUTE, 0)
                from.set(Calendar.SECOND, 0)
                from.set(Calendar.MILLISECOND, 0)
                val until = from.clone() as Calendar
                until.add(Calendar.DAY_OF_MONTH, 1)
                DeviceHistoryBounds(from.timeInMillis, until.timeInMillis)
            }
            DeviceHistoryRange.MONTH -> {
                val from = calendar.clone() as Calendar
                from.set(Calendar.DAY_OF_MONTH, 1)
                from.set(Calendar.HOUR_OF_DAY, 0)
                from.set(Calendar.MINUTE, 0)
                from.set(Calendar.SECOND, 0)
                from.set(Calendar.MILLISECOND, 0)
                val until = from.clone() as Calendar
                until.add(Calendar.MONTH, 1)
                DeviceHistoryBounds(from.timeInMillis, until.timeInMillis)
            }
            DeviceHistoryRange.YEAR -> {
                val from = calendar.clone() as Calendar
                from.set(Calendar.MONTH, Calendar.JANUARY)
                from.set(Calendar.DAY_OF_MONTH, 1)
                from.set(Calendar.HOUR_OF_DAY, 0)
                from.set(Calendar.MINUTE, 0)
                from.set(Calendar.SECOND, 0)
                from.set(Calendar.MILLISECOND, 0)
                val until = from.clone() as Calendar
                until.add(Calendar.YEAR, 1)
                DeviceHistoryBounds(from.timeInMillis, until.timeInMillis)
            }
            DeviceHistoryRange.ALL -> DeviceHistoryBounds(from = null, until = now)
        }
    }
}
