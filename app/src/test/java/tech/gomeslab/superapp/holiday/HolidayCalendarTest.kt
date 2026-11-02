package tech.gomeslab.superapp.holiday

import org.junit.Assert.assertEquals
import org.junit.Test
import tech.gomeslab.superapp.core.holiday.Easter
import tech.gomeslab.superapp.core.holiday.HolidayCalendar
import tech.gomeslab.superapp.core.holiday.HolidayType
import tech.gomeslab.superapp.core.holiday.IndaiatubaHolidays
import java.time.LocalDate

class HolidayCalendarTest {

    @Test
    fun `easter sunday matches known dates`() {
        assertEquals(LocalDate.of(2025, 4, 20), Easter.sunday(2025))
        assertEquals(LocalDate.of(2026, 4, 5), Easter.sunday(2026))
        assertEquals(LocalDate.of(2027, 3, 28), Easter.sunday(2027))
        assertEquals(LocalDate.of(2024, 3, 31), Easter.sunday(2024))
    }

    @Test
    fun `indaiatuba 2026 matches the researched calendar`() {
        val holidays = HolidayCalendar.forYear(2026, IndaiatubaHolidays.rules)
        val byDate = holidays.associate { it.date.toString() to it.type }

        val expected = mapOf(
            "2026-01-01" to HolidayType.NATIONAL,
            "2026-02-02" to HolidayType.MUNICIPAL,
            "2026-02-16" to HolidayType.OPTIONAL,
            "2026-02-17" to HolidayType.OPTIONAL,
            "2026-02-18" to HolidayType.OPTIONAL,
            "2026-04-03" to HolidayType.MUNICIPAL,
            "2026-04-21" to HolidayType.NATIONAL,
            "2026-05-01" to HolidayType.NATIONAL,
            "2026-06-04" to HolidayType.MUNICIPAL,
            "2026-07-09" to HolidayType.STATE,
            "2026-09-07" to HolidayType.NATIONAL,
            "2026-10-12" to HolidayType.NATIONAL,
            "2026-10-28" to HolidayType.OPTIONAL,
            "2026-11-02" to HolidayType.NATIONAL,
            "2026-11-15" to HolidayType.NATIONAL,
            "2026-11-20" to HolidayType.NATIONAL,
            "2026-12-24" to HolidayType.OPTIONAL,
            "2026-12-25" to HolidayType.NATIONAL,
            "2026-12-31" to HolidayType.OPTIONAL,
        )
        assertEquals(expected, byDate)
    }

    @Test
    fun `range spans across years and is sorted`() {
        val holidays = HolidayCalendar.forRange(
            LocalDate.of(2026, 12, 20),
            LocalDate.of(2027, 1, 5),
            IndaiatubaHolidays.rules,
        )
        assertEquals(
            listOf("2026-12-24", "2026-12-25", "2026-12-31", "2027-01-01"),
            holidays.map { it.date.toString() },
        )
    }
}
