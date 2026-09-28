package pk.groscale.feeregister.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * The two bugs this rule exists to close:
 *  - a fee typed wrong and corrected the same month left the month view priced
 *    from the old fee while the student page showed the new one;
 *  - a fee raised later must not reach back and re-price a month already billed.
 */
class RepricingTest {

    private val today = LocalDate.of(2026, 9, 5)
    private val roundingStep = 50

    /** A full September calendar month for a Mon-Sat batch: 26 class days. */
    private fun bill(
        id: Long = 1,
        periodEnd: LocalDate = LocalDate.of(2026, 9, 30),
        fee: Int = 2000,
        inPeriod: Int = 26,
        enrolled: Int = 26,
        paidSoFar: Int = 0,
    ) = Repriceable(id, periodEnd, fee, inPeriod, enrolled, paidSoFar)

    private fun repriceTo(newFee: Int, vararg bills: Repriceable) =
        reprice(bills.toList(), newFee, today, roundingStep)

    @Test fun `a correction reaches the bill for the month still running`() {
        val out = repriceTo(1500, bill(fee = 2000))
        assertEquals(listOf(Repriced(1, 1500, 1500)), out)
    }

    @Test fun `a rise reaches it just the same`() {
        val out = repriceTo(2500, bill(fee = 2000))
        assertEquals(listOf(Repriced(1, 2500, 2500)), out)
    }

    @Test fun `a closed month keeps the fee it was billed at`() {
        val august = bill(periodEnd = LocalDate.of(2026, 8, 31))
        assertTrue(repriceTo(2500, august).isEmpty())
    }

    @Test fun `a bill that money has landed on is left alone`() {
        assertTrue(repriceTo(1500, bill(paidSoFar = 500)).isEmpty())
    }

    @Test fun `a period ending today is still open`() {
        val out = repriceTo(1500, bill(periodEnd = today))
        assertEquals(1, out.size)
    }

    @Test fun `a mid-month stub stays prorated at the new fee`() {
        // Joined on the 11th: 16 of September's 26 class days.
        val out = repriceTo(2500, bill(fee = 2000, enrolled = 16))
        // 2500 x 16 / 26 = 1538.5, rounded to the nearest 50.
        assertEquals(listOf(Repriced(1, 2500, 1550)), out)
    }

    @Test fun `an unchanged fee writes nothing`() {
        assertTrue(repriceTo(2000, bill(fee = 2000)).isEmpty())
    }

    @Test fun `only the open unpaid bills move`() {
        val out = repriceTo(
            2500,
            bill(id = 1, periodEnd = LocalDate.of(2026, 7, 31)),
            bill(id = 2, periodEnd = LocalDate.of(2026, 8, 31), paidSoFar = 2000),
            bill(id = 3, periodEnd = LocalDate.of(2026, 9, 30)),
        )
        assertEquals(listOf(3L), out.map { it.invoiceId })
    }

    /**
     * The teacher's scenario, end to end: 2,000 billed in August and part paid,
     * then the fee goes up by 500 in September. August must stay 500 short at the
     * old rate, September must bill the new one - 3,000 owed in total.
     */
    @Test fun `a rise bills the new month and leaves last month's shortfall alone`() {
        val august = bill(id = 1, periodEnd = LocalDate.of(2026, 8, 31), fee = 2000, paidSoFar = 1500)
        val september = bill(id = 2, periodEnd = LocalDate.of(2026, 9, 30), fee = 2000)

        val out = repriceTo(2500, august, september)

        assertEquals(listOf(Repriced(2, 2500, 2500)), out)
        val augustStillOwed = august.feeSnapshot - august.paidSoFar
        assertEquals(3000, augustStillOwed + out.single().amountDue)
    }
}
