package pk.groscale.feeregister.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

/** The worked examples from data-model.md, verbatim. If these change, the spec changed. */
class FeeCalculatorTest {

    private fun due(fee: Int, inPeriod: Int, enrolled: Int) =
        FeeCalculator.amountDue(fee, inPeriod, enrolled)

    @Test fun `full period bills the exact fee`() = assertEquals(2500, due(2500, 22, 22))

    @Test fun `no days enrolled bills nothing`() = assertEquals(0, due(2500, 22, 0))

    @Test fun `exactly half a period`() = assertEquals(1250, due(2500, 22, 11))

    @Test fun `prorated amount rounds to nearest fifty`() = assertEquals(850, due(2500, 26, 9))

    @Test fun `a month with fewer class days still bills the full fee`() =
        assertEquals(2500, due(2500, 24, 24))

    @Test fun `a period where the batch never met bills nothing`() = assertEquals(0, due(2500, 0, 0))

    @Test fun `enrolled days above period days are clamped to the full fee`() =
        assertEquals(2500, due(2500, 22, 25))

    @Test fun `single day of a thirty day period`() = assertEquals(100, due(3000, 30, 1))

    @Test fun `rounds down when nearer the lower step`() = assertEquals(600, due(2000, 23, 7))

    @Test fun `almost a full period is still less than the full fee`() =
        assertEquals(2400, due(2500, 22, 21))

    // --- properties that must always hold ---------------------------------

    @Test fun `a full period is never rounded`() {
        // 2513 is not a multiple of 50: rule 2 must return it untouched.
        assertEquals(2513, FeeCalculator.amountDue(2513, 22, 22))
    }

    @Test fun `holidays reduce class days but never a full-period fee`() {
        val fee = 2500
        val withHolidays = FeeCalculator.amountDue(fee, classDaysInPeriod = 18, classDaysEnrolled = 18)
        val without = FeeCalculator.amountDue(fee, classDaysInPeriod = 26, classDaysEnrolled = 26)
        assertEquals(without, withHolidays)
    }

    @Test fun `rounding step of one disables rounding`() {
        assertEquals(865, FeeCalculator.amountDue(2500, 26, 9, roundingStep = 1))
    }

    // --- class day counting -----------------------------------------------

    @Test fun `counts mon to sat across a full month`() {
        // August 2026: 1 Aug is a Saturday. 31 days, so 5 Sundays (2,9,16,23,30).
        val days = FeeCalculator.classDays(
            MON_TO_SAT,
            LocalDate.of(2026, 8, 1),
            LocalDate.of(2026, 8, 31),
        )
        assertEquals(26, days)
    }

    @Test fun `holidays are excluded`() {
        val holidays = setOf(LocalDate.of(2026, 8, 3), LocalDate.of(2026, 8, 4))
        val days = FeeCalculator.classDays(
            MON_TO_SAT,
            LocalDate.of(2026, 8, 1),
            LocalDate.of(2026, 8, 31),
            holidays,
        )
        assertEquals(24, days)
    }

    @Test fun `a three day a week batch`() {
        val mask = daysOfWeekMaskOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)
        val days = FeeCalculator.classDays(mask, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31))
        assertEquals(13, days)
    }

    @Test fun `an inverted range counts nothing`() {
        val days = FeeCalculator.classDays(
            MON_TO_SAT,
            LocalDate.of(2026, 8, 31),
            LocalDate.of(2026, 8, 1),
        )
        assertEquals(0, days)
    }

    @Test fun `mid month join counts only remaining class days`() {
        val days = FeeCalculator.classDays(
            MON_TO_SAT,
            LocalDate.of(2026, 6, 18),
            LocalDate.of(2026, 6, 30),
        )
        assertEquals(11, days)
    }
}
