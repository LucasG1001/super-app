package tech.gomeslab.superapp.feature.reminder.domain

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

enum class ReminderKind { TASK, MEDICINE, EVENT }

enum class ReminderRepeat { NONE, DAILY, WEEKLY }

enum class LogStatus { TAKEN, SKIPPED, DONE }

data class Reminder(
    val id: Long = 0,
    val kind: ReminderKind,
    val title: String,
    val note: String = "",
    val startDate: LocalDate,
    val endDate: LocalDate? = null,
    val times: List<LocalTime>,
    val repeat: ReminderRepeat = ReminderRepeat.NONE,
    val daysOfWeek: Int = 0,
    val leadMinutes: Int = 0,
    val dose: String = "",
    val stock: Int? = null,
    val refillThreshold: Int? = null,
    val enabled: Boolean = true,
    val createdAt: Long = 0,
) {
    val needsRefill: Boolean
        get() = stock != null && refillThreshold != null && stock <= refillThreshold
}

data class ReminderLog(
    val id: Long = 0,
    val reminderId: Long,
    val occurrence: LocalDateTime,
    val status: LogStatus,
    val actedAt: Long,
)

/** Bit do dia no mesmo formato do despertador: Dom=0 … Sáb=6. */
fun DayOfWeek.bit(): Int = 1 shl (value % 7)
