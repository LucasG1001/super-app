package tech.gomeslab.superapp.feature.workout.ui.exercise

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import tech.gomeslab.superapp.core.ui.components.ChartPoint
import tech.gomeslab.superapp.core.ui.components.ChoiceDialog
import tech.gomeslab.superapp.core.ui.components.EditorScaffold
import tech.gomeslab.superapp.core.ui.components.ExtendedScaffold
import tech.gomeslab.superapp.core.ui.components.FocusCard
import tech.gomeslab.superapp.core.ui.components.LineChart
import tech.gomeslab.superapp.core.ui.components.OptionDivider
import tech.gomeslab.superapp.core.ui.components.OptionRow
import tech.gomeslab.superapp.core.ui.components.PlainTextField
import tech.gomeslab.superapp.core.ui.components.SectionHeader
import tech.gomeslab.superapp.core.ui.theme.ControlActive
import tech.gomeslab.superapp.core.ui.theme.OnPrimary
import tech.gomeslab.superapp.core.ui.theme.Sizes
import tech.gomeslab.superapp.core.ui.theme.Spacing
import tech.gomeslab.superapp.feature.workout.domain.Equipment
import tech.gomeslab.superapp.feature.workout.domain.Exercise
import tech.gomeslab.superapp.feature.workout.domain.ExerciseKind
import tech.gomeslab.superapp.feature.workout.domain.MuscleGroup
import tech.gomeslab.superapp.feature.workout.domain.formatDecimal
import tech.gomeslab.superapp.feature.workout.ui.countLabel
import tech.gomeslab.superapp.feature.workout.ui.dayMonth
import tech.gomeslab.superapp.feature.workout.ui.restLabel
import tech.gomeslab.superapp.feature.workout.ui.setSummary
import tech.gomeslab.superapp.feature.workout.ui.shortDate
import tech.gomeslab.superapp.feature.workout.ui.weightLabel

private val REST_OPTIONS = listOf(0, 30, 45, 60, 90, 120, 150, 180, 240, 300)

@Composable
fun ExerciseListScreen(
    onBack: () -> Unit,
    onPicked: (Long) -> Unit,
    onOpen: (Long) -> Unit,
    onCreate: (String) -> Unit,
    viewModel: ExerciseListViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ExtendedScaffold(
        title = if (viewModel.pickMode) "Adicionar exercício" else "Exercícios",
        subtitle = countLabel(state.exercises.size, "exercício", "exercícios"),
        navigationIcon = {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar") }
        },
        actions = {
            IconButton(onClick = { onCreate(state.query) }) { Icon(Icons.Filled.Add, contentDescription = "Novo exercício") }
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
            item(key = "search") { SearchField(state.query, viewModel::setQuery) }
            item(key = "groups") {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    items(MuscleGroup.entries) { group ->
                        Pill(label = group.label, selected = state.group == group, onClick = { viewModel.setGroup(group) })
                    }
                }
            }
            if (state.query.isNotBlank() && !state.hasExactMatch) {
                item(key = "create") {
                    Surface(
                        onClick = { onCreate(state.query.trim()) },
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(modifier = Modifier.padding(Spacing.md), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Add, contentDescription = null, tint = ControlActive)
                            Text(
                                text = "Criar \"${state.query.trim()}\"",
                                style = MaterialTheme.typography.titleMedium,
                                color = ControlActive,
                                modifier = Modifier.padding(start = Spacing.md),
                            )
                        }
                    }
                }
            }
            if (state.exercises.isEmpty() && state.query.isBlank()) {
                item(key = "empty") {
                    Text(
                        text = "Nenhum exercício ainda. Busque pelo nome ou toque em + para criar.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.md),
                    )
                }
            }
            items(state.exercises, key = { it.id }) { exercise ->
                ExerciseRow(
                    exercise = exercise,
                    onClick = { if (viewModel.pickMode) onPicked(exercise.id) else onOpen(exercise.id) },
                    modifier = Modifier.animateItem(),
                )
            }
        }
    }
}

@Composable
private fun SearchField(value: String, onChange: (String) -> Unit) {
    TextField(
        value = value,
        onValueChange = onChange,
        placeholder = { Text("Buscar ou criar exercício") },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        singleLine = true,
        shape = CircleShape,
        modifier = Modifier.fillMaxWidth(),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            cursorColor = ControlActive,
        ),
    )
}

@Composable
fun Pill(label: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelLarge,
        color = if (selected) OnPrimary else MaterialTheme.colorScheme.onSurface,
        modifier = Modifier
            .clip(CircleShape)
            .background(if (selected) ControlActive else MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
    )
}

@Composable
fun MuscleBadge(group: MuscleGroup) {
    Box(
        modifier = Modifier
            .size(Sizes.categoryIcon)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = group.label.take(2), style = MaterialTheme.typography.labelLarge, color = ControlActive)
    }
}

@Composable
private fun ExerciseRow(exercise: Exercise, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(onClick = onClick, shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surface, modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            MuscleBadge(exercise.muscleGroup)
            Column(modifier = Modifier.weight(1f)) {
                Text(text = exercise.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    text = "${exercise.muscleGroup.label} · ${exercise.equipment.label}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private enum class ExerciseDialog { GROUP, EQUIPMENT, KIND, REST }

@Composable
fun ExerciseEditScreen(
    onDone: () -> Unit,
    onSaved: (Long) -> Unit,
    viewModel: ExerciseEditViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var dialog by rememberSaveable { mutableStateOf<ExerciseDialog?>(null) }

    EditorScaffold(
        title = if (state.isNew) "Novo exercício" else "Editar exercício",
        canSave = state.canSave,
        onCancel = onDone,
        onSave = { viewModel.save(onSaved) },
    ) {
        FocusCard {
            PlainTextField(value = state.name, onValueChange = viewModel::setName, placeholder = "Nome (ex.: Supino reto)")
        }
        FocusCard {
            OptionRow(title = "Grupo muscular", subtitle = state.muscleGroup.label, onClick = { dialog = ExerciseDialog.GROUP })
            OptionDivider()
            OptionRow(title = "Equipamento", subtitle = state.equipment.label, onClick = { dialog = ExerciseDialog.EQUIPMENT })
            OptionDivider()
            OptionRow(title = "Registrar", subtitle = state.kind.label, onClick = { dialog = ExerciseDialog.KIND })
            OptionDivider()
            OptionRow(title = "Descanso entre séries", subtitle = restLabel(state.restSeconds), onClick = { dialog = ExerciseDialog.REST })
        }
        if (!state.isNew) {
            TextButton(onClick = { viewModel.remove(onDone) }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text("Excluir exercício", color = MaterialTheme.colorScheme.error)
            }
        }
    }

    val close = { dialog = null }
    when (dialog) {
        ExerciseDialog.GROUP -> ChoiceDialog("Grupo muscular", MuscleGroup.entries, state.muscleGroup, { it.label }, { viewModel.setGroup(it); close() }, close)
        ExerciseDialog.EQUIPMENT -> ChoiceDialog("Equipamento", Equipment.entries, state.equipment, { it.label }, { viewModel.setEquipment(it); close() }, close)
        ExerciseDialog.KIND -> ChoiceDialog("Registrar", ExerciseKind.entries, state.kind, { it.label }, { viewModel.setKind(it); close() }, close)
        ExerciseDialog.REST -> ChoiceDialog("Descanso", REST_OPTIONS, state.restSeconds, ::restLabel, { viewModel.setRest(it); close() }, close)
        null -> Unit
    }
}

@Composable
fun ExerciseDetailScreen(
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    viewModel: ExerciseDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val exercise = state.exercise
    ExtendedScaffold(
        title = exercise?.name.orEmpty(),
        subtitle = exercise?.let { "${it.muscleGroup.label} · ${it.equipment.label}" },
        navigationIcon = {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar") }
        },
        actions = {
            if (exercise != null) {
                IconButton(onClick = { onEdit(exercise.id) }) { Icon(Icons.Filled.Edit, contentDescription = "Editar exercício") }
            }
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
            if (state.sessions.isEmpty()) {
                item {
                    Text(
                        text = "Ainda não há treinos com este exercício.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.md),
                    )
                }
                return@LazyColumn
            }
            item(key = "bests") {
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    StatTile("Maior carga", weightLabel(state.bests.heaviestWeight), Modifier.weight(1f))
                    StatTile("1RM estimado", weightLabel(state.bests.bestOneRepMax), Modifier.weight(1f))
                }
            }
            item(key = "bests2") {
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    StatTile("Melhor série", "${formatDecimal(state.bests.bestSetVolume)} kg", Modifier.weight(1f))
                    StatTile("Mais repetições", state.bests.mostReps.toString(), Modifier.weight(1f))
                }
            }
            val chartPoints = state.sessions
                .filter { it.bestOneRepMax > 0 }
                .map { ChartPoint(it.startedAt.toDouble(), it.bestOneRepMax, dayMonth(it.date)) }
            if (chartPoints.size >= 2) {
                item(key = "chartHeader") { SectionHeader("1RM estimado por treino") }
                item(key = "chart") {
                    FocusCard {
                        LineChart(
                            points = chartPoints,
                            formatValue = { "${formatDecimal(it)} kg" },
                            description = "Evolução do 1RM estimado de ${exercise?.name}",
                            modifier = Modifier.padding(Spacing.md),
                        )
                    }
                }
            }
            item(key = "sessionsHeader") { SectionHeader("Treinos") }
            items(state.sessions, key = { it.workoutId }) { session ->
                FocusCard {
                    Column(modifier = Modifier.padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                        Text(text = shortDate(session.date), style = MaterialTheme.typography.titleMedium)
                        session.sets.forEach { set ->
                            Text(
                                text = "${set.type.short.ifBlank { (set.setIndex + 1).toString() }}   ${setSummary(set, exercise?.kind ?: ExerciseKind.WEIGHT_REPS)}",
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

@Composable
fun StatTile(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surface, modifier = modifier) {
        Column(modifier = Modifier.padding(Spacing.md)) {
            Text(text = label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = value, style = MaterialTheme.typography.titleLarge, maxLines = 1)
        }
    }
}
