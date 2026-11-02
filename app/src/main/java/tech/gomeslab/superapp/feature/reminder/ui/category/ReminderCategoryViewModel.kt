package tech.gomeslab.superapp.feature.reminder.ui.category

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import tech.gomeslab.superapp.core.navigation.Routes
import tech.gomeslab.superapp.core.time.minuteTicker
import tech.gomeslab.superapp.feature.reminder.ReminderActions
import tech.gomeslab.superapp.feature.reminder.data.ReminderRepository
import tech.gomeslab.superapp.feature.reminder.domain.Agenda
import tech.gomeslab.superapp.feature.reminder.domain.AgendaItem
import tech.gomeslab.superapp.feature.reminder.domain.AgendaStatus
import tech.gomeslab.superapp.feature.reminder.domain.LogStatus
import tech.gomeslab.superapp.feature.reminder.domain.Reminder
import tech.gomeslab.superapp.feature.reminder.domain.ReminderKind
import tech.gomeslab.superapp.feature.reminder.domain.ReminderSchedule
import tech.gomeslab.superapp.feature.reminder.ui.home.ReminderCategory
import tech.gomeslab.superapp.feature.reminder.ui.label
import tech.gomeslab.superapp.feature.reminder.ui.relativeDayLabel
import tech.gomeslab.superapp.feature.reminder.ui.scheduleSummary
import tech.gomeslab.superapp.feature.reminder.ui.stockLabel
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject

sealed interface CategoryRow {
    val key: String

    data class AgendaEntry(val item: AgendaItem, val subtitle: String?) : CategoryRow {
        override val key = "a-${item.reminder.id}-${item.occurrence}"
    }

    data class Summary(val reminder: Reminder, val subtitle: String, val warning: String?) : CategoryRow {
        override val key = "s-${reminder.id}"
    }

    data class History(val reminder: Reminder, val occurrence: LocalDateTime, val subtitle: String) : CategoryRow {
        override val key = "h-${reminder.id}-$occurrence"
    }
}

data class CategoryUiState(
    val category: ReminderCategory,
    val rows: List<CategoryRow> = emptyList(),
)

@HiltViewModel
class ReminderCategoryViewModel @Inject constructor(
    repository: ReminderRepository,
    private val actions: ReminderActions,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val category: ReminderCategory = savedStateHandle.get<String>(Routes.REMINDER_CATEGORY_ARG)
        ?.let { name -> ReminderCategory.entries.firstOrNull { it.name == name } }
        ?: ReminderCategory.TODAY

    val state: StateFlow<CategoryUiState> = combine(
        repository.reminders,
        repository.logsSince(LocalDate.now().minusDays(HISTORY_DAYS).atStartOfDay()),
        minuteTicker(),
    ) { reminders, logs, nowMillis ->
        val now = LocalDateTime.ofInstant(Instant.ofEpochMilli(nowMillis), ZoneId.systemDefault())
        val today = now.toLocalDate()
        val rows: List<CategoryRow> = when (category) {
            ReminderCategory.TODAY -> Agenda.forDate(today, now, reminders, logs)
                .map { CategoryRow.AgendaEntry(it, null) }

            ReminderCategory.SCHEDULED -> reminders
                .mapNotNull { reminder ->
                    val handled = logs.filter { it.reminderId == reminder.id }.map { it.occurrence }.toSet()
                    ReminderSchedule.nextOccurrence(now, reminder, handled)?.let { reminder to it }
                }
                .sortedBy { it.second }
                .map { (reminder, next) ->
                    CategoryRow.AgendaEntry(
                        AgendaItem(reminder, next, AgendaStatus.PENDING),
                        "${relativeDayLabel(next.toLocalDate(), today)} · ${scheduleSummary(reminder)}",
                    )
                }

            ReminderCategory.MEDICINE -> reminders
                .filter { it.kind == ReminderKind.MEDICINE }
                .map { reminder ->
                    val status = if (reminder.enabled) scheduleSummary(reminder) else "Tratamento encerrado"
                    CategoryRow.Summary(reminder, status, stockLabel(reminder))
                }

            ReminderCategory.DONE -> logs
                .filter { it.status != LogStatus.SKIPPED }
                .sortedByDescending { it.occurrence }
                .mapNotNull { log ->
                    val reminder = reminders.firstOrNull { it.id == log.reminderId } ?: return@mapNotNull null
                    val verb = if (log.status == LogStatus.TAKEN) "Tomado" else "Concluído"
                    val day = relativeDayLabel(log.occurrence.toLocalDate(), today)
                    CategoryRow.History(reminder, log.occurrence, "$verb · $day, ${log.occurrence.toLocalTime().label()}")
                }
        }
        CategoryUiState(category, rows)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CategoryUiState(category))

    fun confirm(item: AgendaItem) {
        val status = if (item.reminder.kind == ReminderKind.MEDICINE) LogStatus.TAKEN else LogStatus.DONE
        viewModelScope.launch { actions.record(item.reminder.id, item.occurrence, status) }
    }

    fun undo(reminderId: Long, occurrence: LocalDateTime) {
        viewModelScope.launch { actions.undo(reminderId, occurrence) }
    }

    fun delete(reminderId: Long) {
        viewModelScope.launch { actions.delete(reminderId) }
    }

    private companion object {
        const val HISTORY_DAYS = 30L
    }
}
