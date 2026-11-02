package tech.gomeslab.superapp.feature.reminder.domain

import java.time.LocalDate
import java.time.LocalDateTime

enum class AgendaStatus { PENDING, OVERDUE, TAKEN, SKIPPED, DONE }

data class AgendaItem(
    val reminder: Reminder,
    val occurrence: LocalDateTime,
    val status: AgendaStatus,
) {
    val isOpen: Boolean get() = status == AgendaStatus.PENDING || status == AgendaStatus.OVERDUE
}

object Agenda {

    fun forDate(
        date: LocalDate,
        now: LocalDateTime,
        reminders: List<Reminder>,
        logs: List<ReminderLog>,
    ): List<AgendaItem> {
        val logIndex = logs.associateBy { it.reminderId to it.occurrence }
        return reminders
            .flatMap { reminder ->
                ReminderSchedule.occurrencesOn(date, reminder).mapNotNull { occurrence ->
                    val log = logIndex[reminder.id to occurrence]
                    if (!reminder.enabled && log == null) return@mapNotNull null
                    AgendaItem(reminder, occurrence, statusOf(log, occurrence, now))
                }
            }
            .sortedBy { it.occurrence }
    }

    fun forRange(
        from: LocalDate,
        days: Int,
        now: LocalDateTime,
        reminders: List<Reminder>,
        logs: List<ReminderLog>,
    ): List<AgendaItem> =
        (0 until days).flatMap { forDate(from.plusDays(it.toLong()), now, reminders, logs) }

    private fun statusOf(log: ReminderLog?, occurrence: LocalDateTime, now: LocalDateTime): AgendaStatus =
        when (log?.status) {
            LogStatus.TAKEN -> AgendaStatus.TAKEN
            LogStatus.SKIPPED -> AgendaStatus.SKIPPED
            LogStatus.DONE -> AgendaStatus.DONE
            null -> if (occurrence.isBefore(now)) AgendaStatus.OVERDUE else AgendaStatus.PENDING
        }
}
