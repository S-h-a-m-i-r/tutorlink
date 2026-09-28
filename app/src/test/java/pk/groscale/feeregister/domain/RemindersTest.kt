package pk.groscale.feeregister.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime

/** September 2026: the 7th is a Monday, the 13th a Sunday. */
class RemindersTest {

    private val evening = ReminderBatch(1, "Evening Batch", MON_TO_SAT, startMinute = 16 * 60)
    private val morning = ReminderBatch(2, "Morning Batch", MON_TO_SAT, startMinute = 8 * 60)
    private val sundayOnly = ReminderBatch(
        3, "Sunday Batch", daysOfWeekMaskOf(DayOfWeek.SUNDAY), startMinute = 10 * 60,
    )

    private fun at(day: Int, hour: Int, minute: Int = 0) =
        LocalDateTime.of(2026, 9, day, hour, minute)

    // --- attendance ---------------------------------------------------------

    @Test fun `fires at the batch's own start time later the same day`() {
        assertEquals(at(7, 16), Reminders.nextAttendanceAt(listOf(evening), at(7, 9)))
    }

    @Test fun `rolls to tomorrow once today's sitting has passed`() {
        assertEquals(at(8, 16), Reminders.nextAttendanceAt(listOf(evening), at(7, 17)))
    }

    @Test fun `skips the day the batch does not meet`() {
        // Saturday 12th 17:00 -> Sunday is not a class day -> Monday 14th.
        assertEquals(at(14, 16), Reminders.nextAttendanceAt(listOf(evening), at(12, 17)))
    }

    @Test fun `a once-a-week batch waits a whole week`() {
        assertEquals(at(20, 10), Reminders.nextAttendanceAt(listOf(sundayOnly), at(13, 11)))
    }

    @Test fun `the earliest of several batches wins`() {
        assertEquals(at(8, 8), Reminders.nextAttendanceAt(listOf(evening, morning), at(7, 17)))
    }

    @Test fun `never returns the moment it was asked about, so it cannot loop`() {
        val next = Reminders.nextAttendanceAt(listOf(evening), at(7, 16))
        assertEquals(at(8, 16), next)
        assertTrue(next!!.isAfter(at(7, 16)))
    }

    @Test fun `no batches means no reminder`() {
        assertNull(Reminders.nextAttendanceAt(emptyList(), at(7, 9)))
    }

    @Test fun `a batch with no days ticked never fires and does not hang`() {
        val noDays = ReminderBatch(4, "Broken", daysOfWeekMask = 0, startMinute = 600)
        assertNull(Reminders.nextAttendanceAt(listOf(noDays), at(7, 9)))
    }

    @Test fun `only the batches actually sitting at that moment are named`() {
        val sitting = Reminders.batchesStartingAt(listOf(evening, morning, sundayOnly), at(7, 16))
        assertEquals(listOf("Evening Batch"), sitting.map { it.name })
    }

    @Test fun `two batches at the same hour are one reminder, not two`() {
        val twin = evening.copy(batchId = 9, name = "Second Evening")
        val sitting = Reminders.batchesStartingAt(listOf(evening, twin), at(7, 16))
        assertEquals(2, sitting.size)
    }

    @Test fun `a batch is not named on a day it does not meet`() {
        assertTrue(Reminders.batchesStartingAt(listOf(evening), at(13, 16)).isEmpty())
    }

    // --- fee day ------------------------------------------------------------

    @Test fun `fee day is the earliest due date still ahead`() {
        val dates = listOf(LocalDate.of(2026, 9, 16), LocalDate.of(2026, 10, 10))
        assertEquals(at(16, 9), Reminders.nextFeeDayAt(dates, at(7, 9), hour = 9))
    }

    @Test fun `a due date earlier today has already been and gone`() {
        val dates = listOf(LocalDate.of(2026, 9, 7), LocalDate.of(2026, 9, 16))
        assertEquals(at(16, 9), Reminders.nextFeeDayAt(dates, at(7, 10), hour = 9))
    }

    @Test fun `today still counts before the reminder hour`() {
        val dates = listOf(LocalDate.of(2026, 9, 7))
        assertEquals(at(7, 9), Reminders.nextFeeDayAt(dates, at(7, 6), hour = 9))
    }

    @Test fun `nothing due means nothing scheduled`() {
        assertNull(Reminders.nextFeeDayAt(emptyList(), at(7, 9), hour = 9))
        assertNull(Reminders.nextFeeDayAt(listOf(LocalDate.of(2026, 9, 1)), at(7, 9), hour = 9))
    }
}
