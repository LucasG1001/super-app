package tech.gomeslab.superapp.feature.alarm

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import tech.gomeslab.superapp.feature.alarm.domain.Alarm
import tech.gomeslab.superapp.feature.alarm.domain.NextAlarm
import tech.gomeslab.superapp.feature.alarm.ui.untilLabel
import java.util.Calendar

class NextAlarmTest {

    private fun calendarAt(year: Int, month: Int, day: Int, hour: Int, minute: Int): Calendar =
        Calendar.getInstance().apply {
            clear()
            set(year, month, day, hour, minute, 0)
        }

    private val now = calendarAt(2026, Calendar.JULY, 1, 8, 0)

    @Test
    fun `no alarms returns null`() {
        assertNull(NextAlarm.nextTriggerAt(now, emptyList()))
    }

    @Test
    fun `all disabled returns null`() {
        val alarms = listOf(Alarm(id = 1, hour = 9, minute = 0, enabled = false))
        assertNull(NextAlarm.nextTriggerAt(now, alarms))
    }

    @Test
    fun `picks the closest enabled alarm`() {
        val alarms = listOf(
            Alarm(id = 1, hour = 7, minute = 0),
            Alarm(id = 2, hour = 8, minute = 30),
            Alarm(id = 3, hour = 8, minute = 10, enabled = false),
        )
        val next = NextAlarm.nextTriggerAt(now, alarms)
        assertEquals(30 * 60_000L, next!! - now.timeInMillis)
    }

    @Test
    fun `repeating alarm on another day can be the closest`() {
        val onlyThursday = Alarm.maskOf(Alarm.THURSDAY)
        val alarms = listOf(
            Alarm(id = 1, hour = 6, minute = 0, daysOfWeek = onlyThursday),
            Alarm(id = 2, hour = 7, minute = 0),
        )
        val next = NextAlarm.nextTriggerAt(now, alarms)
        assertEquals(22 * 60 * 60_000L, next!! - now.timeInMillis)
    }

    @Test
    fun `until label formats hours and minutes`() {
        assertEquals("Alarme em 7 h 12 min", untilLabel((7 * 60 + 12) * 60_000L))
        assertEquals("Alarme em 2 h", untilLabel(2 * 60 * 60_000L))
        assertEquals("Alarme em 1 d 3 h", untilLabel((27 * 60 + 5) * 60_000L))
        assertEquals("Alarme em menos de 1 minuto", untilLabel(30_000L))
    }
}
