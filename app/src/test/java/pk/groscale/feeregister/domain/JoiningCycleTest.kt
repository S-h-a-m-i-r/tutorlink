package pk.groscale.feeregister.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * The mid-month joining rule from v1-pitch.md section 3, both ways.
 *
 * September 2026 for a Mon-Sat batch: 26 class days, 11 of them from the 18th.
 */
class JoiningCycleTest {

    private val the18th = LocalDate.of(2026, 9, 18)
    private val fee = 2500

    private fun bills(cycle: JoiningCycle, from: LocalDate = the18th) =
        previewBills(cycle, from, MON_TO_SAT, fee, graceDays = 10, roundingStep = 50)

    // --- student 1: 18-30 Sep, then the 1st of each month -------------------

    @Test fun `part month is prorated on class days, then whole months follow`() {
        val (stub, october) = bills(JoiningCycle.PART_MONTH_NOW)

        assertEquals(the18th, stub.periodStart)
        assertEquals(LocalDate.of(2026, 9, 30), stub.periodEnd)
        assertEquals(11, stub.classDaysEnrolled)
        assertEquals(26, stub.classDaysInPeriod)
        // 2500 x 11 / 26 = 1057.7, to the nearest 50.
        assertEquals(1050, stub.amountDue)

        assertEquals(LocalDate.of(2026, 10, 1), october.periodStart)
        assertEquals(LocalDate.of(2026, 10, 31), october.periodEnd)
        assertEquals(fee, october.amountDue)
    }

    // --- student 2: 18th to 18th, for ever ---------------------------------

    @Test fun `his own date bills a whole cycle every time, never prorated`() {
        val (first, second) = bills(JoiningCycle.OWN_DATE)

        assertEquals(the18th, first.periodStart)
        assertEquals(LocalDate.of(2026, 10, 17), first.periodEnd)
        assertEquals(fee, first.amountDue)

        assertEquals(LocalDate.of(2026, 10, 18), second.periodStart)
        assertEquals(LocalDate.of(2026, 11, 17), second.periodEnd)
        assertEquals(fee, second.amountDue)
    }

    // --- the teacher must not lose money either way -------------------------

    @Test fun `merging the part month moves only the due date, never the money`() {
        val separate = bills(JoiningCycle.PART_MONTH_NOW)
        val merged = bills(JoiningCycle.PART_MONTH_WITH_NEXT)

        assertEquals(separate.map { it.amountDue }, merged.map { it.amountDue })
        assertEquals(separate.map { it.periodStart }, merged.map { it.periodStart })
        // Both bills are collected together, on the later of the two dates.
        assertEquals(merged[1].dueDate, merged[0].dueDate)
        assertTrue(separate[0].dueDate.isBefore(separate[1].dueDate))
    }

    @Test fun `the first two months cost the same whichever cycle is chosen`() {
        // 11 days of September plus October, against a cycle that runs 18 to 18
        // and has been paid once: neither parent is asked for more than a month
        // of teaching, and the teacher is short on neither.
        val calendar = bills(JoiningCycle.PART_MONTH_NOW)
        assertEquals(1050 + 2500, calendar.sumOf { it.amountDue })
        assertEquals(2500 + 2500, bills(JoiningCycle.OWN_DATE).sumOf { it.amountDue })
        // The anniversary student has simply paid further ahead: his second bill
        // covers 18 Oct - 17 Nov, which the calendar student has not been billed
        // for yet. Nobody is over- or under-charged for a day of teaching.
        assertEquals(LocalDate.of(2026, 11, 17), bills(JoiningCycle.OWN_DATE).last().periodEnd)
        assertEquals(LocalDate.of(2026, 10, 31), calendar.last().periodEnd)
    }

    // --- when the question is worth asking ---------------------------------

    @Test fun `joining on the 1st bills identically whichever cycle is picked`() {
        val first = LocalDate.of(2026, 9, 1)
        val calendar = bills(JoiningCycle.PART_MONTH_NOW, first)
        val ownDate = bills(JoiningCycle.OWN_DATE, first)

        assertEquals(calendar.map { it.periodStart }, ownDate.map { it.periodStart })
        assertEquals(calendar.map { it.periodEnd }, ownDate.map { it.periodEnd })
        assertEquals(calendar.map { it.amountDue }, ownDate.map { it.amountDue })
        assertFalse(JoiningCycle.isAskedFor(first))
    }

    @Test fun `the question is asked on every other day`() {
        assertTrue(JoiningCycle.isAskedFor(the18th))
        assertTrue(JoiningCycle.isAskedFor(LocalDate.of(2026, 9, 2)))
        assertTrue(JoiningCycle.isAskedFor(LocalDate.of(2026, 9, 30)))
    }

    // --- the storage round trip --------------------------------------------

    @Test fun `every cycle survives being written and read back`() {
        for (cycle in JoiningCycle.entries) {
            assertEquals(cycle, JoiningCycle.of(cycle.anchor, cycle.mergeStubIntoNext))
        }
    }

    @Test fun `the meaningless combination falls back rather than throwing`() {
        // An anniversary cycle has no stub to merge. Older backups predate the
        // choice, so this has to read as something rather than blow up.
        assertEquals(
            JoiningCycle.PART_MONTH_NOW,
            JoiningCycle.of(BillingAnchor.JOINING_DATE, mergeStubIntoNext = true),
        )
    }

    @Test fun `a month-end join clamps instead of skipping February`() {
        val jan31 = LocalDate.of(2027, 1, 31)
        val (first, second) = bills(JoiningCycle.OWN_DATE, jan31)
        assertEquals(LocalDate.of(2027, 2, 27), first.periodEnd)
        assertEquals(LocalDate.of(2027, 2, 28), second.periodStart)
    }
}
