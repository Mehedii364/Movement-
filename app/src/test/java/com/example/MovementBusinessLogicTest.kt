package com.example

import com.example.domain.utils.TimeUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

class MovementBusinessLogicTest {

    @Test
    fun testDurationCalculation() {
        val startMillis = 1000000L
        val endMillis = startMillis + TimeUnit.HOURS.toMillis(1) + TimeUnit.MINUTES.toMillis(15)

        val durationMinutes = TimeUtils.calculateDurationMinutes(startMillis, endMillis)
        assertEquals(75L, durationMinutes)

        val formatted = TimeUtils.formatDuration(durationMinutes)
        assertEquals("1 hr 15 min", formatted)
    }

    @Test
    fun testMidnightCrossingDuration() {
        // 11:30 PM to 01:15 AM next day (1 hr 45 min = 105 min)
        val startMillis = 1700000000000L
        val endMillis = startMillis + TimeUnit.HOURS.toMillis(1) + TimeUnit.MINUTES.toMillis(45)

        val minutes = TimeUtils.calculateDurationMinutes(startMillis, endMillis)
        assertEquals(105L, minutes)

        val formatted = TimeUtils.formatDuration(minutes)
        assertEquals("1 hr 45 min", formatted)
    }

    @Test
    fun testZeroAndNegativeDuration() {
        val sameTime = 1700000000000L
        val minutes = TimeUtils.calculateDurationMinutes(sameTime, sameTime)
        assertEquals(0L, minutes)
        assertEquals("0 min", TimeUtils.formatDuration(minutes))

        // Negative time
        val negativeMinutes = TimeUtils.calculateDurationMinutes(sameTime + 1000, sameTime)
        assertEquals(0L, negativeMinutes)
    }

    @Test
    fun testFormatDateTime() {
        val timestamp = 1700000000000L
        val formatted = TimeUtils.formatDate(timestamp, "yyyy")
        assertTrue(formatted.isNotBlank())
    }
}
