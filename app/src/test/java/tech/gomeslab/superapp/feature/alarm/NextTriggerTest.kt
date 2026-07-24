package tech.gomeslab.superapp.feature.alarm

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import tech.gomeslab.superapp.feature.alarm.domain.Alarm
import tech.gomeslab.superapp.feature.alarm.domain.NextTrigger
import java.util.Calendar

class NextTriggerTest {

    private fun calendarAt(year: Int, month: Int, day: Int, hour: Int, minute: Int): Calendar =
        Calendar.getInstance().apply {
            clear()
            set(year, month, day, hour, minute, 0)
        }

    @Test
    fun `one-time in the future today stays today`() {
        // Quarta, 2026-07-01 08:00 -> alarme 09:30 hoje.
        val now = calendarAt(2026, Calendar.JULY, 1, 8, 0)
        val trigger = NextTrigger.compute(now, hour = 9, minute = 30, daysOfWeek = 0)

        val result = Calendar.getInstance().apply { timeInMillis = trigger }
        assertEquals(1, result.get(Calendar.DAY_OF_MONTH))
        assertEquals(9, result.get(Calendar.HOUR_OF_DAY))
        assertEquals(30, result.get(Calendar.MINUTE))
    }

    @Test
    fun `one-time already passed today rolls to tomorrow`() {
        val now = calendarAt(2026, Calendar.JULY, 1, 10, 0)
        val trigger = NextTrigger.compute(now, hour = 9, minute = 30, daysOfWeek = 0)

        val result = Calendar.getInstance().apply { timeInMillis = trigger }
        assertEquals(2, result.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun `repeating picks the next matching weekday`() {
        // 2026-07-01 é uma quarta-feira. Alarme só às sextas às 07:00.
        val now = calendarAt(2026, Calendar.JULY, 1, 8, 0)
        val onlyFriday = Alarm.maskOf(Alarm.FRIDAY)
        val trigger = NextTrigger.compute(now, hour = 7, minute = 0, daysOfWeek = onlyFriday)

        val result = Calendar.getInstance().apply { timeInMillis = trigger }
        assertEquals(Calendar.FRIDAY, result.get(Calendar.DAY_OF_WEEK))
        assertEquals(3, result.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun `repeating today but time passed goes to next week same day`() {
        // Quarta 2026-07-01 10:00; alarme às quartas 09:00 -> próxima quarta (dia 8).
        val now = calendarAt(2026, Calendar.JULY, 1, 10, 0)
        val onlyWednesday = Alarm.maskOf(Alarm.WEDNESDAY)
        val trigger = NextTrigger.compute(now, hour = 9, minute = 0, daysOfWeek = onlyWednesday)

        val result = Calendar.getInstance().apply { timeInMillis = trigger }
        assertEquals(Calendar.WEDNESDAY, result.get(Calendar.DAY_OF_WEEK))
        assertEquals(8, result.get(Calendar.DAY_OF_MONTH))
        assertTrue(trigger > now.timeInMillis)
    }
}
