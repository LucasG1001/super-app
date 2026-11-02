package tech.gomeslab.superapp.core.holiday

import java.time.LocalDate

enum class HolidayType { NATIONAL, STATE, MUNICIPAL, OPTIONAL }

data class Holiday(
    val date: LocalDate,
    val name: String,
    val type: HolidayType,
    val note: String? = null,
) {
    val isDayOff: Boolean get() = type != HolidayType.OPTIONAL
}

sealed interface HolidayRule {
    val name: String
    val type: HolidayType
    val note: String?

    fun dateIn(year: Int): LocalDate

    data class Fixed(
        val month: Int,
        val day: Int,
        override val name: String,
        override val type: HolidayType,
        override val note: String? = null,
    ) : HolidayRule {
        override fun dateIn(year: Int): LocalDate = LocalDate.of(year, month, day)
    }

    data class EasterOffset(
        val days: Long,
        override val name: String,
        override val type: HolidayType,
        override val note: String? = null,
    ) : HolidayRule {
        override fun dateIn(year: Int): LocalDate = Easter.sunday(year).plusDays(days)
    }
}

object Easter {
    fun sunday(year: Int): LocalDate {
        val a = year % 19
        val b = year / 100
        val c = year % 100
        val d = b / 4
        val e = b % 4
        val f = (b + 8) / 25
        val g = (b - f + 1) / 3
        val h = (19 * a + b - d - g + 15) % 30
        val i = c / 4
        val k = c % 4
        val l = (32 + 2 * e + 2 * i - h - k) % 7
        val m = (a + 11 * h + 22 * l) / 451
        val month = (h + l - 7 * m + 114) / 31
        val day = ((h + l - 7 * m + 114) % 31) + 1
        return LocalDate.of(year, month, day)
    }
}

object HolidayCalendar {
    fun forYear(year: Int, rules: List<HolidayRule>): List<Holiday> =
        rules
            .map { Holiday(it.dateIn(year), it.name, it.type, it.note) }
            .sortedBy { it.date }

    fun forRange(from: LocalDate, to: LocalDate, rules: List<HolidayRule>): List<Holiday> =
        (from.year..to.year)
            .flatMap { forYear(it, rules) }
            .filter { !it.date.isBefore(from) && !it.date.isAfter(to) }
}
