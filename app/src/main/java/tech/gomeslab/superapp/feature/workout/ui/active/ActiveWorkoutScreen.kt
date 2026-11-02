package tech.gomeslab.superapp.feature.workout.ui.active

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import tech.gomeslab.superapp.core.ui.theme.AccentGreen
import tech.gomeslab.superapp.core.ui.theme.AccentOrange
import tech.gomeslab.superapp.core.ui.theme.AccentPurple
import tech.gomeslab.superapp.core.ui.theme.ControlActive
import tech.gomeslab.superapp.core.ui.theme.OnPrimary
import tech.gomeslab.superapp.core.ui.theme.Sizes
import tech.gomeslab.superapp.core.ui.theme.Spacing
import tech.gomeslab.superapp.feature.workout.domain.ExerciseKind
import tech.gomeslab.superapp.feature.workout.domain.SetType
import tech.gomeslab.superapp.feature.workout.domain.formatDecimal
import tech.gomeslab.superapp.feature.workout.domain.parseDecimal
import tech.gomeslab.superapp.feature.workout.rest.RestState
import tech.gomeslab.superapp.feature.workout.ui.elapsedClock
import tech.gomeslab.superapp.feature.workout.ui.setSummary

private const val COMPLETED_ROW_ALPHA = 0.18f
private const val REST_STEP_SECONDS = 15
private const val TICK_MILLIS = 250L
private const val SENT_HISTORY = 20
private const val COL_SET = 0.7f
private const val COL_PREVIOUS = 1.7f
private const val COL_VALUE = 1.1f
private const val COL_CHECK = 0.8f

private enum class ActiveDialog { FINISH, EMPTY, DISCARD }

@Composable
fun ActiveWorkoutScreen(
    pickedExerciseId: Long?,
    onPickedConsumed: () -> Unit,
    onAddExercise: () -> Unit,
    onMinimize: () -> Unit,
    onFinished: (Long) -> Unit,
    onDiscarded: () -> Unit,
    viewModel: ActiveWorkoutViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val rest by viewModel.rest.collectAsStateWithLifecycle()
    var dialog by rememberSaveable { mutableStateOf<ActiveDialog?>(null) }

    LaunchedEffect(pickedExerciseId) {
        if (pickedExerciseId != null) {
            viewModel.addExercise(pickedExerciseId)
            onPickedConsumed()
        }
    }
    LaunchedEffect(state.loaded, state.workout) {
        if (state.loaded && state.workout == null) onDiscarded()
    }

    val workout = state.workout ?: return
    val elapsed by produceState(0L, workout.startedAt) {
        while (true) {
            value = System.currentTimeMillis() - workout.startedAt
            delay(1_000)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .imePadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .height(Sizes.topBarHeight)
                .padding(horizontal = Spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onMinimize) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Minimizar treino")
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(text = workout.name, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    text = "${elapsedClock(elapsed)} · ${if (state.completedSets == 1) "1 série" else "${state.completedSets} séries"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Surface(onClick = { dialog = ActiveDialog.FINISH }, shape = CircleShape, color = ControlActive, contentColor = OnPrimary) {
                Text(
                    text = "Finalizar",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm),
                )
            }
            Spacer(Modifier.size(Spacing.sm))
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(start = Spacing.md, end = Spacing.md, top = Spacing.sm, bottom = Spacing.xl),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            items(state.blocks, key = { it.order }) { block ->
                ExerciseCard(block = block, viewModel = viewModel, modifier = Modifier.animateItem())
            }
            item(key = "add") {
                Surface(onClick = onAddExercise, shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
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
            item(key = "discard") {
                TextButton(onClick = { dialog = ActiveDialog.DISCARD }, modifier = Modifier.fillMaxWidth()) {
                    Text("Descartar treino", color = MaterialTheme.colorScheme.error)
                }
            }
        }

        AnimatedVisibility(
            visible = rest != null,
            enter = slideInVertically { it },
            exit = slideOutVertically { it },
        ) {
            rest?.let { RestBar(it, onAdjust = viewModel::adjustRest, onSkip = viewModel::skipRest) }
        }
        Spacer(Modifier.windowInsetsPadding(WindowInsets.navigationBars))
    }

    when (dialog) {
        ActiveDialog.FINISH -> ConfirmDialog(
            title = "Finalizar treino?",
            text = "Séries não marcadas serão descartadas.",
            confirm = "Finalizar",
            onConfirm = {
                dialog = null
                viewModel.finish(onFinished = onFinished, onEmpty = { dialog = ActiveDialog.EMPTY })
            },
            onDismiss = { dialog = null },
        )
        ActiveDialog.EMPTY -> ConfirmDialog(
            title = "Nenhuma série concluída",
            text = "Marque ao menos uma série para salvar, ou descarte o treino.",
            confirm = "Descartar",
            destructive = true,
            onConfirm = {
                dialog = null
                viewModel.discard(onDiscarded)
            },
            onDismiss = { dialog = null },
        )
        ActiveDialog.DISCARD -> ConfirmDialog(
            title = "Descartar treino?",
            text = "Tudo o que foi registrado neste treino será apagado.",
            confirm = "Descartar",
            destructive = true,
            onConfirm = {
                dialog = null
                viewModel.discard(onDiscarded)
            },
            onDismiss = { dialog = null },
        )
        null -> Unit
    }
}

@Composable
private fun ConfirmDialog(
    title: String,
    text: String,
    confirm: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    destructive: Boolean = false,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.large,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirm, color = if (destructive) MaterialTheme.colorScheme.error else ControlActive)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar", color = ControlActive) }
        },
    )
}

@Composable
private fun ExerciseCard(block: ExerciseBlock, viewModel: ActiveWorkoutViewModel, modifier: Modifier = Modifier) {
    var menuOpen by remember { mutableStateOf(false) }
    val kind = block.exercise.kind
    Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surface, modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(vertical = Spacing.sm)) {
            Row(
                modifier = Modifier.padding(start = Spacing.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = block.exercise.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = ControlActive,
                    modifier = Modifier.weight(1f),
                )
                Box {
                    IconButton(onClick = { menuOpen = true }) { Icon(Icons.Filled.MoreVert, contentDescription = "Opções do exercício") }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text("Remover exercício", color = MaterialTheme.colorScheme.error) },
                            onClick = {
                                menuOpen = false
                                viewModel.removeExercise(block)
                            },
                        )
                    }
                }
            }
            Row(modifier = Modifier.padding(horizontal = Spacing.sm), verticalAlignment = Alignment.CenterVertically) {
                HeaderCell("SÉRIE", COL_SET)
                HeaderCell("ANTERIOR", COL_PREVIOUS)
                when (kind) {
                    ExerciseKind.WEIGHT_REPS -> {
                        HeaderCell("KG", COL_VALUE)
                        HeaderCell("REPS", COL_VALUE)
                    }
                    ExerciseKind.REPS -> HeaderCell("REPS", COL_VALUE * 2)
                    ExerciseKind.DURATION -> HeaderCell("SEG", COL_VALUE * 2)
                }
                HeaderCell("", COL_CHECK)
            }
            block.rows.forEach { row -> key(row.set.id) { SetRowView(block, row, viewModel) } }
            TextButton(onClick = { viewModel.addSet(block) }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.Add, contentDescription = null, tint = ControlActive, modifier = Modifier.size(Sizes.smallIcon))
                Text("  Adicionar série", color = ControlActive)
            }
        }
    }
}

@Composable
private fun RowScope.HeaderCell(text: String, weight: Float) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.weight(weight),
    )
}

@Composable
private fun SetRowView(block: ExerciseBlock, row: SetRow, viewModel: ActiveWorkoutViewModel) {
    val set = row.set
    val view = LocalView.current
    val background by animateColorAsState(
        targetValue = if (set.completed) AccentGreen.copy(alpha = COMPLETED_ROW_ALPHA) else Color.Transparent,
        label = "setRow",
    )
    var typeMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(background)
            .padding(horizontal = Spacing.sm, vertical = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.weight(COL_SET), contentAlignment = Alignment.Center) {
            Text(
                text = row.label,
                style = MaterialTheme.typography.titleMedium,
                color = when (set.type) {
                    SetType.WARMUP -> AccentOrange
                    SetType.DROP -> AccentPurple
                    SetType.FAILURE -> MaterialTheme.colorScheme.error
                    SetType.NORMAL -> MaterialTheme.colorScheme.onSurface
                },
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable { typeMenu = true }
                    .padding(horizontal = Spacing.md, vertical = Spacing.sm),
            )
            DropdownMenu(expanded = typeMenu, onDismissRequest = { typeMenu = false }) {
                SetType.entries.forEach { type ->
                    DropdownMenuItem(
                        text = { Text(type.label) },
                        onClick = {
                            typeMenu = false
                            viewModel.setType(set, type)
                        },
                    )
                }
                HorizontalDivider()
                DropdownMenuItem(
                    text = { Text("Remover série", color = MaterialTheme.colorScheme.error) },
                    onClick = {
                        typeMenu = false
                        viewModel.removeSet(set)
                    },
                )
            }
        }
        Text(
            text = row.previous?.let { setSummary(it, block.exercise.kind) } ?: "—",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(COL_PREVIOUS)
                .clip(CircleShape)
                .clickable(enabled = row.previous != null && !set.completed) { viewModel.copyPrevious(row) }
                .padding(vertical = Spacing.sm),
        )
        when (block.exercise.kind) {
            ExerciseKind.WEIGHT_REPS -> {
                NumberCell(
                    value = set.weightKg,
                    hint = row.previous?.weightKg,
                    decimal = true,
                    onChange = { viewModel.setWeight(set, it) },
                    modifier = Modifier.weight(COL_VALUE),
                )
                NumberCell(
                    value = set.reps?.toDouble(),
                    hint = row.previous?.reps?.toDouble(),
                    decimal = false,
                    onChange = { viewModel.setReps(set, it?.toInt()) },
                    modifier = Modifier.weight(COL_VALUE),
                )
            }
            ExerciseKind.REPS -> NumberCell(
                value = set.reps?.toDouble(),
                hint = row.previous?.reps?.toDouble(),
                decimal = false,
                onChange = { viewModel.setReps(set, it?.toInt()) },
                modifier = Modifier.weight(COL_VALUE * 2),
            )
            ExerciseKind.DURATION -> NumberCell(
                value = set.durationSec?.toDouble(),
                hint = row.previous?.durationSec?.toDouble(),
                decimal = false,
                onChange = { viewModel.setDuration(set, it?.toInt()) },
                modifier = Modifier.weight(COL_VALUE * 2),
            )
        }
        Box(modifier = Modifier.weight(COL_CHECK), contentAlignment = Alignment.Center) {
            val checkBackground by animateColorAsState(
                targetValue = if (set.completed) AccentGreen else MaterialTheme.colorScheme.surfaceVariant,
                label = "check",
            )
            Box(
                modifier = Modifier
                    .size(Sizes.categoryIcon)
                    .clip(MaterialTheme.shapes.small)
                    .background(checkBackground)
                    .clickable {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        viewModel.toggle(block, row)
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = if (set.completed) "Desmarcar série" else "Concluir série",
                    tint = if (set.completed) OnPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(Sizes.smallIcon),
                )
            }
        }
    }
}

@Composable
private fun NumberCell(
    value: Double?,
    hint: Double?,
    decimal: Boolean,
    onChange: (Double?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var text by remember { mutableStateOf(value?.let(::formatDecimal).orEmpty()) }
    var focused by remember { mutableStateOf(false) }
    // O banco devolve os valores com atraso; enquanto o campo está em foco, ecos do que já foi
    // digitado não podem sobrescrever o texto atual (senão apagar/corrigir "volta" o número).
    val sent = remember { ArrayDeque<Double?>() }
    LaunchedEffect(value) {
        val echo = focused && value in sent
        if (!echo && parseDecimal(text) != value) text = value?.let(::formatDecimal).orEmpty()
    }
    val textStyle = MaterialTheme.typography.titleMedium.copy(
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center,
    )
    BasicTextField(
        value = text,
        onValueChange = { input ->
            val filtered = input.filter { it.isDigit() || (decimal && (it == ',' || it == '.')) }.take(6)
            text = filtered
            val parsed = parseDecimal(filtered)
            sent.addLast(parsed)
            if (sent.size > SENT_HISTORY) sent.removeFirst()
            onChange(parsed)
        },
        singleLine = true,
        textStyle = textStyle,
        cursorBrush = SolidColor(ControlActive),
        keyboardOptions = KeyboardOptions(keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Number),
        modifier = modifier
            .padding(horizontal = Spacing.xs)
            .onFocusChanged { focused = it.isFocused },
        decorationBox = { inner ->
            Box(
                modifier = Modifier
                    .clip(MaterialTheme.shapes.small)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(vertical = Spacing.sm),
                contentAlignment = Alignment.Center,
            ) {
                if (text.isEmpty()) {
                    Text(
                        text = hint?.let(::formatDecimal) ?: "–",
                        style = textStyle.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                    )
                }
                inner()
            }
        },
    )
}

@Composable
private fun RestBar(rest: RestState, onAdjust: (Int) -> Unit, onSkip: () -> Unit) {
    val remaining by produceState(rest.endsAt - System.currentTimeMillis(), rest) {
        while (true) {
            value = (rest.endsAt - System.currentTimeMillis()).coerceAtLeast(0)
            delay(TICK_MILLIS)
        }
    }
    Surface(color = MaterialTheme.colorScheme.surface, shape = MaterialTheme.shapes.large, modifier = Modifier.padding(Spacing.sm)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.md, vertical = Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { (remaining.toFloat() / rest.totalMillis).coerceIn(0f, 1f) },
                    color = ControlActive,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.size(Sizes.iconButton),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(text = elapsedClock(remaining + 999), style = MaterialTheme.typography.headlineMedium)
                Text(
                    text = rest.exerciseName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            TextButton(onClick = { onAdjust(-REST_STEP_SECONDS) }, contentPadding = PaddingValues(horizontal = Spacing.sm)) {
                Text("−15", color = ControlActive)
            }
            TextButton(onClick = { onAdjust(REST_STEP_SECONDS) }, contentPadding = PaddingValues(horizontal = Spacing.sm)) {
                Text("+15", color = ControlActive)
            }
            Surface(onClick = onSkip, shape = CircleShape, color = ControlActive, contentColor = OnPrimary) {
                Text("Pular", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm))
            }
        }
    }
}
