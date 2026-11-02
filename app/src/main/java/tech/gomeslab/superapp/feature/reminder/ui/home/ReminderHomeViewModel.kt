package tech.gomeslab.superapp.feature.reminder.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import tech.gomeslab.superapp.core.holiday.Holiday
import tech.gomeslab.superapp.core.holiday.HolidayCalendar
import tech.gomeslab.superapp.core.holiday.IndaiatubaHolidays
import tech.gomeslab.superapp.core.time.minuteTicker
import tech.gomeslab.superapp.feature.reminder.ReminderActions
import tech.gomeslab.superapp.feature.reminder.data.ReminderRepository
import tech.gomeslab.superapp.feature.reminder.domain.Agenda
import tech.gomeslab.superapp.feature.reminder.domain.AgendaItem
import tech.gomeslab.superapp.feature.reminder.domain.LogStatus
import tech.gomeslab.superapp.feature.reminder.domain.ReminderKind
import tech.gomeslab.superapp.feature.reminder.domain.ReminderSchedule
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject

enum class ReminderCategory(val title: String) {
    TODAY("Hoje"),
    SCHEDULED("Agendados"),
    MEDICINE("Remédios"),
    DONE("Concluídos"),
}

data class ReminderHomeUiState(
    val today: LocalDate = LocalDate.now(),
    val todayItems: List<AgendaItem> = emptyList(),
    val upcoming: List<AgendaItem> = emptyList(),
    val counts: Map<ReminderCategory, Int> = emptyMap(),
    val nextHoliday: Holiday? = null,
) {
    val openToday: Int get() = todayItems.count { it.isOpen }
}

@HiltViewModel
class ReminderHomeViewModel @Inject constructor(
    repository: ReminderRepository,
    private val actions: ReminderActions,
) : ViewModel() {

    private val historyStart = LocalDate.now().minusDays(HISTORY_DAYS).atStartOfDay()

    val state: StateFlow<ReminderHomeUiState> = combine(
        repository.reminders,
        repository.logsSince(historyStart),
        minuteTicker(),
    ) { reminders, logs, nowMillis ->
        val now = LocalDateTime.ofInstant(Instant.ofEpochMilli(nowMillis), ZoneId.systemDefault())
        val today = now.toLocalDate()
        val todayItems = Agenda.forDate(today, now, reminders, logs)
        val upcoming = Agenda.forRange(today.plusDays(1), UPCOMING_DAYS, now, reminders, logs)
            .filter { it.isOpen }
            .take(UPCOMING_LIMIT)
        val scheduled = reminders.count { ReminderSchedule.nextOccurrence(now, it) != null }
        ReminderHomeUiState(
            today = today,
            todayItems = todayItems,
            upcoming = upcoming,
            counts = mapOf(
                ReminderCategory.TODAY to todayItems.size,
                ReminderCategory.SCHEDULED to scheduled,
                ReminderCategory.MEDICINE to reminders.count { it.kind == ReminderKind.MEDICINE && it.enabled },
                ReminderCategory.DONE to logs.count { it.status != LogStatus.SKIPPED },
            ),
            nextHoliday = HolidayCalendar
                .forRange(today, today.plusYears(1), IndaiatubaHolidays.rules)
                .firstOrNull { it.isDayOff },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReminderHomeUiState())

    fun confirm(item: AgendaItem) {
        val status = if (item.reminder.kind == ReminderKind.MEDICINE) LogStatus.TAKEN else LogStatus.DONE
        viewModelScope.launch { actions.record(item.reminder.id, item.occurrence, status) }
    }

    fun undo(item: AgendaItem) {
        viewModelScope.launch { actions.undo(item.reminder.id, item.occurrence) }
    }

    fun delete(reminderId: Long) {
        viewModelScope.launch { actions.delete(reminderId) }
    }

    private companion object {
        const val HISTORY_DAYS = 30L
        const val UPCOMING_DAYS = 7
        const val UPCOMING_LIMIT = 10
    }
}
