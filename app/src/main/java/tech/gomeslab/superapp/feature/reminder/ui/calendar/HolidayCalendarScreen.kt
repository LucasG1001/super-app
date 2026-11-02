package tech.gomeslab.superapp.feature.reminder.ui.calendar

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import tech.gomeslab.superapp.core.holiday.Holiday
import tech.gomeslab.superapp.core.holiday.HolidayType
import tech.gomeslab.superapp.core.holiday.IndaiatubaHolidays
import tech.gomeslab.superapp.core.ui.components.ExtendedScaffold
import tech.gomeslab.superapp.core.ui.theme.ControlActive
import tech.gomeslab.superapp.core.ui.theme.OnPrimary
import tech.gomeslab.superapp.core.ui.theme.Sizes
import tech.gomeslab.superapp.core.ui.theme.Spacing
import tech.gomeslab.superapp.feature.reminder.domain.AgendaItem
import tech.gomeslab.superapp.feature.reminder.ui.KindBadge
import tech.gomeslab.superapp.core.ui.components.SectionHeader
import tech.gomeslab.superapp.feature.reminder.ui.holidayTypeLabel
import tech.gomeslab.superapp.feature.reminder.ui.label
import tech.gomeslab.superapp.feature.reminder.ui.monthTitle
import tech.gomeslab.superapp.feature.reminder.ui.relativeDayLabel
import tech.gomeslab.superapp.feature.reminder.ui.shortDateLabel
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

private val FIRST_MONTH: YearMonth = YearMonth.of(2020, 1)
private const val MONTH_COUNT = 12 * 16
private val WEEK_HEADER = listOf("D", "S", "T", "Q", "Q", "S", "S")

@Composable
fun HolidayCalendarScreen(
    onBack: () -> Unit,
    onOpenReminder: (Long) -> Unit,
    viewModel: HolidayCalendarViewModel = hiltViewModel(),
) {
    val data by viewModel.data.collectAsStateWithLifecycle()
    val today = remember { LocalDate.now() }
    val todayPage = ChronoUnit.MONTHS.between(FIRST_MONTH, YearMonth.from(today)).toInt()
    val pagerState = rememberPagerState(initialPage = todayPage) { MONTH_COUNT }
    val scope = rememberCoroutineScope()
    var selected by rememberSaveable { mutableStateOf(today) }

    val visibleMonth = FIRST_MONTH.plusMonths(pagerState.currentPage.toLong())
    val monthHolidays = viewModel.holidaysOf(visibleMonth)
    val selectedHolidays = viewModel.holidaysOf(YearMonth.from(selected)).filter { it.date == selected }
    val selectedAgenda = remember(selected, data) { viewModel.agendaOf(selected, data) }

    ExtendedScaffold(
        title = "Calendário",
        subtitle = "Feriados de ${IndaiatubaHolidays.CITY}",
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
            }
        },
        actions = {
            TextButton(onClick = {
                selected = today
                scope.launch { pagerState.animateScrollToPage(todayPage) }
            }) {
                Text("Hoje", color = ControlActive)
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
            item(key = "month") {
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier
                            .padding(vertical = Spacing.md)
                            .animateContentSize(),
                    ) {
                        MonthHeader(
                            month = visibleMonth,
                            onPrevious = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) } },
                            onNext = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) } },
                        )
                        WeekHeader()
                        HorizontalPager(state = pagerState) { page ->
                            val month = FIRST_MONTH.plusMonths(page.toLong())
                            val holidays = viewModel.holidaysOf(month).associateBy { it.date }
                            val reminderDays = remember(month, data.reminders) {
                                viewModel.reminderDays(month, data.reminders)
                            }
                            MonthGrid(
                                month = month,
                                today = today,
                                selected = selected,
                                holidays = holidays,
                                reminderDays = reminderDays,
                                onSelect = { selected = it },
                            )
                        }
                        Legend()
                    }
                }
            }

            item(key = "dayHeader") { SectionHeader(relativeDayLabel(selected, today)) }
            if (selectedHolidays.isEmpty() && selectedAgenda.isEmpty()) {
                item(key = "dayEmpty") {
                    Text(
                        text = "Nenhum feriado ou lembrete neste dia.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = Spacing.sm),
                    )
                }
            }
            items(selectedHolidays, key = { "sh-${it.date}-${it.name}" }) { holiday ->
                HolidayRow(holiday = holiday, showDate = false)
            }
            items(selectedAgenda, key = { "sa-${it.reminder.id}-${it.occurrence}" }) { item ->
                DayAgendaRow(item = item, onClick = { onOpenReminder(item.reminder.id) })
            }

            item(key = "monthHeader") { SectionHeader("Feriados de ${monthTitle(visibleMonth.year, visibleMonth.monthValue).lowercase()}") }
            if (monthHolidays.isEmpty()) {
                item(key = "monthEmpty") {
                    Text(
                        text = "Nenhum feriado neste mês.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = Spacing.sm),
                    )
                }
            }
            items(monthHolidays, key = { "mh-${it.date}-${it.name}" }) { holiday ->
                HolidayRow(
                    holiday = holiday,
                    showDate = true,
                    onClick = { selected = holiday.date },
                )
            }
        }
    }
}

@Composable
private fun MonthHeader(month: YearMonth, onPrevious: () -> Unit, onNext: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onPrevious) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Mês anterior")
        }
        Text(
            text = monthTitle(month.year, month.monthValue),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onNext) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Próximo mês")
        }
    }
}

@Composable
private fun WeekHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.sm, vertical = Spacing.xs),
    ) {
        WEEK_HEADER.forEachIndexed { index, day ->
            Text(
                text = day,
                style = MaterialTheme.typography.labelMedium,
                color = if (index == 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun MonthGrid(
    month: YearMonth,
    today: LocalDate,
    selected: LocalDate,
    holidays: Map<LocalDate, Holiday>,
    reminderDays: Set<LocalDate>,
    onSelect: (LocalDate) -> Unit,
) {
    val leading = month.atDay(1).dayOfWeek.value % 7
    val rows = (leading + month.lengthOfMonth() + 6) / 7
    Column(modifier = Modifier.padding(horizontal = Spacing.sm)) {
        repeat(rows) { row ->
            Row {
                repeat(7) { column ->
                    val dayNumber = row * 7 + column - leading + 1
                    Box(modifier = Modifier.weight(1f).height(Sizes.calendarCell), contentAlignment = Alignment.Center) {
                        if (dayNumber in 1..month.lengthOfMonth()) {
                            val date = month.atDay(dayNumber)
                            DayCell(
                                date = date,
                                isToday = date == today,
                                isSelected = date == selected,
                                isSunday = column == 0,
                                holiday = holidays[date],
                                hasReminder = date in reminderDays,
                                onClick = { onSelect(date) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    isToday: Boolean,
    isSelected: Boolean,
    isSunday: Boolean,
    holiday: Holiday?,
    hasReminder: Boolean,
    onClick: () -> Unit,
) {
    val holidayColor = holidayColor(holiday?.type)
    val textColor = when {
        isSelected -> OnPrimary
        holiday != null -> holidayColor
        isSunday -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurface
    }
    Column(
        modifier = Modifier
            .size(Sizes.calendarCell)
            .clip(CircleShape)
            .background(if (isSelected) ControlActive else Color.Transparent)
            .then(
                if (isToday && !isSelected) {
                    Modifier.border(BorderStroke(Spacing.xxs, ControlActive), CircleShape)
                } else {
                    Modifier
                },
            )
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = date.dayOfMonth.toString(),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (holiday?.isDayOff == true || isToday) FontWeight.Bold else FontWeight.Normal,
            color = textColor,
        )
        Row(
            modifier = Modifier.height(Sizes.calendarDot),
            horizontalArrangement = Arrangement.spacedBy(Spacing.xxs),
        ) {
            if (holiday != null) Dot(if (isSelected) OnPrimary else holidayColor)
            if (hasReminder) Dot(if (isSelected) OnPrimary else ControlActive)
        }
    }
}

@Composable
private fun Dot(color: Color) {
    Box(
        modifier = Modifier
            .size(Sizes.calendarDot)
            .clip(CircleShape)
            .background(color),
    )
}

@Composable
private fun holidayColor(type: HolidayType?): Color = when (type) {
    HolidayType.OPTIONAL -> MaterialTheme.colorScheme.onSurfaceVariant
    null -> MaterialTheme.colorScheme.onSurface
    else -> MaterialTheme.colorScheme.error
}

@Composable
private fun Legend() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = Spacing.lg, end = Spacing.lg, top = Spacing.md),
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        LegendItem(MaterialTheme.colorScheme.error, "Feriado")
        LegendItem(MaterialTheme.colorScheme.onSurfaceVariant, "Facultativo")
        LegendItem(ControlActive, "Lembrete")
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Dot(color)
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun HolidayRow(holiday: Holiday, showDate: Boolean, onClick: (() -> Unit)? = null) {
    val color = holidayColor(holiday.type)
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
    ) {
        Row(
            modifier = Modifier.padding(Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .width(Spacing.xs)
                    .height(Sizes.categoryIcon)
                    .clip(CircleShape)
                    .background(color),
            )
            Spacer(Modifier.width(Spacing.md))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = holiday.name, style = MaterialTheme.typography.titleMedium)
                val details = listOfNotNull(
                    shortDateLabel(holiday.date).takeIf { showDate },
                    holidayTypeLabel(holiday.type),
                    holiday.note,
                ).joinToString(" · ")
                Text(
                    text = details,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun DayAgendaRow(item: AgendaItem, onClick: () -> Unit) {
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
            KindBadge(item.reminder.kind)
            Column(modifier = Modifier.weight(1f)) {
                Text(text = item.reminder.title, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = item.occurrence.toLocalTime().label(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
