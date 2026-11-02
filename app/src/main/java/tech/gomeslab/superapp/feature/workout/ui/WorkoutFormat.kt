package tech.gomeslab.superapp.feature.workout.ui

import tech.gomeslab.superapp.feature.workout.domain.ExerciseKind
import tech.gomeslab.superapp.feature.workout.domain.WorkoutSet
import tech.gomeslab.superapp.feature.workout.domain.formatDecimal
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val PT_BR: Locale = Locale.forLanguageTag("pt-BR")
private val SHORT_DATE = DateTimeFormatter.ofPattern("EEE, d 'de' MMM", PT_BR)
private val MONTH = DateTimeFormatter.ofPattern("MMMM 'de' yyyy", PT_BR)
private val DAY_MONTH = DateTimeFormatter.ofPattern("d/MM", PT_BR)

fun Long.toLocalDate(): LocalDate = Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate()

fun elapsedClock(millis: Long): String {
    val total = (millis / 1000).coerceAtLeast(0)
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}

fun durationLabel(millis: Long): String {
    val minutes = (millis / 60_000).coerceAtLeast(0)
    if (minutes == 0L) return "< 1 min"
    return if (minutes >= 60) "${minutes / 60} h ${"%02d".format(minutes % 60)} min" else "$minutes min"
}

fun volumeLabel(kg: Double): String = "${NumberFormat.getIntegerInstance(PT_BR).format(Math.round(kg))} kg"

fun countLabel(count: Int, one: String, many: String): String = if (count == 1) "1 $one" else "$count $many"

fun weightLabel(kg: Double): String = "${formatDecimal(kg)} kg"

fun restLabel(seconds: Int): String = when {
    seconds <= 0 -> "Sem descanso"
    seconds < 60 -> "$seconds s"
    seconds % 60 == 0 -> "${seconds / 60} min"
    else -> "${seconds / 60} min ${seconds % 60} s"
}

fun setSummary(set: WorkoutSet, kind: ExerciseKind): String = when (kind) {
    ExerciseKind.WEIGHT_REPS -> "${formatDecimal(set.weightKg ?: 0.0)} kg × ${set.reps ?: 0}"
    ExerciseKind.REPS -> "${set.reps ?: 0} reps"
    ExerciseKind.DURATION -> "${set.durationSec ?: 0} s"
}

fun shortDate(date: LocalDate): String = date.format(SHORT_DATE).replaceFirstChar { it.uppercase(PT_BR) }

fun dayMonth(date: LocalDate): String = date.format(DAY_MONTH)

fun monthLabel(date: LocalDate): String = date.format(MONTH).replaceFirstChar { it.uppercase(PT_BR) }

fun relativeDay(date: LocalDate, today: LocalDate = LocalDate.now()): String = when (date) {
    today -> "Hoje"
    today.minusDays(1) -> "Ontem"
    else -> shortDate(date)
}
