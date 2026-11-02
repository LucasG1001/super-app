package tech.gomeslab.superapp.feature.alarm.ui

import tech.gomeslab.superapp.feature.alarm.domain.Alarm
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val PT_BR: Locale = Locale.forLanguageTag("pt-BR")

fun formatTime(hour: Int, minute: Int): String = "%02d:%02d".format(hour, minute)

fun twoDigits(value: Int): String = "%02d".format(value)

val DAY_INITIALS = listOf("D", "S", "T", "Q", "Q", "S", "S")

private val DAY_SHORT = arrayOf("Dom", "Seg", "Ter", "Qua", "Qui", "Sex", "Sáb")

fun daysLabel(daysOfWeek: Int): String {
    if (daysOfWeek == 0) return "Uma vez"
    val all = (0..6).all { (daysOfWeek and (1 shl it)) != 0 }
    if (all) return "Todos os dias"
    val weekdays = Alarm.maskOf(
        Alarm.MONDAY, Alarm.TUESDAY, Alarm.WEDNESDAY, Alarm.THURSDAY, Alarm.FRIDAY,
    )
    if (daysOfWeek == weekdays) return "Dias de semana"
    if (daysOfWeek == Alarm.maskOf(Alarm.SATURDAY, Alarm.SUNDAY)) return "Fins de semana"
    return (0..6)
        .filter { (daysOfWeek and (1 shl it)) != 0 }
        .joinToString(", ") { DAY_SHORT[it] }
}

fun untilLabel(millis: Long): String {
    val totalMinutes = (millis + 59_999) / 60_000
    if (totalMinutes <= 1) return "Alarme em menos de 1 minuto"
    val days = totalMinutes / (24 * 60)
    val hours = (totalMinutes % (24 * 60)) / 60
    val minutes = totalMinutes % 60
    val parts = buildList {
        if (days > 0) add("$days d")
        if (hours > 0) add("$hours h")
        if (minutes > 0 && days == 0L) add("$minutes min")
    }
    return "Alarme em " + parts.joinToString(" ")
}

fun dayLabel(triggerAt: Long, now: Calendar): String {
    val target = Calendar.getInstance().apply { timeInMillis = triggerAt }
    val today = now.get(Calendar.YEAR) * 1000 + now.get(Calendar.DAY_OF_YEAR)
    val tomorrowCal = (now.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 1) }
    val tomorrow = tomorrowCal.get(Calendar.YEAR) * 1000 + tomorrowCal.get(Calendar.DAY_OF_YEAR)
    val day = target.get(Calendar.YEAR) * 1000 + target.get(Calendar.DAY_OF_YEAR)
    val date = SimpleDateFormat("EEE, d 'de' MMM", PT_BR).format(Date(triggerAt))
    return when (day) {
        today -> "Hoje · $date"
        tomorrow -> "Amanhã · $date"
        else -> date.replaceFirstChar { it.uppercase(PT_BR) }
    }
}

fun fullDateLabel(millis: Long): String =
    SimpleDateFormat("EEEE, d 'de' MMMM", PT_BR).format(Date(millis))
        .replaceFirstChar { it.uppercase(PT_BR) }

fun snoozeLabel(minutes: Int): String = "$minutes minutos"

fun gradualVolumeLabel(seconds: Int): String = when {
    seconds <= 0 -> "Desligado"
    seconds < 60 -> "$seconds segundos"
    seconds == 60 -> "1 minuto"
    else -> "${seconds / 60} minutos"
}
