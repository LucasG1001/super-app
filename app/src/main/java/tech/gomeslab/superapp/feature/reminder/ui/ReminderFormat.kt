package tech.gomeslab.superapp.feature.reminder.ui

import tech.gomeslab.superapp.core.holiday.HolidayType
import tech.gomeslab.superapp.feature.reminder.domain.Reminder
import tech.gomeslab.superapp.feature.reminder.domain.ReminderKind
import tech.gomeslab.superapp.feature.reminder.domain.ReminderRepeat
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

val PT_BR: Locale = Locale.forLanguageTag("pt-BR")

private val TIME = DateTimeFormatter.ofPattern("HH:mm")
private val SHORT_DATE = DateTimeFormatter.ofPattern("EEE, d 'de' MMM", PT_BR)

fun LocalTime.label(): String = format(TIME)

fun relativeDayLabel(date: LocalDate, today: LocalDate): String = when (date) {
    today -> "Hoje"
    today.plusDays(1) -> "Amanhã"
    today.minusDays(1) -> "Ontem"
    else -> date.format(SHORT_DATE).replaceFirstChar { it.uppercase(PT_BR) }
}

fun shortDateLabel(date: LocalDate): String = date.format(SHORT_DATE)

fun monthTitle(year: Int, month: Int): String {
    val name = java.time.Month.of(month).getDisplayName(TextStyle.FULL_STANDALONE, PT_BR)
    return "${name.replaceFirstChar { it.uppercase(PT_BR) }} $year"
}

fun kindLabel(kind: ReminderKind): String = when (kind) {
    ReminderKind.TASK -> "Lembrete"
    ReminderKind.MEDICINE -> "Remédio"
    ReminderKind.EVENT -> "Evento"
}

fun holidayTypeLabel(type: HolidayType): String = when (type) {
    HolidayType.NATIONAL -> "Feriado nacional"
    HolidayType.STATE -> "Feriado estadual"
    HolidayType.MUNICIPAL -> "Feriado municipal"
    HolidayType.OPTIONAL -> "Ponto facultativo"
}

fun leadLabel(minutes: Int): String = when {
    minutes <= 0 -> "Na hora"
    minutes < 60 -> "$minutes min antes"
    minutes < 24 * 60 -> "${minutes / 60} h antes"
    else -> "${minutes / (24 * 60)} dia antes"
}

fun intervalLabel(hours: Int): String = "A cada $hours horas"

fun durationLabel(days: Int?): String = when (days) {
    null -> "Uso contínuo"
    1 -> "Por 1 dia"
    else -> "Por $days dias"
}

private val DAY_SHORT = arrayOf("Dom", "Seg", "Ter", "Qua", "Qui", "Sex", "Sáb")

fun repeatLabel(reminder: Reminder): String = when (reminder.repeat) {
    ReminderRepeat.NONE -> "Uma vez"
    ReminderRepeat.DAILY -> "Todos os dias"
    ReminderRepeat.WEEKLY -> (0..6)
        .filter { (reminder.daysOfWeek and (1 shl it)) != 0 }
        .joinToString(", ") { DAY_SHORT[it] }
        .ifBlank { "Toda semana" }
}

fun scheduleSummary(reminder: Reminder): String = buildList {
    add(reminder.times.sorted().joinToString(" · ") { it.label() })
    add(repeatLabel(reminder))
    if (reminder.kind == ReminderKind.MEDICINE && reminder.dose.isNotBlank()) add(reminder.dose)
}.joinToString(" — ")

fun stockLabel(reminder: Reminder): String? {
    val stock = reminder.stock ?: return null
    return if (reminder.needsRefill) "Restam $stock · comprar mais" else "Restam $stock"
}
