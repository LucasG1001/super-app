package tech.gomeslab.superapp.feature.workout.ui.exercise

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import tech.gomeslab.superapp.core.navigation.Routes
import tech.gomeslab.superapp.feature.workout.data.WorkoutRepository
import tech.gomeslab.superapp.feature.workout.domain.Equipment
import tech.gomeslab.superapp.feature.workout.domain.Exercise
import tech.gomeslab.superapp.feature.workout.domain.ExerciseBests
import tech.gomeslab.superapp.feature.workout.domain.ExerciseKind
import tech.gomeslab.superapp.feature.workout.domain.MuscleGroup
import tech.gomeslab.superapp.feature.workout.domain.OneRepMax
import tech.gomeslab.superapp.feature.workout.domain.PersonalRecords
import tech.gomeslab.superapp.feature.workout.domain.WorkoutSet
import java.text.Normalizer
import java.time.LocalDate
import javax.inject.Inject

data class ExerciseListUiState(
    val query: String = "",
    val group: MuscleGroup? = null,
    val exercises: List<Exercise> = emptyList(),
    val hasExactMatch: Boolean = false,
)

@HiltViewModel
class ExerciseListViewModel @Inject constructor(
    repository: WorkoutRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val pickMode: Boolean = savedStateHandle.get<Boolean>(Routes.EXERCISES_PICK_ARG) ?: false

    private val query = MutableStateFlow("")
    private val group = MutableStateFlow<MuscleGroup?>(null)

    val state: StateFlow<ExerciseListUiState> = combine(repository.exercises, query, group) { all, q, g ->
        val needle = normalize(q)
        ExerciseListUiState(
            query = q,
            group = g,
            exercises = all.filter { (g == null || it.muscleGroup == g) && normalize(it.name).contains(needle) },
            hasExactMatch = all.any { normalize(it.name) == needle },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ExerciseListUiState())

    fun setQuery(value: String) {
        query.value = value
    }

    fun setGroup(value: MuscleGroup?) {
        group.value = if (group.value == value) null else value
    }

    private fun normalize(text: String): String =
        Normalizer.normalize(text.trim().lowercase(), Normalizer.Form.NFD).replace(Regex("\\p{M}"), "")
}

data class ExerciseEditState(
    val id: Long = 0,
    val name: String = "",
    val muscleGroup: MuscleGroup = MuscleGroup.CHEST,
    val equipment: Equipment = Equipment.MACHINE,
    val kind: ExerciseKind = ExerciseKind.WEIGHT_REPS,
    val restSeconds: Int = Exercise.DEFAULT_REST_SECONDS,
) {
    val isNew: Boolean get() = id == 0L
    val canSave: Boolean get() = name.isNotBlank()
}

@HiltViewModel
class ExerciseEditViewModel @Inject constructor(
    private val repository: WorkoutRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val exerciseId = savedStateHandle.get<Long>(Routes.EXERCISE_EDIT_ID_ARG) ?: -1L
    private val _state = MutableStateFlow(
        ExerciseEditState(name = savedStateHandle.get<String>(Routes.EXERCISE_EDIT_NAME_ARG).orEmpty()),
    )
    val state: StateFlow<ExerciseEditState> = _state.asStateFlow()

    init {
        if (exerciseId != -1L) {
            viewModelScope.launch {
                repository.getExercise(exerciseId)?.let {
                    _state.value = ExerciseEditState(it.id, it.name, it.muscleGroup, it.equipment, it.kind, it.restSeconds)
                }
            }
        }
    }

    fun setName(value: String) = _state.update { it.copy(name = value) }
    fun setGroup(value: MuscleGroup) = _state.update { it.copy(muscleGroup = value) }
    fun setEquipment(value: Equipment) = _state.update { it.copy(equipment = value) }
    fun setKind(value: ExerciseKind) = _state.update { it.copy(kind = value) }
    fun setRest(value: Int) = _state.update { it.copy(restSeconds = value) }

    fun save(onSaved: (Long) -> Unit) {
        val s = _state.value
        if (!s.canSave) return
        viewModelScope.launch {
            val id = repository.saveExercise(
                Exercise(s.id, s.name.trim(), s.muscleGroup, s.equipment, s.kind, s.restSeconds),
            )
            onSaved(id)
        }
    }

    /** Exclui de vez se nunca foi usado; com histórico, arquiva (some da lista, o histórico fica). */
    fun remove(onDone: () -> Unit) {
        val id = _state.value.id
        if (id == 0L) return
        viewModelScope.launch {
            repository.removeExercise(id)
            onDone()
        }
    }
}

data class ExerciseSession(
    val workoutId: Long,
    val startedAt: Long,
    val date: LocalDate,
    val sets: List<WorkoutSet>,
    val bestOneRepMax: Double,
)

data class ExerciseDetailUiState(
    val exercise: Exercise? = null,
    val bests: ExerciseBests = ExerciseBests(),
    val sessions: List<ExerciseSession> = emptyList(),
)

@HiltViewModel
class ExerciseDetailViewModel @Inject constructor(
    repository: WorkoutRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val exerciseId = savedStateHandle.get<Long>(Routes.EXERCISE_DETAIL_ARG) ?: -1L

    val state: StateFlow<ExerciseDetailUiState> = combine(repository.exercisesById, repository.history) { exercises, history ->
        val sessions = history.mapNotNull { w ->
            val sets = w.sets.filter { it.exerciseId == exerciseId }.sortedBy { it.setIndex }
            if (sets.isEmpty()) return@mapNotNull null
            ExerciseSession(
                workoutId = w.workout.id,
                startedAt = w.workout.startedAt,
                date = java.time.Instant.ofEpochMilli(w.workout.startedAt).atZone(java.time.ZoneId.systemDefault()).toLocalDate(),
                sets = sets,
                bestOneRepMax = sets.filter { it.countsForStats }.maxOfOrNull { OneRepMax.epley(it.weightKg ?: 0.0, it.reps ?: 0) } ?: 0.0,
            )
        }
        ExerciseDetailUiState(
            exercise = exercises[exerciseId],
            bests = PersonalRecords.bests(sessions.flatMap { it.sets }),
            sessions = sessions,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ExerciseDetailUiState())
}
