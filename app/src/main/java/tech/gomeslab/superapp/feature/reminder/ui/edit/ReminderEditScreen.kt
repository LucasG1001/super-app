package tech.gomeslab.superapp.feature.reminder.ui.edit

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import tech.gomeslab.superapp.core.ui.components.ChoiceDialog
import tech.gomeslab.superapp.core.ui.components.DatePickerDialogOneUi
import tech.gomeslab.superapp.core.ui.components.DayCircle
import tech.gomeslab.superapp.core.ui.components.FocusCard
import tech.gomeslab.superapp.core.ui.components.OptionDivider
import tech.gomeslab.superapp.core.ui.components.OptionRow
import tech.gomeslab.superapp.core.ui.components.TimeWheelsDialog
import tech.gomeslab.superapp.core.ui.theme.ControlActive
import tech.gomeslab.superapp.core.ui.theme.OnPrimary
import tech.gomeslab.superapp.core.ui.theme.Sizes
import tech.gomeslab.superapp.core.ui.theme.Spacing
import tech.gomeslab.superapp.feature.reminder.domain.ReminderKind
import tech.gomeslab.superapp.feature.reminder.domain.ReminderRepeat
import tech.gomeslab.superapp.feature.reminder.ui.durationLabel
import tech.gomeslab.superapp.feature.reminder.ui.intervalLabel
import tech.gomeslab.superapp.feature.reminder.ui.kindLabel
import tech.gomeslab.superapp.feature.reminder.ui.label
import tech.gomeslab.superapp.feature.reminder.ui.leadLabel
import tech.gomeslab.superapp.feature.reminder.ui.relativeDayLabel
import java.time.LocalDate
import java.time.LocalTime

private val DAY_INITIALS = listOf("D", "S", "T", "Q", "Q", "S", "S")
private val LEAD_OPTIONS = listOf(0, 10, 30, 60, 24 * 60)
private val DURATION_OPTIONS: List<Int?> = listOf(null, 3, 5, 7, 10, 14, 30)
private val INTERVAL_OPTIONS = listOf(4, 6, 8, 12)
private const val DISABLED_ALPHA = 0.4f

private enum class EditDialog { DATE, SINGLE_TIME, ADD_TIME, REPEAT, LEAD, DURATION, INTERVAL }

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReminderEditScreen(
    onDone: () -> Unit,
    viewModel: ReminderEditViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var dialog by rememberSaveable { mutableStateOf<EditDialog?>(null) }
    val today = LocalDate.now()
    val isMedicine = state.kind == ReminderKind.MEDICINE

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.systemBars)
            .imePadding(),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.md)
                .animateContentSize(),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Spacer(Modifier.height(Spacing.lg))
            Text(
                text = if (state.isNew) "Novo lembrete" else "Editar lembrete",
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.padding(horizontal = Spacing.sm),
            )

            KindSelector(selected = state.kind, onSelect = viewModel::setKind)

            FocusCard {
                PlainField(
                    value = state.title,
                    onValueChange = viewModel::setTitle,
                    placeholder = when (state.kind) {
                        ReminderKind.TASK -> "O que lembrar?"
                        ReminderKind.MEDICINE -> "Nome do remédio"
                        ReminderKind.EVENT -> "Nome do evento"
                    },
                )
                AnimatedVisibility(visible = isMedicine) {
                    PlainField(
                        value = state.dose,
                        onValueChange = viewModel::setDose,
                        placeholder = "Dose (ex.: 1 comprimido)",
                    )
                }
            }

            FocusCard {
                OptionRow(
                    title = if (isMedicine) "Início" else "Data",
                    subtitle = relativeDayLabel(state.date, today),
                    onClick = { dialog = EditDialog.DATE },
                )
                OptionDivider()
                if (isMedicine) {
                    Text(
                        text = "Horários",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(start = Spacing.lg, top = Spacing.md),
                    )
                    FlowRow(
                        modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.sm),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                    ) {
                        state.times.forEach { time ->
                            TimeChip(label = time.label(), onRemove = { viewModel.removeTime(time) })
                        }
                        AddChip(onClick = { dialog = EditDialog.ADD_TIME })
                    }
                    OptionRow(
                        title = "Preencher a cada…",
                        subtitle = "A partir de ${state.times.minOrNull()?.label() ?: "08:00"}",
                        onClick = { dialog = EditDialog.INTERVAL },
                    )
                    OptionDivider()
                    OptionRow(
                        title = "Duração",
                        subtitle = durationLabel(state.durationDays),
                        onClick = { dialog = EditDialog.DURATION },
                    )
                } else {
                    OptionRow(
                        title = "Horário",
                        subtitle = state.times.firstOrNull()?.label() ?: "Escolher",
                        onClick = { dialog = EditDialog.SINGLE_TIME },
                    )
                    OptionDivider()
                    if (state.kind == ReminderKind.EVENT) {
                        OptionRow(
                            title = "Avisar",
                            subtitle = leadLabel(state.leadMinutes),
                            onClick = { dialog = EditDialog.LEAD },
                        )
                    } else {
                        OptionRow(
                            title = "Repetir",
                            subtitle = repeatOptionLabel(state.repeat),
                            onClick = { dialog = EditDialog.REPEAT },
                        )
                        AnimatedVisibility(visible = state.repeat == ReminderRepeat.WEEKLY) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = Spacing.md, vertical = Spacing.sm),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                            ) {
                                DAY_INITIALS.forEachIndexed { bit, initial ->
                                    DayCircle(
                                        label = initial,
                                        selected = (state.daysOfWeek and (1 shl bit)) != 0,
                                        isSunday = bit == 0,
                                        onClick = { viewModel.toggleDay(bit) },
                                    )
                                }
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(visible = isMedicine) {
                FocusCard {
                    PlainField(
                        value = state.stock,
                        onValueChange = viewModel::setStock,
                        placeholder = "Quantidade em estoque (opcional)",
                        numeric = true,
                    )
                    PlainField(
                        value = state.refillThreshold,
                        onValueChange = viewModel::setRefill,
                        placeholder = "Avisar quando restarem…",
                        numeric = true,
                    )
                }
            }

            FocusCard {
                PlainField(
                    value = state.note,
                    onValueChange = viewModel::setNote,
                    placeholder = "Observações",
                    singleLine = false,
                )
            }

            if (!state.isNew) {
                TextButton(
                    onClick = { viewModel.delete(onDone) },
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                ) {
                    Text("Excluir lembrete", color = MaterialTheme.colorScheme.error)
                }
            }
            Spacer(Modifier.height(Spacing.md))
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(Sizes.bottomBarHeight)
                .padding(horizontal = Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onDone, modifier = Modifier.weight(1f)) {
                Text("Cancelar", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onBackground)
            }
            TextButton(
                onClick = { viewModel.save(onDone) },
                enabled = state.canSave,
                modifier = Modifier
                    .weight(1f)
                    .alpha(if (state.canSave) 1f else DISABLED_ALPHA),
            ) {
                Text("Salvar", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onBackground)
            }
        }
    }

    when (dialog) {
        EditDialog.DATE -> DatePickerDialogOneUi(
            initial = state.date,
            onConfirm = {
                viewModel.setDate(it)
                dialog = null
            },
            onDismiss = { dialog = null },
        )
        EditDialog.SINGLE_TIME, EditDialog.ADD_TIME -> {
            val adding = dialog == EditDialog.ADD_TIME
            val initial = if (adding) state.times.maxOrNull()?.plusHours(4) ?: LocalTime.of(8, 0) else state.times.firstOrNull() ?: LocalTime.of(8, 0)
            TimeWheelsDialog(
                title = if (adding) "Adicionar horário" else "Horário",
                initialHour = initial.hour,
                initialMinute = initial.minute,
                onConfirm = { hour, minute ->
                    val time = LocalTime.of(hour, minute)
                    if (adding) viewModel.addTime(time) else viewModel.setSingleTime(time)
                    dialog = null
                },
                onDismiss = { dialog = null },
            )
        }
        EditDialog.REPEAT -> ChoiceDialog(
            title = "Repetir",
            options = ReminderRepeat.entries,
            current = state.repeat,
            label = ::repeatOptionLabel,
            onSelect = {
                viewModel.setRepeat(it)
                dialog = null
            },
            onDismiss = { dialog = null },
        )
        EditDialog.LEAD -> ChoiceDialog(
            title = "Avisar",
            options = LEAD_OPTIONS,
            current = state.leadMinutes,
            label = ::leadLabel,
            onSelect = {
                viewModel.setLead(it)
                dialog = null
            },
            onDismiss = { dialog = null },
        )
        EditDialog.DURATION -> ChoiceDialog(
            title = "Duração do tratamento",
            options = DURATION_OPTIONS,
            current = state.durationDays,
            label = ::durationLabel,
            onSelect = {
                viewModel.setDuration(it)
                dialog = null
            },
            onDismiss = { dialog = null },
        )
        EditDialog.INTERVAL -> ChoiceDialog(
            title = "Preencher horários",
            options = INTERVAL_OPTIONS,
            current = -1,
            label = ::intervalLabel,
            onSelect = {
                viewModel.fillEvery(it)
                dialog = null
            },
            onDismiss = { dialog = null },
        )
        null -> Unit
    }
}

private fun repeatOptionLabel(repeat: ReminderRepeat): String = when (repeat) {
    ReminderRepeat.NONE -> "Não repetir"
    ReminderRepeat.DAILY -> "Todos os dias"
    ReminderRepeat.WEEKLY -> "Dias da semana"
}

@Composable
private fun KindSelector(selected: ReminderKind, onSelect: (ReminderKind) -> Unit) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(modifier = Modifier.padding(Spacing.xs)) {
            ReminderKind.entries.forEach { kind ->
                val isSelected = kind == selected
                val background by animateColorAsState(
                    targetValue = if (isSelected) ControlActive else Color.Transparent,
                    label = "kindBg",
                )
                val content by animateColorAsState(
                    targetValue = if (isSelected) OnPrimary else MaterialTheme.colorScheme.onSurface,
                    label = "kindText",
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(CircleShape)
                        .background(background)
                        .clickable { onSelect(kind) }
                        .padding(vertical = Spacing.sm + Spacing.xs),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = kindLabel(kind), style = MaterialTheme.typography.labelLarge, color = content)
                }
            }
        }
    }
}

@Composable
private fun PlainField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    singleLine: Boolean = true,
    numeric: Boolean = false,
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder) },
        singleLine = singleLine,
        keyboardOptions = if (numeric) KeyboardOptions(keyboardType = KeyboardType.Number) else KeyboardOptions.Default,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.sm, vertical = Spacing.xs),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            focusedIndicatorColor = ControlActive,
            unfocusedIndicatorColor = MaterialTheme.colorScheme.outlineVariant,
            cursorColor = ControlActive,
        ),
    )
}

@Composable
private fun TimeChip(label: String, onRemove: () -> Unit) {
    Surface(shape = CircleShape, color = ControlActive, contentColor = OnPrimary) {
        Row(
            modifier = Modifier.padding(start = Spacing.md, end = Spacing.sm, top = Spacing.sm, bottom = Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            Text(text = label, style = MaterialTheme.typography.labelLarge)
            Icon(
                Icons.Filled.Close,
                contentDescription = "Remover $label",
                modifier = Modifier
                    .size(Sizes.smallIcon)
                    .clip(CircleShape)
                    .clickable(onClick = onRemove),
            )
        }
    }
}

@Composable
private fun AddChip(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(Sizes.smallIcon))
            Text(text = "Adicionar", style = MaterialTheme.typography.labelLarge)
        }
    }
}
