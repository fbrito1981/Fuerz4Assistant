package com.fuerz4.assistant.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar
import java.util.GregorianCalendar

class DeviceHistoryRangeCalculatorTest {

    /** 2026-08-06 14:30 local time — a Thursday, mid-month, mid-year. */
    private fun selectedDate(): Long = GregorianCalendar(2026, Calendar.AUGUST, 6, 14, 30).timeInMillis

    private fun millisOf(year: Int, month: Int, day: Int): Long =
        GregorianCalendar(year, month, day, 0, 0, 0).apply { set(Calendar.MILLISECOND, 0) }.timeInMillis

    @Test
    fun `DAY bounds are the start and end of the selected calendar day`() {
        val bounds = DeviceHistoryRangeCalculator.bounds(DeviceHistoryRange.DAY, selectedDate())

        assertEquals(millisOf(2026, Calendar.AUGUST, 6), bounds.from)
        assertEquals(millisOf(2026, Calendar.AUGUST, 7), bounds.until)
    }

    @Test
    fun `MONTH bounds are the first day of the selected month and the next month`() {
        val bounds = DeviceHistoryRangeCalculator.bounds(DeviceHistoryRange.MONTH, selectedDate())

        assertEquals(millisOf(2026, Calendar.AUGUST, 1), bounds.from)
        assertEquals(millisOf(2026, Calendar.SEPTEMBER, 1), bounds.until)
    }

    @Test
    fun `YEAR bounds are January 1st of the selected year and the next year`() {
        val bounds = DeviceHistoryRangeCalculator.bounds(DeviceHistoryRange.YEAR, selectedDate())

        assertEquals(millisOf(2026, Calendar.JANUARY, 1), bounds.from)
        assertEquals(millisOf(2027, Calendar.JANUARY, 1), bounds.until)
    }

    @Test
    fun `ALL bounds are unbounded from and now as until`() {
        val now = selectedDate()
        val bounds = DeviceHistoryRangeCalculator.bounds(DeviceHistoryRange.ALL, selectedDateMillis = 0L, now = now)

        assertEquals(null, bounds.from)
        assertEquals(now, bounds.until)
    }
}
