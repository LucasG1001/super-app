package tech.gomeslab.superapp.feature.workout.ui.active

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import tech.gomeslab.superapp.feature.workout.data.WorkoutRepository
import tech.gomeslab.superapp.feature.workout.domain.Exercise
import tech.gomeslab.superapp.feature.workout.domain.PreviousSets
import tech.gomeslab.superapp.feature.workout.domain.SetType
import tech.gomeslab.superapp.feature.workout.domain.Workout
import tech.gomeslab.superapp.feature.workout.domain.WorkoutSet
import tech.gomeslab.superapp.feature.workout.domain.WorkoutWithSets
import tech.gomeslab.superapp.feature.workout.rest.RestState
import tech.gomeslab.superapp.feature.workout.rest.RestTimer
import javax.inject.Inject

data class SetRow(val set: WorkoutSet, val label: String, val previous: WorkoutSet?)

data class ExerciseBlock(val order: Int, val exercise: Exercise, val rows: List<SetRow>)

data class ActiveWorkoutUiState(
    val loaded: Boolean = false,
    val workout: Workout? = null,
    val blocks: List<ExerciseBlock> = emptyList(),
) {
    val completedSets: Int get() = blocks.sumOf { b -> b.rows.count { it.set.completed } }
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ActiveWorkoutViewModel @Inject constructor(
    private val repository: WorkoutRepository,
    private val restTimer: RestTimer,
) : ViewModel() {

    private val current = repository.activeWorkout.flatMapLatest { active ->
        if (active == null) flowOf(null) else repository.observeWorkout(active.id)
    }

    val state: StateFlow<ActiveWorkoutUiState> = combine(
        current,
        repository.history,
        repository.exercisesById,
    ) { workout, history, exercises -> build(workout, history, exercises) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ActiveWorkoutUiState())

    val rest: StateFlow<RestState?> = restTimer.state

    private fun build(
        workout: WorkoutWithSets?,
        history: List<WorkoutWithSets>,
        exercises: Map<Long, Exercise>,
    ): ActiveWorkoutUiState {
        if (workout == null) return ActiveWorkoutUiState(loaded = true)
        val blocks = workout.sets.groupBy { it.exerciseOrder }.toSortedMap().mapNotNull { (order, sets) ->
            val exercise = exercises[sets.first().exerciseId] ?: return@mapNotNull null
            val previous = PreviousSets.forExercise(exercise.id, history, workout.workout.startedAt)
            var number = 0
            val rows = sets.sortedBy { it.setIndex }.mapIndexed { index, set ->
                val label = if (set.type == SetType.NORMAL) (++number).toString() else {
                    if (set.type != SetType.WARMUP) number++
                    set.type.short
                }
                SetRow(set, label, previous.getOrNull(index))
            }
            ExerciseBlock(order, exercise, rows)
        }
        return ActiveWorkoutUiState(loaded = true, workout = workout.workout, blocks = blocks)
    }

    fun setWeight(set: WorkoutSet, value: Double?) = launch { repository.updateWeight(set.id, value) }

    fun setReps(set: WorkoutSet, value: Int?) = launch { repository.updateReps(set.id, value) }

    fun setDuration(set: WorkoutSet, value: Int?) = launch { repository.updateDuration(set.id, value) }

    fun setType(set: WorkoutSet, type: SetType) = launch { repository.updateType(set.id, type) }

    fun copyPrevious(row: SetRow) = launch {
        val previous = row.previous ?: return@launch
        repository.updateWeight(row.set.id, previous.weightKg)
        repository.updateReps(row.set.id, previous.reps)
        repository.updateDuration(row.set.id, previous.durationSec)
    }

    /** Marca/desmarca a série. Ao marcar, completa campos vazios com o "anterior" e inicia o descanso. */
    fun toggle(block: ExerciseBlock, row: SetRow) = launch {
        val set = row.set
        if (set.completed) {
            repository.updateSet(set.copy(completed = false, completedAt = null))
            return@launch
        }
        repository.updateSet(
            set.copy(
                weightKg = set.weightKg ?: row.previous?.weightKg,
                reps = set.reps ?: row.previous?.reps,
                durationSec = set.durationSec ?: row.previous?.durationSec,
                completed = true,
                completedAt = System.currentTimeMillis(),
            ),
        )
        val next = block.rows.firstOrNull { it.set.setIndex > set.setIndex }
        if (next?.set?.type != SetType.DROP) restTimer.start(block.exercise.restSeconds, block.exercise.name)
    }

    fun addSet(block: ExerciseBlock) = launch {
        val workoutId = state.value.workout?.id ?: return@launch
        repository.addSet(workoutId, block.order, block.exercise.id)
    }

    fun removeSet(set: WorkoutSet) = launch { repository.deleteSet(set.id) }

    fun removeExercise(block: ExerciseBlock) = launch {
        val workoutId = state.value.workout?.id ?: return@launch
        repository.removeExercise(workoutId, block.order)
    }

    fun addExercise(exerciseId: Long) = launch {
        val workoutId = repository.getActiveWorkout()?.id ?: return@launch
        repository.addExercise(workoutId, exerciseId)
    }

    fun adjustRest(deltaSeconds: Int) = restTimer.adjust(deltaSeconds)

    fun skipRest() = restTimer.skip()

    fun finish(onFinished: (Long) -> Unit, onEmpty: () -> Unit) = launch {
        val workoutId = state.value.workout?.id ?: return@launch
        if (repository.finish(workoutId)) {
            restTimer.skip()
            onFinished(workoutId)
        } else {
            onEmpty()
        }
    }

    fun discard(onDone: () -> Unit) = launch {
        val workoutId = state.value.workout?.id ?: return@launch
        restTimer.skip()
        repository.deleteWorkout(workoutId)
        onDone()
    }

    private fun launch(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }
}
