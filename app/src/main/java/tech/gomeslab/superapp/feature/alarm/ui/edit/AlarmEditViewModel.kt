package tech.gomeslab.superapp.feature.alarm.ui.edit

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
import tech.gomeslab.superapp.feature.alarm.data.AlarmRepository
import tech.gomeslab.superapp.feature.alarm.domain.Alarm
import tech.gomeslab.superapp.feature.alarm.domain.AlarmScheduler
import java.util.Calendar
import javax.inject.Inject

data class AlarmEditState(
    val id: Long = 0,
    val hour: Int = 7,
    val minute: Int = 0,
    val label: String = "",
    val daysOfWeek: Int = 0,
    val vibrate: Boolean = true,
    val snoozeMinutes: Int = 5,
    val isNew: Boolean = true,
)

@HiltViewModel
class AlarmEditViewModel @Inject constructor(
    private val repository: AlarmRepository,
    private val scheduler: AlarmScheduler,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val alarmId: Long = savedStateHandle.get<Long>(Routes.ALARM_EDIT_ARG) ?: -1L

    private val _state = MutableStateFlow(defaultState())
    val state: StateFlow<AlarmEditState> = _state.asStateFlow()

    init {
        if (alarmId != -1L) {
            viewModelScope.launch {
                repository.getById(alarmId)?.let { alarm ->
                    _state.value = AlarmEditState(
                        id = alarm.id,
                        hour = alarm.hour,
                        minute = alarm.minute,
                        label = alarm.label,
                        daysOfWeek = alarm.daysOfWeek,
                        vibrate = alarm.vibrate,
                        snoozeMinutes = alarm.snoozeMinutes,
                        isNew = false,
                    )
                }
            }
        }
    }

    private fun defaultState(): AlarmEditState {
        val now = Calendar.getInstance()
        return AlarmEditState(
            hour = now.get(Calendar.HOUR_OF_DAY),
            minute = now.get(Calendar.MINUTE),
        )
    }

    fun setTime(hour: Int, minute: Int) = _state.update { it.copy(hour = hour, minute = minute) }

    fun setLabel(label: String) = _state.update { it.copy(label = label) }

    fun toggleDay(dayBitIndex: Int) = _state.update {
        it.copy(daysOfWeek = it.daysOfWeek xor (1 shl dayBitIndex))
    }

    fun setVibrate(vibrate: Boolean) = _state.update { it.copy(vibrate = vibrate) }

    fun setSnooze(minutes: Int) = _state.update { it.copy(snoozeMinutes = minutes) }

    fun save(onDone: () -> Unit) {
        viewModelScope.launch {
            val s = _state.value
            val existing = if (s.isNew) null else repository.getById(s.id)
            val alarm = Alarm(
                id = s.id,
                hour = s.hour,
                minute = s.minute,
                label = s.label.trim(),
                enabled = true,
                daysOfWeek = s.daysOfWeek,
                soundUri = existing?.soundUri,
                vibrate = s.vibrate,
                snoozeMinutes = s.snoozeMinutes,
                createdAt = existing?.createdAt ?: System.currentTimeMillis(),
            )
            val id = repository.upsert(alarm)
            scheduler.schedule(alarm.copy(id = id))
            onDone()
        }
    }
}
