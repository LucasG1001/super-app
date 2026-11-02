package tech.gomeslab.superapp.feature.reminder.ui.edit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import tech.gomeslab.superapp.core.navigation.Routes
import tech.gomeslab.superapp.feature.reminder.ReminderActions
import tech.gomeslab.superapp.feature.reminder.ReminderScheduler
import tech.gomeslab.superapp.feature.reminder.data.ReminderRepository
import tech.gomeslab.superapp.feature.reminder.domain.Reminder
import tech.gomeslab.superapp.feature.reminder.domain.ReminderKind
import tech.gomeslab.superapp.feature.reminder.domain.ReminderRepeat
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import javax.inject.Inject

data class ReminderEditState(
    val id: Long = 0,
    val kind: ReminderKind = ReminderKind.TASK,
    val title: String = "",
    val note: String = "",
    val date: LocalDate = LocalDate.now(),
    val times: List<LocalTime> = listOf(defaultTime()),
    val repeat: ReminderRepeat = ReminderRepeat.NONE,
    val daysOfWeek: Int = 0,
    val durationDays: Int? = null,
    val leadMinutes: Int = 0,
    val dose: String = "",
    val stock: String = "",
    val refillThreshold: String = "",
    val createdAt: Long = 0,
    val isNew: Boolean = true,
) {
    val canSave: Boolean get() = title.isNotBlank() && times.isNotEmpty()
}

private fun defaultTime(): LocalTime = LocalTime.now().plusHours(1).withMinute(0).withSecond(0).withNano(0)

@HiltViewModel
class ReminderEditViewModel @Inject constructor(
    private val repository: ReminderRepository,
    private val scheduler: ReminderScheduler,
    private val actions: ReminderActions,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val reminderId: Long = savedStateHandle.get<Long>(Routes.REMINDER_EDIT_ARG) ?: -1L

    private val _state = MutableStateFlow(ReminderEditState())
    val state: StateFlow<ReminderEditState> = _state.asStateFlow()

    init {
        if (reminderId != -1L) {
            viewModelScope.launch {
                repository.getById(reminderId)?.let { reminder ->
                    _state.value = ReminderEditState(
                        id = reminder.id,
                        kind = reminder.kind,
                        title = reminder.title,
                        note = reminder.note,
                        date = reminder.startDate,
                        times = reminder.times.sorted(),
                        repeat = reminder.repeat,
                        daysOfWeek = reminder.daysOfWeek,
                        durationDays = reminder.endDate?.let {
                            ChronoUnit.DAYS.between(reminder.startDate, it).toInt() + 1
                        },
                        leadMinutes = reminder.leadMinutes,
                        dose = reminder.dose,
                        stock = reminder.stock?.toString().orEmpty(),
                        refillThreshold = reminder.refillThreshold?.toString().orEmpty(),
                        createdAt = reminder.createdAt,
                        isNew = false,
                    )
                }
            }
        }
    }

    fun setKind(kind: ReminderKind) = _state.update {
        when (kind) {
            ReminderKind.MEDICINE -> it.copy(kind = kind, repeat = ReminderRepeat.DAILY, leadMinutes = 0)
            ReminderKind.EVENT -> it.copy(
                kind = kind,
                repeat = ReminderRepeat.NONE,
                times = it.times.take(1),
                leadMinutes = if (it.kind == ReminderKind.EVENT) it.leadMinutes else DEFAULT_EVENT_LEAD,
            )
            ReminderKind.TASK -> it.copy(kind = kind, times = it.times.take(1), leadMinutes = 0)
        }
    }

    fun setTitle(value: String) = _state.update { it.copy(title = value) }
    fun setNote(value: String) = _state.update { it.copy(note = value) }
    fun setDose(value: String) = _state.update { it.copy(dose = value) }
    fun setStock(value: String) = _state.update { it.copy(stock = value.filter(Char::isDigit).take(4)) }
    fun setRefill(value: String) = _state.update { it.copy(refillThreshold = value.filter(Char::isDigit).take(4)) }
    fun setDate(value: LocalDate) = _state.update { it.copy(date = value) }
    fun setRepeat(value: ReminderRepeat) = _state.update { it.copy(repeat = value) }
    fun toggleDay(bit: Int) = _state.update { it.copy(daysOfWeek = it.daysOfWeek xor (1 shl bit)) }
    fun setDuration(days: Int?) = _state.update { it.copy(durationDays = days) }
    fun setLead(minutes: Int) = _state.update { it.copy(leadMinutes = minutes) }

    fun setSingleTime(time: LocalTime) = _state.update { it.copy(times = listOf(time)) }

    fun addTime(time: LocalTime) = _state.update { it.copy(times = (it.times + time).distinct().sorted()) }

    fun removeTime(time: LocalTime) = _state.update { it.copy(times = it.times - time) }

    /** Preenche os horários "a cada N horas" a partir do primeiro horário escolhido. */
    fun fillEvery(hours: Int) = _state.update { s ->
        val start = s.times.minOrNull() ?: LocalTime.of(8, 0)
        val count = 24 / hours
        s.copy(times = (0 until count).map { start.plusHours((it * hours).toLong()) }.distinct().sorted())
    }

    fun save(onDone: () -> Unit) {
        val s = _state.value
        if (!s.canSave) return
        viewModelScope.launch {
            val reminder = Reminder(
                id = s.id,
                kind = s.kind,
                title = s.title.trim(),
                note = s.note.trim(),
                startDate = s.date,
                endDate = if (s.kind == ReminderKind.MEDICINE) s.durationDays?.let { s.date.plusDays(it - 1L) } else null,
                times = s.times,
                repeat = s.repeat,
                daysOfWeek = if (s.repeat == ReminderRepeat.WEEKLY) s.daysOfWeek else 0,
                leadMinutes = if (s.kind == ReminderKind.EVENT) s.leadMinutes else 0,
                dose = if (s.kind == ReminderKind.MEDICINE) s.dose.trim() else "",
                stock = s.stock.toIntOrNull().takeIf { s.kind == ReminderKind.MEDICINE },
                refillThreshold = s.refillThreshold.toIntOrNull().takeIf { s.kind == ReminderKind.MEDICINE },
                enabled = true,
                createdAt = if (s.isNew) System.currentTimeMillis() else s.createdAt,
            )
            val id = repository.upsert(reminder)
            scheduler.cancel(id)
            scheduler.scheduleNext(reminder.copy(id = id))
            onDone()
        }
    }

    fun delete(onDone: () -> Unit) {
        val id = _state.value.id
        if (id == 0L) return
        viewModelScope.launch {
            actions.delete(id)
            onDone()
        }
    }

    private companion object {
        const val DEFAULT_EVENT_LEAD = 30
    }
}
