package tech.gomeslab.superapp.feature.alarm.domain

import java.util.Calendar

// Cálculo puro do próximo horário de disparo — sem dependência de framework Android,
// para ser testável em unit test. Recebe `now` para ser determinístico.
object NextTrigger {

    fun compute(now: Calendar, hour: Int, minute: Int, daysOfWeek: Int): Long {
        val base = (now.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (daysOfWeek == 0) {
            if (base.timeInMillis <= now.timeInMillis) {
                base.add(Calendar.DAY_OF_YEAR, 1)
            }
            return base.timeInMillis
        }

        for (i in 0..7) {
            val candidate = (base.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, i) }
            val bit = 1 shl (candidate.get(Calendar.DAY_OF_WEEK) - 1)
            if ((daysOfWeek and bit) != 0 && candidate.timeInMillis > now.timeInMillis) {
                return candidate.timeInMillis
            }
        }

        return base.apply { add(Calendar.DAY_OF_YEAR, 7) }.timeInMillis
    }
}
