package tech.gomeslab.superapp.feature.workout.ui.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import tech.gomeslab.superapp.core.ui.components.ExtendedScaffold
import tech.gomeslab.superapp.core.ui.theme.AccentGreen
import tech.gomeslab.superapp.core.ui.theme.AccentOrange
import tech.gomeslab.superapp.core.ui.theme.AccentPurple
import tech.gomeslab.superapp.core.ui.theme.ControlActive
import tech.gomeslab.superapp.core.ui.theme.OnPrimary
import tech.gomeslab.superapp.core.ui.theme.Sizes
import tech.gomeslab.superapp.core.ui.theme.Spacing
import tech.gomeslab.superapp.core.ui.components.SectionHeader
import tech.gomeslab.superapp.feature.workout.domain.Routine
import tech.gomeslab.superapp.feature.workout.domain.WeekSummary
import tech.gomeslab.superapp.feature.workout.ui.countLabel
import tech.gomeslab.superapp.feature.workout.ui.durationLabel
import java.time.LocalDate

private val WEEK_INITIALS = listOf("D", "S", "T", "Q", "Q", "S", "S")

@Composable
fun WorkoutHomeScreen(
    onBack: () -> Unit,
    onOpenActive: () -> Unit,
    onEditRoutine: (Long?) -> Unit,
    onOpenHistory: () -> Unit,
    onOpenExercises: () -> Unit,
    onOpenMeasurements: () -> Unit,
    viewModel: WorkoutHomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val subtitle = when {
        state.active != null -> "Treino em andamento"
        state.nextRoutine != null -> "Próximo: ${state.nextRoutine?.name}"
        else -> "Crie sua primeira ficha"
    }

    ExtendedScaffold(
        title = "Treino",
        subtitle = subtitle,
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
            }
        },
        actions = {
            IconButton(onClick = { onEditRoutine(null) }) {
                Icon(Icons.Filled.Add, contentDescription = "Nova ficha")
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
            item(key = "start") {
                val active = state.active
                if (active != null) {
                    StartCard(
                        title = "Continuar ${active.name}",
                        subtitle = "Em andamento há ${durationLabel(state.now - active.startedAt)}",
                        icon = Icons.Filled.Timer,
                        onClick = onOpenActive,
                    )
                } else {
                    val next = state.nextRoutine
                    StartCard(
                        title = if (next != null) "Começar ${next.name}" else "Começar treino livre",
                        subtitle = next?.let { exercisesSummary(it) } ?: "Adicione exercícios durante o treino",
                        icon = Icons.Filled.PlayArrow,
                        onClick = { viewModel.start(next, onOpenActive) },
                    )
                }
            }
            item(key = "week") { WeekStrip(today = state.today, trained = state.trainedThisWeek) }
            item(key = "shortcuts") {
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    Shortcut("Histórico", Icons.Filled.History, AccentOrange, onOpenHistory, Modifier.weight(1f))
                    Shortcut("Exercícios", Icons.Filled.FitnessCenter, AccentPurple, onOpenExercises, Modifier.weight(1f))
                    Shortcut("Medidas", Icons.Filled.MonitorWeight, AccentGreen, onOpenMeasurements, Modifier.weight(1f))
                }
            }
            item(key = "routinesHeader") { SectionHeader("Minhas fichas") }
            if (state.routines.isEmpty()) {
                item(key = "routinesEmpty") {
                    Text(
                        text = "Nenhuma ficha ainda. Toque em + para montar o Treino A.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.md),
                    )
                }
            }
            items(state.routines, key = { it.id }) { routine ->
                RoutineCard(
                    routine = routine,
                    isNext = routine.id == state.nextRoutine?.id,
                    canStart = state.active == null,
                    onStart = { viewModel.start(routine, onOpenActive) },
                    onEdit = { onEditRoutine(routine.id) },
                    onDelete = { viewModel.deleteRoutine(routine.id) },
                    modifier = Modifier.animateItem(),
                )
            }
            if (state.active == null && state.routines.isNotEmpty()) {
                item(key = "free") {
                    Text(
                        text = "Começar treino livre",
                        style = MaterialTheme.typography.labelLarge,
                        color = ControlActive,
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable { viewModel.start(null, onOpenActive) }
                            .padding(horizontal = Spacing.sm, vertical = Spacing.md),
                    )
                }
            }
        }
    }
}

private fun exercisesSummary(routine: Routine): String =
    if (routine.exercises.isEmpty()) "Sem exercícios" else
        routine.exercises.joinToString(" · ") { it.exercise.name }

@Composable
private fun StartCard(title: String, subtitle: String, icon: ImageVector, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        color = ControlActive,
        contentColor = OnPrimary,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(Spacing.lg),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.headlineMedium)
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Box(
                modifier = Modifier
                    .size(Sizes.iconButton)
                    .clip(CircleShape)
                    .background(OnPrimary),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = ControlActive)
            }
        }
    }
}

@Composable
private fun WeekStrip(today: LocalDate, trained: Set<LocalDate>) {
    val start = WeekSummary.weekStart(today)
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Text(
                text = when (trained.size) {
                    0 -> "Nenhum treino esta semana"
                    1 -> "1 treino esta semana"
                    else -> "${trained.size} treinos esta semana"
                },
                style = MaterialTheme.typography.titleMedium,
            )
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                (0..6).forEach { offset ->
                    val day = start.plusDays(offset.toLong())
                    val done = day in trained
                    val isToday = day == today
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                        Text(
                            text = WEEK_INITIALS[offset],
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isToday) ControlActive else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Box(
                            modifier = Modifier
                                .size(Sizes.dayCircle)
                                .clip(CircleShape)
                                .background(if (done) AccentGreen else MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = day.dayOfMonth.toString(),
                                style = MaterialTheme.typography.labelLarge,
                                color = if (done) OnPrimary else MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Shortcut(label: String, icon: ImageVector, color: Color, onClick: () -> Unit, modifier: Modifier) {
    Surface(onClick = onClick, shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surface, modifier = modifier) {
        Column(
            modifier = Modifier.padding(vertical = Spacing.md),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Box(
                modifier = Modifier
                    .size(Sizes.categoryIcon)
                    .clip(CircleShape)
                    .background(color),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = OnPrimary, modifier = Modifier.size(Sizes.smallIcon))
            }
            Text(text = label, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RoutineCard(
    routine: Routine,
    isNext: Boolean,
    canStart: Boolean,
    onStart: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    var menuOpen by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier
                    .combinedClickable(
                        onClick = onEdit,
                        onLongClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            menuOpen = true
                        },
                    )
                    .padding(Spacing.md),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        Text(text = routine.name, style = MaterialTheme.typography.titleLarge)
                        if (isNext) {
                            Text(
                                text = "Próximo",
                                style = MaterialTheme.typography.labelMedium,
                                color = OnPrimary,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(ControlActive)
                                    .padding(horizontal = Spacing.sm, vertical = Spacing.xxs),
                            )
                        }
                    }
                    Text(
                        text = "${countLabel(routine.exercises.size, "exercício", "exercícios")} · ${exercisesSummary(routine)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (canStart) {
                    IconButton(onClick = onStart) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = "Começar ${routine.name}", tint = ControlActive)
                    }
                }
            }
        }
        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            DropdownMenuItem(text = { Text("Editar ficha") }, onClick = { menuOpen = false; onEdit() })
            DropdownMenuItem(
                text = { Text("Excluir ficha", color = MaterialTheme.colorScheme.error) },
                onClick = { menuOpen = false; onDelete() },
            )
        }
    }
}
