package tech.gomeslab.superapp.feature.reminder.ui.home

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import tech.gomeslab.superapp.core.holiday.Holiday
import tech.gomeslab.superapp.core.ui.components.ExtendedScaffold
import tech.gomeslab.superapp.core.ui.theme.AccentGray
import tech.gomeslab.superapp.core.ui.theme.AccentGreen
import tech.gomeslab.superapp.core.ui.theme.AccentOrange
import tech.gomeslab.superapp.core.ui.theme.ControlActive
import tech.gomeslab.superapp.core.ui.theme.OnPrimary
import tech.gomeslab.superapp.core.ui.theme.Sizes
import tech.gomeslab.superapp.core.ui.theme.Spacing
import tech.gomeslab.superapp.feature.reminder.ui.AgendaRow
import tech.gomeslab.superapp.core.ui.components.SectionHeader
import tech.gomeslab.superapp.feature.reminder.ui.holidayTypeLabel
import tech.gomeslab.superapp.feature.reminder.ui.relativeDayLabel
import tech.gomeslab.superapp.feature.reminder.ui.shortDateLabel
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@Composable
fun ReminderHomeScreen(
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onOpenReminder: (Long) -> Unit,
    onOpenCategory: (ReminderCategory) -> Unit,
    onOpenCalendar: () -> Unit,
    viewModel: ReminderHomeViewModel = hiltViewModel(),
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

    val subtitle = when {
        state.todayItems.isEmpty() -> "Nada para hoje"
        state.openToday == 0 -> "Tudo em dia hoje"
        state.openToday == 1 -> "1 pendente hoje"
        else -> "${state.openToday} pendentes hoje"
    }

    ExtendedScaffold(
        title = "Lembretes",
        subtitle = subtitle,
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
            }
        },
        actions = {
            IconButton(onClick = onOpenCalendar) {
                Icon(Icons.Filled.CalendarMonth, contentDescription = "Calendário de feriados")
            }
            IconButton(onClick = onAdd) {
                Icon(Icons.Filled.Add, contentDescription = "Novo lembrete")
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
            item(key = "categories") {
                CategoryGrid(counts = state.counts, onOpen = onOpenCategory)
            }
            state.nextHoliday?.let { holiday ->
                item(key = "holiday") {
                    NextHolidayCard(holiday = holiday, today = state.today, onClick = onOpenCalendar)
                }
            }

            item(key = "todayHeader") { SectionHeader("Hoje") }
            if (state.todayItems.isEmpty()) {
                item(key = "todayEmpty") {
                    EmptyHint("Nenhum lembrete para hoje. Toque em + para criar.")
                }
            }
            items(state.todayItems, key = { "t-${it.reminder.id}-${it.occurrence}" }) { item ->
                AgendaRow(
                    item = item,
                    onConfirm = { viewModel.confirm(item) },
                    onUndo = { viewModel.undo(item) },
                    onOpen = { onOpenReminder(item.reminder.id) },
                    onDelete = { viewModel.delete(item.reminder.id) },
                    modifier = Modifier.animateItem(),
                )
            }

            if (state.upcoming.isNotEmpty()) {
                item(key = "upcomingHeader") { SectionHeader("Próximos dias") }
                items(state.upcoming, key = { "u-${it.reminder.id}-${it.occurrence}" }) { item ->
                    AgendaRow(
                        item = item,
                        onConfirm = { viewModel.confirm(item) },
                        onUndo = { viewModel.undo(item) },
                        onOpen = { onOpenReminder(item.reminder.id) },
                        onDelete = { viewModel.delete(item.reminder.id) },
                        subtitle = relativeDayLabel(item.occurrence.toLocalDate(), state.today),
                        modifier = Modifier.animateItem(),
                    )
                }
            }
        }
    }
}

private data class CategoryStyle(val icon: ImageVector, val color: Color)

private fun styleOf(category: ReminderCategory): CategoryStyle = when (category) {
    ReminderCategory.TODAY -> CategoryStyle(Icons.Filled.Today, ControlActive)
    ReminderCategory.SCHEDULED -> CategoryStyle(Icons.Filled.Schedule, AccentOrange)
    ReminderCategory.MEDICINE -> CategoryStyle(Icons.Filled.Medication, AccentGreen)
    ReminderCategory.DONE -> CategoryStyle(Icons.Filled.CheckCircle, AccentGray)
}

@Composable
private fun CategoryGrid(counts: Map<ReminderCategory, Int>, onOpen: (ReminderCategory) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        ReminderCategory.entries.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                row.forEach { category ->
                    CategoryCard(
                        category = category,
                        count = counts[category] ?: 0,
                        onClick = { onOpen(category) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryCard(
    category: ReminderCategory,
    count: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val style = styleOf(category)
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier.padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = MaterialTheme.shapes.extraLarge, color = style.color, contentColor = OnPrimary) {
                    Icon(
                        imageVector = style.icon,
                        contentDescription = null,
                        modifier = Modifier
                            .padding(Spacing.sm)
                            .size(Sizes.smallIcon),
                    )
                }
                Spacer(Modifier.weight(1f))
                Text(text = count.toString(), style = MaterialTheme.typography.headlineMedium)
            }
            Text(
                text = category.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun NextHolidayCard(holiday: Holiday, today: LocalDate, onClick: () -> Unit) {
    val days = ChronoUnit.DAYS.between(today, holiday.date)
    val whenLabel = when (days) {
        0L -> "hoje"
        1L -> "amanhã"
        else -> "em $days dias"
    }
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Surface(shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.error, contentColor = OnPrimary) {
                Icon(
                    Icons.Filled.CalendarMonth,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(Spacing.sm)
                        .size(Sizes.smallIcon),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (days == 0L) "Hoje é feriado" else "Próximo feriado · $whenLabel",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(text = holiday.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "${shortDateLabel(holiday.date)} · ${holidayTypeLabel(holiday.type)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
fun EmptyHint(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.sm, vertical = Spacing.md),
    )
}
