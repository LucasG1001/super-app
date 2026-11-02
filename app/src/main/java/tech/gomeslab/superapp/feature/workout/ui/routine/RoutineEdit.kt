package tech.gomeslab.superapp.feature.workout.ui.routine

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.hilt.navigation.compose.hiltViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import tech.gomeslab.superapp.core.navigation.Routes
import tech.gomeslab.superapp.core.ui.components.EditorScaffold
import tech.gomeslab.superapp.core.ui.components.FocusCard
import tech.gomeslab.superapp.core.ui.components.PlainTextField
import tech.gomeslab.superapp.core.ui.theme.ControlActive
import tech.gomeslab.superapp.core.ui.theme.Sizes
import tech.gomeslab.superapp.core.ui.theme.Spacing
import tech.gomeslab.superapp.feature.workout.data.WorkoutRepository
import tech.gomeslab.superapp.feature.workout.domain.Routine
import tech.gomeslab.superapp.feature.workout.domain.RoutineExercise
import tech.gomeslab.superapp.feature.workout.ui.exercise.MuscleBadge
import javax.inject.Inject

private const val MAX_SETS = 10

data class RoutineEditState(
    val id: Long = 0,
    val name: String = "",
    val position: Int = 0,
    val items: List<RoutineExercise> = emptyList(),
    val isNew: Boolean = true,
) {
    val canSave: Boolean get() = name.isNotBlank()
}

@HiltViewModel
class RoutineEditViewModel @Inject constructor(
    private val repository: WorkoutRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val routineId = savedStateHandle.get<Long>(Routes.ROUTINE_EDIT_ARG) ?: -1L
    private val _state = MutableStateFlow(RoutineEditState())
    val state: StateFlow<RoutineEditState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            if (routineId != -1L) {
                repository.getRoutine(routineId)?.let {
                    _state.value = RoutineEditState(it.id, it.name, it.position, it.exercises, isNew = false)
                }
            } else {
                val count = repository.routines.first().size
                _state.update { it.copy(name = "Treino ${'A' + count.coerceAtMost(25)}") }
            }
        }
    }

    fun setName(value: String) = _state.update { it.copy(name = value) }

    fun addExercise(exerciseId: Long) {
        viewModelScope.launch {
            val exercise = repository.getExercise(exerciseId) ?: return@launch
            _state.update { s -> s.copy(items = s.items + RoutineExercise(exercise = exercise, position = s.items.size)) }
        }
    }

    fun changeSets(index: Int, delta: Int) = updateItem(index) { it.copy(setCount = (it.setCount + delta).coerceIn(1, MAX_SETS)) }

    fun setReps(index: Int, value: String) = updateItem(index) { it.copy(targetReps = value.take(10)) }

    fun move(index: Int, delta: Int) = _state.update { s ->
        val target = index + delta
        if (target !in s.items.indices) return@update s
        val list = s.items.toMutableList()
        list.add(target, list.removeAt(index))
        s.copy(items = list)
    }

    fun remove(index: Int) = _state.update { s -> s.copy(items = s.items.filterIndexed { i, _ -> i != index }) }

    fun save(onDone: () -> Unit) {
        val s = _state.value
        if (!s.canSave) return
        viewModelScope.launch {
            repository.saveRoutine(Routine(id = s.id, name = s.name.trim(), position = s.position, exercises = s.items))
            onDone()
        }
    }

    private fun updateItem(index: Int, transform: (RoutineExercise) -> RoutineExercise) = _state.update { s ->
        s.copy(items = s.items.mapIndexed { i, item -> if (i == index) transform(item) else item })
    }
}

@Composable
fun RoutineEditScreen(
    pickedExerciseId: Long?,
    onPickedConsumed: () -> Unit,
    onAddExercise: () -> Unit,
    onDone: () -> Unit,
    viewModel: RoutineEditViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(pickedExerciseId) {
        if (pickedExerciseId != null) {
            viewModel.addExercise(pickedExerciseId)
            onPickedConsumed()
        }
    }

    EditorScaffold(
        title = if (state.isNew) "Nova ficha" else "Editar ficha",
        canSave = state.canSave,
        onCancel = onDone,
        onSave = { viewModel.save(onDone) },
    ) {
        FocusCard {
            PlainTextField(value = state.name, onValueChange = viewModel::setName, placeholder = "Nome (ex.: Treino A — Peito e tríceps)")
        }
        state.items.forEachIndexed { index, item ->
            FocusCard {
                Column(modifier = Modifier.padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                        MuscleBadge(item.exercise.muscleGroup)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "${index + 1}. ${item.exercise.name}", style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = item.exercise.muscleGroup.label,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        IconButton(onClick = { viewModel.move(index, -1) }, enabled = index > 0) {
                            Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "Subir")
                        }
                        IconButton(onClick = { viewModel.move(index, 1) }, enabled = index < state.items.lastIndex) {
                            Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Descer")
                        }
                        IconButton(onClick = { viewModel.remove(index) }) {
                            Icon(Icons.Filled.Close, contentDescription = "Remover")
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        Text("Séries", style = MaterialTheme.typography.bodyLarge)
                        IconButton(onClick = { viewModel.changeSets(index, -1) }) { Icon(Icons.Filled.Remove, contentDescription = "Menos séries") }
                        Text(
                            text = item.setCount.toString(),
                            style = MaterialTheme.typography.titleLarge,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.width(Sizes.smallIcon * 2),
                        )
                        IconButton(onClick = { viewModel.changeSets(index, 1) }) { Icon(Icons.Filled.Add, contentDescription = "Mais séries") }
                        TextField(
                            value = item.targetReps,
                            onValueChange = { viewModel.setReps(index, it) },
                            placeholder = { Text("Reps (8-12)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                            modifier = Modifier.weight(1f),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = ControlActive,
                                unfocusedIndicatorColor = MaterialTheme.colorScheme.outlineVariant,
                                cursorColor = ControlActive,
                            ),
                        )
                    }
                }
            }
        }
        Surface(
            onClick = onAddExercise,
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(modifier = Modifier.padding(Spacing.md), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Add, contentDescription = null, tint = ControlActive)
                Text(
                    text = "Adicionar exercício",
                    style = MaterialTheme.typography.titleMedium,
                    color = ControlActive,
                    modifier = Modifier.padding(start = Spacing.md),
                )
            }
        }
    }
}
