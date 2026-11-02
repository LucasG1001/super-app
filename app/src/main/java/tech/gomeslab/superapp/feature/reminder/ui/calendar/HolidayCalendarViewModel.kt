package tech.gomeslab.superapp.feature.reminder.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import tech.gomeslab.superapp.core.holiday.Holiday
import tech.gomeslab.superapp.core.holiday.HolidayCalendar
import tech.gomeslab.superapp.core.holiday.IndaiatubaHolidays
import tech.gomeslab.superapp.feature.reminder.data.ReminderRepository
import tech.gomeslab.superapp.feature.reminder.domain.Agenda
import tech.gomeslab.superapp.feature.reminder.domain.AgendaItem
import tech.gomeslab.superapp.feature.reminder.domain.Reminder
import tech.gomeslab.superapp.feature.reminder.domain.ReminderLog
import tech.gomeslab.superapp.feature.reminder.domain.ReminderSchedule
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import javax.inject.Inject

data class CalendarData(
    val reminders: List<Reminder> = emptyList(),
    val logs: List<ReminderLog> = emptyList(),
)

@HiltViewModel
class HolidayCalendarViewModel @Inject constructor(
    repository: ReminderRepository,
) : ViewModel() {

    private val holidayCache = mutableMapOf<Int, List<Holiday>>()

    val data: StateFlow<CalendarData> = combine(
        repository.reminders,
        repository.logsSince(LocalDate.now().minusYears(1).atStartOfDay()),
    ) { reminders, logs -> CalendarData(reminders, logs) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CalendarData())

    fun holidaysOf(month: YearMonth): List<Holiday> =
        holidayCache.getOrPut(month.year) { HolidayCalendar.forYear(month.year, IndaiatubaHolidays.rules) }
            .filter { YearMonth.from(it.date) == month }

    fun reminderDays(month: YearMonth, reminders: List<Reminder>): Set<LocalDate> =
        (1..month.lengthOfMonth())
            .map { month.atDay(it) }
            .filter { day -> reminders.any { it.enabled && ReminderSchedule.occursOn(day, it) } }
            .toSet()

    fun agendaOf(day: LocalDate, data: CalendarData): List<AgendaItem> =
        Agenda.forDate(day, LocalDateTime.now(), data.reminders, data.logs)
}
