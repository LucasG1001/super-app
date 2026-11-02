package tech.gomeslab.superapp.feature.workout.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.hilt.navigation.compose.hiltViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import tech.gomeslab.superapp.core.navigation.Routes
import tech.gomeslab.superapp.core.ui.components.ExtendedScaffold
import tech.gomeslab.superapp.core.ui.components.FocusCard
import tech.gomeslab.superapp.core.ui.components.SectionHeader
import tech.gomeslab.superapp.core.ui.theme.AccentOrange
import tech.gomeslab.superapp.core.ui.theme.ControlActive
import tech.gomeslab.superapp.core.ui.theme.Spacing
import tech.gomeslab.superapp.feature.workout.data.WorkoutRepository
import tech.gomeslab.superapp.feature.workout.domain.Exercise
import tech.gomeslab.superapp.feature.workout.domain.PersonalRecord
import tech.gomeslab.superapp.feature.workout.domain.PersonalRecords
import tech.gomeslab.superapp.feature.workout.domain.RecordType
import tech.gomeslab.superapp.feature.workout.domain.WorkoutStats
import tech.gomeslab.superapp.feature.workout.domain.WorkoutTotals
import tech.gomeslab.superapp.feature.workout.domain.WorkoutWithSets
import tech.gomeslab.superapp.feature.workout.domain.formatDecimal
import tech.gomeslab.superapp.feature.workout.ui.durationLabel
import tech.gomeslab.superapp.feature.workout.ui.exercise.StatTile
import tech.gomeslab.superapp.feature.workout.ui.monthLabel
import tech.gomeslab.superapp.feature.workout.ui.relativeDay
import tech.gomeslab.superapp.feature.workout.ui.setSummary
import tech.gomeslab.superapp.feature.workout.ui.toLocalDate
import tech.gomeslab.superapp.feature.workout.ui.volumeLabel
import tech.gomeslab.superapp.feature.workout.ui.weightLabel
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class HistoryItem(val workout: WorkoutWithSets, val totals: WorkoutTotals, val records: Int)

@HiltViewModel
class HistoryViewModel @Inject constructor(repository: WorkoutRepository) : ViewModel() {
    val items: StateFlow<List<HistoryItem>> = combine(repository.history, repository.exercisesById) { history, _ ->
        history.map { w -> HistoryItem(w, WorkoutStats.totals(w), PersonalRecords.detect(w, history).size) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}

data class WorkoutDetailUiState(
    val workout: WorkoutWithSets? = null,
    val totals: WorkoutTotals? = null,
    val records: List<PersonalRecord> = emptyList(),
    val exercises: Map<Long, Exercise> = emptyMap(),
)

@HiltViewModel
class WorkoutDetailViewModel @Inject constructor(
    private val repository: WorkoutRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val workoutId = savedStateHandle.get<Long>(Routes.WORKOUT_DETAIL_ARG) ?: -1L
    val isSummary: Boolean = savedStateHandle.get<Boolean>(Routes.WORKOUT_DETAIL_SUMMARY_ARG) ?: false

    val state: StateFlow<WorkoutDetailUiState> = combine(
        repository.observeWorkout(workoutId),
        repository.history,
        repository.exercisesById,
    ) { workout, history, exercises ->
        if (workout == null) return@combine WorkoutDetailUiState()
        WorkoutDetailUiState(workout, WorkoutStats.totals(workout), PersonalRecords.detect(workout, history), exercises)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WorkoutDetailUiState())

    fun delete(onDone: () -> Unit) {
        viewModelScope.launch {
            repository.deleteWorkout(workoutId)
            onDone()
        }
    }
}

@Composable
fun HistoryScreen(onBack: () -> Unit, onOpen: (Long) -> Unit, viewModel: HistoryViewModel = hiltViewModel()) {
    val items by viewModel.items.collectAsStateWithLifecycle()
    ExtendedScaffold(
        title = "Histórico",
        subtitle = when (items.size) {
            0 -> null
            1 -> "1 treino"
            else -> "${items.size} treinos"
        },
        navigationIcon = {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar") }
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = Spacing.md,
                end = Spacing.md,
                top = innerPadding.calculateTopPadding() + Spacing.xs,
                bottom = innerPadding.calculateBottomPadding() + Spacing.xl,
            ),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            if (items.isEmpty()) {
                item {
                    Text(
                        text = "Nenhum treino finalizado ainda.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.md),
                    )
                }
            }
            items.groupBy { monthLabel(it.workout.workout.startedAt.toLocalDate()) }.forEach { (month, monthItems) ->
                item(key = "m-$month") { SectionHeader(month) }
                items(monthItems, key = { it.workout.workout.id }) { item ->
                    HistoryCard(item, onClick = { onOpen(item.workout.workout.id) })
                }
            }
        }
    }
}

@Composable
private fun HistoryCard(item: HistoryItem, onClick: () -> Unit) {
    val workout = item.workout.workout
    Surface(onClick = onClick, shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(Spacing.md), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = workout.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "${relativeDay(workout.startedAt.toLocalDate())} · ${durationLabel(item.totals.durationMillis)} · ${volumeLabel(item.totals.volumeKg)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (item.records > 0) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.EmojiEvents, contentDescription = "Recordes", tint = AccentOrange)
                    Text(text = item.records.toString(), style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

private val TIME = DateTimeFormatter.ofPattern("HH:mm")

@Composable
fun WorkoutDetailScreen(
    onBack: () -> Unit,
    onOpenExercise: (Long) -> Unit,
    viewModel: WorkoutDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = MaterialTheme.shapes.large,
            title = { Text("Excluir treino?") },
            text = { Text("O treino e suas séries serão apagados do histórico.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    viewModel.delete(onBack)
                }) { Text("Excluir", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancelar", color = ControlActive) } },
        )
    }
    val workout = state.workout ?: return
    val totals = state.totals ?: return
    val started = java.time.Instant.ofEpochMilli(workout.workout.startedAt).atZone(java.time.ZoneId.systemDefault())

    ExtendedScaffold(
        title = if (viewModel.isSummary) "Treino concluído!" else workout.workout.name,
        subtitle = "${if (viewModel.isSummary) workout.workout.name + " · " else ""}${relativeDay(started.toLocalDate())}, ${started.format(TIME)}",
        collapsedTitle = workout.workout.name,
        navigationIcon = {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar") }
        },
        actions = {
            IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Filled.Delete, contentDescription = "Excluir treino") }
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = Spacing.md,
                end = Spacing.md,
                top = innerPadding.calculateTopPadding() + Spacing.xs,
                bottom = innerPadding.calculateBottomPadding() + Spacing.xl,
            ),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            item(key = "stats") {
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    StatTile("Duração", durationLabel(totals.durationMillis), Modifier.weight(1f))
                    StatTile("Volume", volumeLabel(totals.volumeKg), Modifier.weight(1f))
                    StatTile("Séries", totals.sets.toString(), Modifier.weight(1f))
                }
            }
            if (state.records.isNotEmpty()) {
                item(key = "recordsHeader") { SectionHeader("Recordes pessoais") }
                item(key = "records") {
                    FocusCard {
                        Column(modifier = Modifier.padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                            state.records.forEach { record ->
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                                    Icon(Icons.Filled.EmojiEvents, contentDescription = null, tint = AccentOrange)
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = state.exercises[record.exerciseId]?.name.orEmpty(), style = MaterialTheme.typography.titleMedium)
                                        Text(
                                            text = "${record.type.label}: ${recordValue(record)}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            item(key = "exercisesHeader") { SectionHeader("Exercícios") }
            val blocks = workout.sets.groupBy { it.exerciseOrder }.toSortedMap().values.toList()
            items(blocks, key = { it.first().exerciseOrder }) { sets ->
                val exercise = state.exercises[sets.first().exerciseId] ?: return@items
                Surface(
                    onClick = { onOpenExercise(exercise.id) },
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                        Text(text = exercise.name, style = MaterialTheme.typography.titleMedium)
                        var number = 0
                        sets.sortedBy { it.setIndex }.forEach { set ->
                            val label = set.type.short.ifBlank { (++number).toString() }
                            Text(
                                text = "$label   ${setSummary(set, exercise.kind)}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun recordValue(record: PersonalRecord): String = when (record.type) {
    RecordType.MOST_REPS -> "${record.value.toInt()} repetições"
    RecordType.BEST_SET_VOLUME -> "${formatDecimal(record.value)} kg"
    else -> weightLabel(record.value)
}
