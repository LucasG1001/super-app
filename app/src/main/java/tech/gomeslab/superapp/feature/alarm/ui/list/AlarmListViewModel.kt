package tech.gomeslab.superapp.feature.alarm.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import tech.gomeslab.superapp.feature.alarm.data.AlarmRepository
import tech.gomeslab.superapp.feature.alarm.domain.Alarm
import tech.gomeslab.superapp.feature.alarm.domain.AlarmScheduler
import javax.inject.Inject

@HiltViewModel
class AlarmListViewModel @Inject constructor(
    private val repository: AlarmRepository,
    private val scheduler: AlarmScheduler,
) : ViewModel() {

    val alarms: StateFlow<List<Alarm>> = repository.alarms
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setEnabled(alarm: Alarm, enabled: Boolean) {
        viewModelScope.launch {
            repository.setEnabled(alarm.id, enabled)
            val updated = alarm.copy(enabled = enabled)
            if (enabled) scheduler.schedule(updated) else scheduler.cancel(alarm.id)
        }
    }

    fun delete(alarm: Alarm) {
        viewModelScope.launch {
            scheduler.cancel(alarm.id)
            repository.delete(alarm.id)
        }
    }
}
