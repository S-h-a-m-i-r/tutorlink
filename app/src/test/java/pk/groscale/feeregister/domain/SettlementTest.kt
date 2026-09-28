package pk.groscale.feeregister.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * The leave-mid-period rule, which design.md section 12 left open as "refund,
 * prorate, or forfeit". Prorate, and say which way the money goes.
 *
 * September 2026, Mon-Sat: 26 class days, 12 of them up to and including the 14th.
 */
class SettlementTest {

    private val fee = 2500
    private val the14th = LocalDate.of(2026, 9, 14)

    private fun september(
        amountDue: Int = fee,
        adjustment: Int = 0,
        classDaysTaught: Int = 12,
    ) = LeavingInvoice(
        invoiceId = 2,
        periodStart = LocalDate.of(2026, 9, 1),
        periodEnd = LocalDate.of(2026, 9, 30),
        feeSnapshot = fee,
        classDaysInPeriod = 26,
        classDaysTaught = classDaysTaught,
        amountDue = amountDue,
        adjustment = adjustment,
    )

    private fun august(amountDue: Int = fee) = LeavingInvoice(
        invoiceId = 1,
        periodStart = LocalDate.of(2026, 8, 1),
        periodEnd = LocalDate.of(2026, 8, 31),
        feeSnapshot = fee,
        classDaysInPeriod = 26,
        classDaysTaught = 26,
        amountDue = amountDue,
        adjustment = 0,
    )

    private fun settle(vararg invoices: LeavingInvoice, received: Int) =
        settleLeaving(invoices.toList(), received, the14th, roundingStep = 50)

    // --- he has paid too much: the teacher owes him -------------------------

    @Test fun `paying the whole month then leaving mid month is money back`() {
        // 2500 x 12 / 26 = 1153.8, to the nearest 50.
        val s = settle(september(), received = fee)
        assertEquals(1150, s.billed)
        assertEquals(1350, s.toGiveBack)
        assertEquals(0, s.duesToCollect)
    }

    @Test fun `advance credit comes back too`() {
        // He paid two months up front and leaves half way through the first.
        val s = settle(september(), received = 5000)
        assertEquals(3850, s.toGiveBack)
    }

    // --- he has not paid enough: the teacher still collects ------------------

    @Test fun `leaving does not wipe what he already owes`() {
        val s = settle(august(), september(), received = 0)
        assertEquals(2500 + 1150, s.billed)
        assertEquals(3650, s.duesToCollect)
        assertEquals(0, s.toGiveBack)
    }

    @Test fun `an unpaid earlier month survives an overpaid last one`() {
        // August unpaid, September paid in full, then he leaves on the 14th.
        val s = settle(august(), september(), received = fee)
        assertEquals(3650, s.billed)
        assertEquals(1150, s.duesToCollect)
    }

    @Test fun `settled both ways reads as clear`() {
        val s = settle(september(), received = 1150)
        assertTrue(s.isClear)
        assertEquals(0, s.duesToCollect)
        assertEquals(0, s.toGiveBack)
    }

    // --- what leaving must never do -----------------------------------------

    @Test fun `a month taught in full is never cut`() {
        val s = settle(august(), received = 0)
        assertTrue(s.prorations.isEmpty())
        assertEquals(fee, s.billed)
    }

    @Test fun `leaving on the last day of the period changes nothing`() {
        val s = settleLeaving(
            listOf(september(classDaysTaught = 26)),
            received = 0,
            leavingOn = LocalDate.of(2026, 9, 30),
            roundingStep = 50,
        )
        assertTrue(s.prorations.isEmpty())
        assertEquals(fee, s.billed)
    }

    @Test fun `a bill is never raised on the way out`() {
        // Already prorated down to a stub; leaving must not push it back up.
        val s = settle(september(amountDue = 900, classDaysTaught = 12), received = 0)
        assertTrue(s.prorations.isEmpty())
        assertEquals(900, s.billed)
    }

    @Test fun `a month he was never taught in costs nothing`() {
        val s = settle(september(classDaysTaught = 0), received = fee)
        assertEquals(0, s.billed)
        assertEquals(fee, s.toGiveBack)
    }

    // --- the write-back -----------------------------------------------------

    @Test fun `the adjustment is what makes the bill net out to the new amount`() {
        val p = settle(september(), received = 0).prorations.single()
        assertEquals(2, p.invoiceId)
        assertEquals(-1350, p.adjustment)
        assertEquals(fee, p.amountBefore)
        assertEquals(1150, p.amountAfter)
        assertEquals(p.amountAfter, september().amountDue + p.adjustment)
    }

    @Test fun `settling twice lands on the same numbers`() {
        val once = settle(september(), received = fee)
        val stored = once.prorations.single()
        val again = settle(september(adjustment = stored.adjustment), received = fee)

        assertEquals(once.billed, again.billed)
        assertEquals(once.toGiveBack, again.toGiveBack)
        assertTrue("re-settling must not discount the discount", again.prorations.isEmpty())
    }
}
