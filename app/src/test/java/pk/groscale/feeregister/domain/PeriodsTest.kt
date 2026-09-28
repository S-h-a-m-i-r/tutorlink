package pk.groscale.feeregister.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class PeriodsTest {

    private val grace = 10

    // --- CALENDAR anchor ---------------------------------------------------

    @Test fun `mid month join creates a stub then whole calendar months`() {
        val periods = Periods.generate(
            anchor = BillingAnchor.CALENDAR,
            startDate = LocalDate.of(2026, 6, 18),
            endDate = null,
            graceDays = grace,
            upTo = LocalDate.of(2026, 8, 15),
        )
        assertEquals(3, periods.size)

        assertEquals(LocalDate.of(2026, 6, 18), periods[0].start)
        assertEquals(LocalDate.of(2026, 6, 30), periods[0].end)
        assertTrue(periods[0].isStub)

        assertEquals(LocalDate.of(2026, 7, 1), periods[1].start)
        assertEquals(LocalDate.of(2026, 7, 31), periods[1].end)
        assertEquals(LocalDate.of(2026, 7, 10), periods[1].dueDate)
        assertTrue(!periods[1].isStub)

        assertEquals(LocalDate.of(2026, 8, 1), periods[2].start)
        assertEquals(LocalDate.of(2026, 8, 31), periods[2].end)
    }

    @Test fun `mergeStubIntoNext moves only the due date, never the period`() {
        val periods = Periods.generate(
            anchor = BillingAnchor.CALENDAR,
            startDate = LocalDate.of(2026, 6, 18),
            endDate = null,
            graceDays = grace,
            upTo = LocalDate.of(2026, 7, 15),
            mergeStubIntoNext = true,
        )
        assertEquals(2, periods.size)
        // The stub still exists as its own period, so it can explain itself.
        assertEquals(LocalDate.of(2026, 6, 18), periods[0].start)
        assertEquals(LocalDate.of(2026, 6, 30), periods[0].end)
        // But it is collected with July.
        assertEquals(LocalDate.of(2026, 7, 10), periods[0].dueDate)
    }

    @Test fun `joining on the first is not a stub`() {
        val periods = Periods.generate(
            BillingAnchor.CALENDAR, LocalDate.of(2026, 7, 1), null, grace, LocalDate.of(2026, 7, 20),
        )
        assertEquals(1, periods.size)
        assertTrue(!periods[0].isStub)
        assertEquals(LocalDate.of(2026, 7, 10), periods[0].dueDate)
    }

    @Test fun `never generates a future period`() {
        val periods = Periods.generate(
            BillingAnchor.CALENDAR, LocalDate.of(2026, 6, 1), null, grace, LocalDate.of(2026, 8, 5),
        )
        assertEquals(3, periods.size)
        assertEquals(LocalDate.of(2026, 8, 1), periods.last().start)
    }

    @Test fun `leaving mid month truncates the final period`() {
        val periods = Periods.generate(
            BillingAnchor.CALENDAR,
            LocalDate.of(2026, 6, 1),
            endDate = LocalDate.of(2026, 7, 14),
            graceDays = grace,
            upTo = LocalDate.of(2026, 9, 1),
        )
        assertEquals(2, periods.size)
        assertEquals(LocalDate.of(2026, 7, 14), periods.last().end)
    }

    // --- JOINING_DATE anchor ----------------------------------------------

    @Test fun `anniversary cycles run eighteenth to seventeenth`() {
        val periods = Periods.generate(
            BillingAnchor.JOINING_DATE, LocalDate.of(2026, 6, 18), null, grace, LocalDate.of(2026, 8, 20),
        )
        assertEquals(3, periods.size)
        assertEquals(LocalDate.of(2026, 6, 18) to LocalDate.of(2026, 7, 17), periods[0].start to periods[0].end)
        assertEquals(LocalDate.of(2026, 7, 18) to LocalDate.of(2026, 8, 17), periods[1].start to periods[1].end)
        assertEquals(LocalDate.of(2026, 8, 18) to LocalDate.of(2026, 9, 17), periods[2].start to periods[2].end)
        assertEquals(LocalDate.of(2026, 6, 27), periods[0].dueDate)
    }

    @Test fun `month end joins clamp instead of overflowing`() {
        val periods = Periods.generate(
            BillingAnchor.JOINING_DATE, LocalDate.of(2026, 1, 31), null, grace, LocalDate.of(2026, 3, 5),
        )
        // 31 Jan -> 28 Feb (2026 is not a leap year), not 3 March.
        assertEquals(LocalDate.of(2026, 2, 28), periods[1].start)
        assertEquals(LocalDate.of(2026, 2, 27), periods[0].end)
    }
}
