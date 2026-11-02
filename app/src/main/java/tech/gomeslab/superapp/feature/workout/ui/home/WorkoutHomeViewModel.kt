package tech.gomeslab.superapp.feature.workout.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import tech.gomeslab.superapp.core.time.minuteTicker
import tech.gomeslab.superapp.feature.workout.data.WorkoutRepository
import tech.gomeslab.superapp.feature.workout.domain.Routine
import tech.gomeslab.superapp.feature.workout.domain.RoutineRotation
import tech.gomeslab.superapp.feature.workout.domain.WeekSummary
import tech.gomeslab.superapp.feature.workout.domain.Workout
import java.time.LocalDate
import javax.inject.Inject

data class WorkoutHomeUiState(
    val today: LocalDate = LocalDate.now(),
    val now: Long = System.currentTimeMillis(),
    val routines: List<Routine> = emptyList(),
    val nextRoutine: Routine? = null,
    val active: Workout? = null,
    val trainedThisWeek: Set<LocalDate> = emptySet(),
    val totalWorkouts: Int = 0,
)

@HiltViewModel
class WorkoutHomeViewModel @Inject constructor(
    private val repository: WorkoutRepository,
) : ViewModel() {

    val state: StateFlow<WorkoutHomeUiState> = combine(
        repository.routines,
        repository.activeWorkout,
        repository.history,
        minuteTicker(),
    ) { routines, active, history, now ->
        val today = LocalDate.now()
        WorkoutHomeUiState(
            today = today,
            now = now,
            routines = routines,
            nextRoutine = RoutineRotation.next(routines, history.firstOrNull { it.workout.routineId != null }?.workout?.routineId),
            active = active,
            trainedThisWeek = WeekSummary.trainedDays(today, history.map { it.workout }),
            totalWorkouts = history.size,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WorkoutHomeUiState())

    fun start(routine: Routine?, onStarted: () -> Unit) {
        viewModelScope.launch {
            repository.startWorkout(routine)
            onStarted()
        }
    }

    fun deleteRoutine(id: Long) {
        viewModelScope.launch { repository.deleteRoutine(id) }
    }
}
