package tech.gomeslab.superapp.feature.reminder

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import tech.gomeslab.superapp.feature.reminder.domain.Agenda
import tech.gomeslab.superapp.feature.reminder.domain.AgendaStatus
import tech.gomeslab.superapp.feature.reminder.domain.LogStatus
import tech.gomeslab.superapp.feature.reminder.domain.Reminder
import tech.gomeslab.superapp.feature.reminder.domain.ReminderKind
import tech.gomeslab.superapp.feature.reminder.domain.ReminderLog
import tech.gomeslab.superapp.feature.reminder.domain.ReminderRepeat
import tech.gomeslab.superapp.feature.reminder.domain.ReminderSchedule
import tech.gomeslab.superapp.feature.reminder.domain.bit
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class ReminderScheduleTest {

    // 2026-07-01 é uma quarta-feira.
    private val today = LocalDate.of(2026, 7, 1)
    private fun at(hour: Int, minute: Int = 0, date: LocalDate = today) = date.atTime(hour, minute)

    private val medicine = Reminder(
        id = 1,
        kind = ReminderKind.MEDICINE,
        title = "Dipirona",
        startDate = today,
        times = listOf(LocalTime.of(20, 0), LocalTime.of(8, 0), LocalTime.of(14, 0)),
        repeat = ReminderRepeat.DAILY,
    )

    @Test
    fun `daily medicine picks the next time today`() {
        assertEquals(at(14), ReminderSchedule.nextOccurrence(at(9), medicine))
    }

    @Test
    fun `after the last dose it rolls to tomorrow morning`() {
        assertEquals(at(8, date = today.plusDays(1)), ReminderSchedule.nextOccurrence(at(21), medicine))
    }

    @Test
    fun `already handled occurrence is skipped`() {
        val next = ReminderSchedule.nextOccurrence(at(9), medicine, handled = setOf(at(14)))
        assertEquals(at(20), next)
    }

    @Test
    fun `treatment end date stops occurrences`() {
        val sevenDays = medicine.copy(endDate = today.plusDays(6))
        assertEquals(at(20, date = today.plusDays(6)), ReminderSchedule.nextOccurrence(at(19, date = today.plusDays(6)), sevenDays))
        assertNull(ReminderSchedule.nextOccurrence(at(21, date = today.plusDays(6)), sevenDays))
    }

    @Test
    fun `weekly reminder only on selected days`() {
        val mondays = Reminder(
            kind = ReminderKind.TASK,
            title = "Lixo reciclável",
            startDate = today,
            times = listOf(LocalTime.of(19, 0)),
            repeat = ReminderRepeat.WEEKLY,
            daysOfWeek = DayOfWeek.MONDAY.bit(),
        )
        assertEquals(at(19, date = LocalDate.of(2026, 7, 6)), ReminderSchedule.nextOccurrence(at(8), mondays))
    }

    @Test
    fun `event notifies ahead of time but returns the occurrence`() {
        val event = Reminder(
            kind = ReminderKind.EVENT,
            title = "Dentista",
            startDate = today,
            times = listOf(LocalTime.of(15, 0)),
            leadMinutes = 30,
        )
        assertEquals(at(15), ReminderSchedule.nextOccurrence(at(14, 20), event))
        assertNull(ReminderSchedule.nextOccurrence(at(14, 40), event))
    }

    @Test
    fun `one-shot in the past has no next occurrence`() {
        val past = Reminder(
            kind = ReminderKind.TASK,
            title = "Pagar boleto",
            startDate = today.minusDays(2),
            times = listOf(LocalTime.of(10, 0)),
        )
        assertNull(ReminderSchedule.nextOccurrence(at(9), past))
    }

    @Test
    fun `agenda marks overdue, taken and pending`() {
        val logs = listOf(ReminderLog(reminderId = 1, occurrence = at(8), status = LogStatus.TAKEN, actedAt = 0))
        val items = Agenda.forDate(today, now = at(15), reminders = listOf(medicine), logs = logs)
        assertEquals(
            listOf(AgendaStatus.TAKEN, AgendaStatus.OVERDUE, AgendaStatus.PENDING),
            items.map { it.status },
        )
        assertEquals(listOf(at(8), at(14), at(20)), items.map { it.occurrence })
    }

    @Test
    fun `refill is needed at or below the threshold`() {
        assertEquals(true, medicine.copy(stock = 5, refillThreshold = 5).needsRefill)
        assertEquals(false, medicine.copy(stock = 6, refillThreshold = 5).needsRefill)
        assertEquals(false, medicine.copy(stock = 2).needsRefill)
    }
}
