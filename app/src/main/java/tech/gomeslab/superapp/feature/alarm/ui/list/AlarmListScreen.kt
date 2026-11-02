package tech.gomeslab.superapp.feature.alarm.ui.list

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import tech.gomeslab.superapp.core.ui.components.ExtendedScaffold
import tech.gomeslab.superapp.core.ui.components.OneUiSwitch
import tech.gomeslab.superapp.core.ui.theme.ControlActive
import tech.gomeslab.superapp.core.ui.theme.Sizes
import tech.gomeslab.superapp.core.ui.theme.Spacing
import tech.gomeslab.superapp.feature.alarm.domain.Alarm
import tech.gomeslab.superapp.feature.alarm.domain.NextTrigger
import tech.gomeslab.superapp.feature.alarm.ui.DAY_INITIALS
import tech.gomeslab.superapp.feature.alarm.ui.dayLabel
import tech.gomeslab.superapp.feature.alarm.ui.formatTime
import tech.gomeslab.superapp.feature.alarm.ui.untilLabel
import java.util.Calendar

private const val DISABLED_ALPHA = 0.4f

@Composable
fun AlarmListScreen(
    onAddAlarm: () -> Unit,
    onEditAlarm: (Long) -> Unit,
    onBack: () -> Unit,
    viewModel: AlarmListViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) {}
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    BackHandler(enabled = state.selectionMode) { viewModel.clearSelection() }

    val selectedCount = state.selectedIds.size
    val title = when {
        !state.selectionMode -> "Alarme"
        selectedCount == 0 -> "Selecionar alarmes"
        else -> "$selectedCount selecionado(s)"
    }
    val subtitle = when {
        state.selectionMode -> null
        state.alarms.isEmpty() -> null
        else -> state.nextTriggerAt?.let { untilLabel(it - state.now) }
            ?: "Todos os alarmes desligados"
    }

    ExtendedScaffold(
        title = title,
        subtitle = subtitle,
        navigationIcon = {
            if (state.selectionMode) {
                val allSelected = state.alarms.isNotEmpty() && selectedCount == state.alarms.size
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clickable(onClick = viewModel::toggleSelectAll)
                        .padding(end = Spacing.sm),
                ) {
                    Checkbox(
                        checked = allSelected,
                        onCheckedChange = { viewModel.toggleSelectAll() },
                        colors = CheckboxDefaults.colors(checkedColor = ControlActive),
                    )
                    Text("Todos", style = MaterialTheme.typography.labelMedium)
                }
            } else {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                }
            }
        },
        actions = {
            if (!state.selectionMode) {
                IconButton(onClick = onAddAlarm) {
                    Icon(Icons.Filled.Add, contentDescription = "Adicionar alarme")
                }
                MoreMenu(
                    enabled = state.alarms.isNotEmpty(),
                    onEdit = { viewModel.startSelection() },
                )
            }
        },
        bottomBar = {
            AnimatedVisibility(
                visible = state.selectionMode,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut(),
            ) {
                SelectionBar(
                    enabled = selectedCount > 0,
                    onDelete = viewModel::deleteSelected,
                )
            }
        },
    ) { innerPadding ->
        if (state.alarms.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState()),
                contentAlignment = Alignment.TopCenter,
            ) {
                Text(
                    text = "Nenhum alarme",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = Spacing.xxl),
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = Spacing.md,
                    end = Spacing.md,
                    top = innerPadding.calculateTopPadding() + Spacing.xs,
                    bottom = innerPadding.calculateBottomPadding() + Spacing.lg,
                ),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                items(state.alarms, key = { it.id }) { alarm ->
                    AlarmCard(
                        alarm = alarm,
                        now = state.now,
                        selectionMode = state.selectionMode,
                        selected = alarm.id in state.selectedIds,
                        onClick = {
                            if (state.selectionMode) viewModel.toggleSelection(alarm.id)
                            else onEditAlarm(alarm.id)
                        },
                        onLongClick = {
                            if (state.selectionMode) viewModel.toggleSelection(alarm.id)
                            else viewModel.startSelection(alarm.id)
                        },
                        onToggle = { enabled -> viewModel.setEnabled(alarm, enabled) },
                        modifier = Modifier.animateItem(),
                    )
                }
            }
        }
    }
}

@Composable
private fun MoreMenu(enabled: Boolean, onEdit: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Filled.MoreVert, contentDescription = "Mais opções")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("Editar") },
                enabled = enabled,
                onClick = {
                    expanded = false
                    onEdit()
                },
            )
        }
    }
}

@Composable
private fun SelectionBar(enabled: Boolean, onDelete: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.background) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .height(Sizes.bottomBarHeight),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier
                    .alpha(if (enabled) 1f else DISABLED_ALPHA)
                    .clickable(enabled = enabled, onClick = onDelete)
                    .padding(horizontal = Spacing.xl, vertical = Spacing.xs),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(Icons.Outlined.Delete, contentDescription = null)
                Text("Excluir", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AlarmCard(
    alarm: Alarm,
    now: Long,
    selectionMode: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    val contentAlpha by animateFloatAsState(
        targetValue = if (alarm.enabled) 1f else DISABLED_ALPHA,
        label = "alarmAlpha",
    )

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Row(
            modifier = Modifier
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onLongClick()
                    },
                )
                .padding(horizontal = Spacing.lg, vertical = Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AnimatedVisibility(
                visible = selectionMode,
                enter = expandHorizontally() + fadeIn(),
                exit = shrinkHorizontally() + fadeOut(),
            ) {
                Row {
                    Checkbox(
                        checked = selected,
                        onCheckedChange = null,
                        colors = CheckboxDefaults.colors(checkedColor = ControlActive),
                    )
                    Spacer(Modifier.width(Spacing.md))
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .alpha(contentAlpha),
            ) {
                if (alarm.label.isNotBlank()) {
                    Text(
                        text = alarm.label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
                Text(
                    text = formatTime(alarm.hour, alarm.minute),
                    style = MaterialTheme.typography.displayMedium,
                )
                ScheduleLine(alarm = alarm, now = now)
            }

            AnimatedVisibility(
                visible = !selectionMode,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                OneUiSwitch(checked = alarm.enabled, onCheckedChange = onToggle)
            }
        }
    }
}

@Composable
private fun ScheduleLine(alarm: Alarm, now: Long) {
    if (!alarm.repeats) {
        val nowCal = Calendar.getInstance().apply { timeInMillis = now }
        val triggerAt = NextTrigger.compute(nowCal, alarm.hour, alarm.minute, 0)
        Text(
            text = dayLabel(triggerAt, nowCal),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        DAY_INITIALS.forEachIndexed { day, initial ->
            val active = alarm.repeatsOn(day)
            Text(
                text = initial,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                color = when {
                    active && day == Alarm.SUNDAY -> MaterialTheme.colorScheme.error
                    active -> ControlActive
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
    }
}
