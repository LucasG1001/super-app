package tech.gomeslab.superapp.feature.reminder.domain

import java.time.LocalDate
import java.time.LocalDateTime

object ReminderSchedule {

    private const val MAX_DAYS_AHEAD = 400L

    fun occursOn(date: LocalDate, reminder: Reminder): Boolean {
        if (date.isBefore(reminder.startDate)) return false
        val end = reminder.endDate
        if (end != null && date.isAfter(end)) return false
        return when (reminder.repeat) {
            ReminderRepeat.NONE -> date == reminder.startDate
            ReminderRepeat.DAILY -> true
            ReminderRepeat.WEEKLY ->
                reminder.daysOfWeek == 0 || (reminder.daysOfWeek and date.dayOfWeek.bit()) != 0
        }
    }

    fun occurrencesOn(date: LocalDate, reminder: Reminder): List<LocalDateTime> =
        if (occursOn(date, reminder)) reminder.times.sorted().map { date.atTime(it) } else emptyList()

    fun notifyAt(occurrence: LocalDateTime, reminder: Reminder): LocalDateTime =
        occurrence.minusMinutes(reminder.leadMinutes.toLong())

    /** Próxima ocorrência cujo aviso ainda não passou e que não foi registrada (tomada/pulada/feita). */
    fun nextOccurrence(
        now: LocalDateTime,
        reminder: Reminder,
        handled: Set<LocalDateTime> = emptySet(),
    ): LocalDateTime? {
        if (!reminder.enabled || reminder.times.isEmpty()) return null
        var date = maxOf(now.toLocalDate(), reminder.startDate)
        val limit = date.plusDays(MAX_DAYS_AHEAD)
        while (!date.isAfter(limit)) {
            val end = reminder.endDate
            if (end != null && date.isAfter(end)) return null
            val next = occurrencesOn(date, reminder).firstOrNull { occurrence ->
                notifyAt(occurrence, reminder).isAfter(now) && occurrence !in handled
            }
            if (next != null) return next
            if (reminder.repeat == ReminderRepeat.NONE) return null
            date = date.plusDays(1)
        }
        return null
    }
}
