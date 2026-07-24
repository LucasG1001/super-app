package tech.gomeslab.superapp.feature.alarm.ui

import tech.gomeslab.superapp.feature.alarm.domain.Alarm

// Formatação de exibição do despertador (texto de UI em português).
fun formatTime(hour: Int, minute: Int): String = "%02d:%02d".format(hour, minute)

private val DAY_SHORT = arrayOf("Dom", "Seg", "Ter", "Qua", "Qui", "Sex", "Sáb")

fun daysLabel(daysOfWeek: Int): String {
    if (daysOfWeek == 0) return "Uma vez"
    val all = (0..6).all { (daysOfWeek and (1 shl it)) != 0 }
    if (all) return "Todos os dias"
    val weekdays = Alarm.maskOf(
        Alarm.MONDAY, Alarm.TUESDAY, Alarm.WEDNESDAY, Alarm.THURSDAY, Alarm.FRIDAY,
    )
    if (daysOfWeek == weekdays) return "Dias de semana"
    return (0..6)
        .filter { (daysOfWeek and (1 shl it)) != 0 }
        .joinToString(", ") { DAY_SHORT[it] }
}
