package tech.gomeslab.superapp.feature.alarm.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import tech.gomeslab.superapp.feature.alarm.data.AlarmRepository
import tech.gomeslab.superapp.feature.alarm.domain.Alarm
import tech.gomeslab.superapp.feature.alarm.domain.AlarmScheduler
import tech.gomeslab.superapp.feature.alarm.domain.NextAlarm
import java.util.Calendar
import javax.inject.Inject

data class AlarmListUiState(
    val alarms: List<Alarm> = emptyList(),
    val now: Long = System.currentTimeMillis(),
    val nextTriggerAt: Long? = null,
    val selectionMode: Boolean = false,
    val selectedIds: Set<Long> = emptySet(),
)

@HiltViewModel
class AlarmListViewModel @Inject constructor(
    private val repository: AlarmRepository,
    private val scheduler: AlarmScheduler,
) : ViewModel() {

    private val minuteTicker = flow {
        while (true) {
            val now = System.currentTimeMillis()
            emit(now)
            delay(60_000 - now % 60_000)
        }
    }

    private val selection = MutableStateFlow<Set<Long>?>(null)

    val state: StateFlow<AlarmListUiState> =
        combine(repository.alarms, minuteTicker, selection) { alarms, now, selected ->
            val calendar = Calendar.getInstance().apply { timeInMillis = now }
            AlarmListUiState(
                alarms = alarms,
                now = now,
                nextTriggerAt = NextAlarm.nextTriggerAt(calendar, alarms),
                selectionMode = selected != null,
                selectedIds = selected.orEmpty().filterTo(mutableSetOf()) { id ->
                    alarms.any { it.id == id }
                },
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AlarmListUiState())

    fun setEnabled(alarm: Alarm, enabled: Boolean) {
        viewModelScope.launch {
            repository.setEnabled(alarm.id, enabled)
            val updated = alarm.copy(enabled = enabled)
            if (enabled) scheduler.schedule(updated) else scheduler.cancel(alarm.id)
        }
    }

    fun startSelection(initialId: Long? = null) {
        selection.value = setOfNotNull(initialId)
    }

    fun toggleSelection(id: Long) = selection.update { current ->
        val set = current.orEmpty()
        if (id in set) set - id else set + id
    }

    fun toggleSelectAll() {
        val all = state.value.alarms.map { it.id }.toSet()
        selection.update { current -> if (current.orEmpty().containsAll(all)) emptySet() else all }
    }

    fun clearSelection() {
        selection.value = null
    }

    fun deleteSelected() {
        val ids = selection.value.orEmpty()
        selection.value = null
        viewModelScope.launch {
            ids.forEach { id ->
                scheduler.cancel(id)
                repository.delete(id)
            }
        }
    }
}
