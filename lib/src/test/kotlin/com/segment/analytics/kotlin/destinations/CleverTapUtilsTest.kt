package com.segment.analytics.kotlin.destinations

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.text.SimpleDateFormat
import java.util.*

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class CleverTapUtilsTest {
    @Test
    fun `test getClevertapDate returns correctly formatted date string`() {
        // Given
        val timestamp = 1640995200000L // January 1, 2022 00:00:00 UTC
        val date = Date(timestamp)

        // When
        val result = CleverTapUtils.getClevertapDate(date)

        // Then
        val expectedSeconds = timestamp / 1000 // 1640995200
        val expected = "\$D_$expectedSeconds"
        assertEquals(expected, result)
        assertEquals("\$D_1640995200", result)
    }

    @Test
    fun `test getClevertapDate with epoch date`() {
        // Given - Unix epoch (January 1, 1970 00:00:00 UTC)
        val date = Date(0L)

        // When
        val result = CleverTapUtils.getClevertapDate(date)

        // Then
        assertEquals("\$D_0", result)
    }


    @Test
    fun `test getClevertapDate with future dates`() {
        // Given - test with future dates
        val futureTimestamp = 4102444800000L // January 1, 2100 00:00:00 UTC
        val date = Date(futureTimestamp)

        // When
        val result = CleverTapUtils.getClevertapDate(date)

        // Then
        val expectedSeconds = futureTimestamp / 1000
        assertEquals("\$D_$expectedSeconds", result)
    }


    @Test
    fun `test integration with real Date operations`() {
        // Given - create dates using various methods
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"), Locale.US)
        calendar.set(2023, Calendar.JANUARY, 15, 10, 30, 45)
        calendar.set(Calendar.MILLISECOND, 500)

        val dateFromCalendar = calendar.time
        val dateFromMillis = Date(calendar.timeInMillis)
        val sdf = SimpleDateFormat("dd/MM/yyyy/HH/mm/ss", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val dateSDF = sdf.parse("15/01/2023/10/30/45")
        // When
        val result1 = CleverTapUtils.getClevertapDate(dateFromCalendar)
        val result2 = CleverTapUtils.getClevertapDate(dateFromMillis)
        val result3 = CleverTapUtils.getClevertapDate(dateSDF!!)

        // Then - compare all results
        assertEquals("Calendar and millis should match", result1, result2)
        assertEquals("Calendar and SDF should match", result1, result3)

        assertTrue("Should start with prefix", result1.startsWith("\$D_"))

        // Verify the seconds calculation
        val expectedSeconds = calendar.timeInMillis / 1000
        val actualSecondsStr = result1.substring(3)
        assertEquals("Seconds should match", expectedSeconds, actualSecondsStr.toLong())
    }
}
