package pk.groscale.feeregister.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * Where the fee rules and the period rules meet.
 *
 * Both halves were tested alone and both were right; the bill they produced
 * together was not, because a part period measured against itself looks like a
 * whole one. These are the cases that catch that.
 */
class InvoiceGeneratorTest {

    private val fee = 2500

    private fun generate(
        start: LocalDate,
        today: LocalDate,
        end: LocalDate? = null,
        anchor: BillingAnchor = BillingAnchor.CALENDAR,
        holidays: Set<LocalDate> = emptySet(),
    ) = InvoiceGenerator.generate(
        input = EnrollmentInput(
            enrollmentId = 1,
            batchDaysOfWeekMask = MON_TO_SAT,
            fee = fee,
            startDate = start,
            endDate = end,
            anchor = anchor,
            mergeStubIntoNext = false,
            holidays = holidays,
        ),
        today = today,
        graceDays = 10,
        roundingStep = 50,
    )

    @Test fun `a whole month is the whole fee`() {
        val bills = generate(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 20))
        assertEquals(1, bills.size)
        assertEquals(fee, bills[0].amountDue)
        assertEquals(26, bills[0].classDaysInPeriod)
        assertEquals(26, bills[0].classDaysEnrolled)
    }

    @Test fun `joining mid month prorates against the whole month`() {
        // September 2026 has 26 Mon-Sat class days, 11 of them from the 18th.
        val bills = generate(LocalDate.of(2026, 9, 18), LocalDate.of(2026, 9, 20))
        assertEquals(1, bills.size)
        assertEquals(11, bills[0].classDaysEnrolled)
        assertEquals(26, bills[0].classDaysInPeriod)
        assertEquals(1050, bills[0].amountDue)
        assertTrue("a part month must cost less than a whole one", bills[0].amountDue < fee)
    }

    @Test fun `leaving mid month prorates against the whole month too`() {
        val bills = generate(
            start = LocalDate.of(2026, 8, 1),
            today = LocalDate.of(2026, 9, 20),
            end = LocalDate.of(2026, 9, 14),
        )
        val september = bills.last()
        assertEquals(26, september.classDaysInPeriod)
        assertEquals(12, september.classDaysEnrolled)
        assertEquals(1150, september.amountDue)
    }

    @Test fun `an anniversary cycle is never prorated while it runs whole`() {
        val bills = generate(
            start = LocalDate.of(2026, 9, 18),
            today = LocalDate.of(2026, 11, 1),
            anchor = BillingAnchor.JOINING_DATE,
        )
        assertEquals(2, bills.size)
        bills.forEach { assertEquals(fee, it.amountDue) }
    }

    @Test fun `leaving mid cycle prorates the final anniversary period`() {
        val bills = generate(
            start = LocalDate.of(2026, 9, 18),
            today = LocalDate.of(2026, 11, 1),
            end = LocalDate.of(2026, 10, 31),
            anchor = BillingAnchor.JOINING_DATE,
        )
        val last = bills.last()
        assertEquals(LocalDate.of(2026, 10, 18), last.periodStart)
        assertTrue("a part cycle must cost less than a whole one", last.amountDue < fee)
        assertTrue(last.classDaysEnrolled < last.classDaysInPeriod)
    }

    @Test fun `holidays never reduce a whole month's fee`() {
        val bills = generate(
            start = LocalDate.of(2026, 9, 1),
            today = LocalDate.of(2026, 9, 20),
            holidays = setOf(LocalDate.of(2026, 9, 7), LocalDate.of(2026, 9, 8)),
        )
        assertEquals(fee, bills[0].amountDue)
        assertEquals(24, bills[0].classDaysInPeriod)
    }

    @Test fun `holidays do shrink the denominator a part month is measured against`() {
        // The two holidays fall before he joins, so they leave him a bigger
        // share of a shorter month - and he must not be charged for days the
        // batch never met.
        val bills = generate(
            start = LocalDate.of(2026, 9, 18),
            today = LocalDate.of(2026, 9, 20),
            holidays = setOf(LocalDate.of(2026, 9, 7), LocalDate.of(2026, 9, 8)),
        )
        assertEquals(24, bills[0].classDaysInPeriod)
        assertEquals(11, bills[0].classDaysEnrolled)
        assertEquals(1150, bills[0].amountDue)
    }

    @Test fun `a period taught to nobody produces no bill at all`() {
        val bills = generate(
            start = LocalDate.of(2026, 8, 1),
            today = LocalDate.of(2026, 9, 20),
            end = LocalDate.of(2026, 8, 31),
        )
        assertEquals(1, bills.size)
        assertEquals(LocalDate.of(2026, 8, 1), bills[0].periodStart)
    }

    @Test fun `never bills a month that has not started`() {
        val bills = generate(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 20))
        assertTrue(bills.none { it.periodStart > LocalDate.of(2026, 9, 20) })
    }
}
