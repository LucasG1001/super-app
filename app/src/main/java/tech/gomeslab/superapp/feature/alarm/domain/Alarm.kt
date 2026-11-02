package tech.gomeslab.superapp.feature.alarm.domain

// Modelo de domínio do alarme. `daysOfWeek` é um bitmask: bit (Calendar.DAY_OF_WEEK - 1).
// Vazio (0) significa alarme único que dispara uma vez e se desativa.
data class Alarm(
    val id: Long = 0,
    val hour: Int,
    val minute: Int,
    val label: String = "",
    val enabled: Boolean = true,
    val daysOfWeek: Int = 0,
    val soundUri: String? = null,
    val vibrate: Boolean = true,
    val snoozeMinutes: Int = 5,
    val gradualVolumeSeconds: Int = 0,
    val createdAt: Long = 0,
) {
    val repeats: Boolean get() = daysOfWeek != 0

    fun repeatsOn(dayBitIndex: Int): Boolean = (daysOfWeek and (1 shl dayBitIndex)) != 0

    companion object {
        // Índices de bit por dia (alinhados a Calendar.DAY_OF_WEEK - 1).
        const val SUNDAY = 0
        const val MONDAY = 1
        const val TUESDAY = 2
        const val WEDNESDAY = 3
        const val THURSDAY = 4
        const val FRIDAY = 5
        const val SATURDAY = 6

        fun maskOf(vararg dayBitIndices: Int): Int =
            dayBitIndices.fold(0) { acc, d -> acc or (1 shl d) }
    }
}
