package pk.groscale.feeregister.data.db

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface BatchDao {
    @Insert suspend fun insert(batch: BatchEntity): Long
    @Update suspend fun update(batch: BatchEntity)
    @Query("SELECT * FROM batches WHERE active = 1 ORDER BY startMinute")
    fun observeActive(): Flow<List<BatchEntity>>
    @Query("SELECT * FROM batches WHERE id = :id")
    suspend fun byId(id: Long): BatchEntity?

    /** One-shot read for the reminder scheduler, which has no lifecycle to observe from. */
    @Query("SELECT * FROM batches WHERE active = 1 ORDER BY startMinute")
    suspend fun allActive(): List<BatchEntity>
}

@Dao
interface FamilyDao {
    @Insert suspend fun insert(family: FamilyEntity): Long
    @Update suspend fun update(family: FamilyEntity)
    @Query("SELECT * FROM families WHERE id = :id")
    suspend fun byId(id: Long): FamilyEntity?
}

@Dao
interface StudentDao {
    @Insert suspend fun insert(student: StudentEntity): Long
    @Update suspend fun update(student: StudentEntity)

    /** Students are never hard-deleted; invoices and payments must survive them. */
    @Query("UPDATE students SET status = 'LEFT' WHERE id = :id")
    suspend fun markLeft(id: Long)

    @Query("SELECT * FROM students WHERE status = 'ACTIVE' ORDER BY name")
    fun observeActive(): Flow<List<StudentEntity>>

    @Query("SELECT * FROM students WHERE id = :id")
    suspend fun byId(id: Long): StudentEntity?
}

@Dao
interface EnrollmentDao {
    @Insert suspend fun insert(enrollment: EnrollmentEntity): Long
    @Update suspend fun update(enrollment: EnrollmentEntity)

    @Query(
        """
        SELECT e.* FROM enrollments e
        JOIN students s ON s.id = e.studentId
        WHERE s.status = 'ACTIVE' AND (e.endDate IS NULL OR e.endDate >= :asOf)
        """
    )
    suspend fun billable(asOf: LocalDate): List<EnrollmentEntity>

    @Query("SELECT * FROM enrollments WHERE batchId = :batchId AND (endDate IS NULL OR endDate >= :asOf)")
    fun observeForBatch(batchId: Long, asOf: LocalDate): Flow<List<EnrollmentEntity>>

    @Query("SELECT * FROM enrollments WHERE endDate IS NULL")
    fun observeCurrent(): Flow<List<EnrollmentEntity>>

    @Query("SELECT * FROM enrollments WHERE id = :id")
    suspend fun byId(id: Long): EnrollmentEntity?

    /**
     * Every enrollment for one student, open or closed. A student in two batches
     * leaves both of them at once, and his old bills still need the batch they
     * were taught in to count class days against.
     */
    @Query("SELECT * FROM enrollments WHERE studentId = :studentId")
    suspend fun forStudent(studentId: Long): List<EnrollmentEntity>
}

@Dao
interface HolidayDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insert(holiday: HolidayEntity): Long
    @Query("DELETE FROM holidays WHERE id = :id") suspend fun delete(id: Long)

    /** Holidays for one batch: its own, plus the global ones. */
    @Query("SELECT date FROM holidays WHERE batchId IS NULL OR batchId = :batchId")
    suspend fun datesFor(batchId: Long): List<LocalDate>
}

@Dao
interface InvoiceDao {
    /** IGNORE + the unique (enrollmentId, periodStart) index is what makes generation idempotent. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfAbsent(invoice: InvoiceEntity): Long

    @Update suspend fun update(invoice: InvoiceEntity)

    /**
     * Corrects a bill after the fee behind it was edited. Only the two priced
     * columns move; the day counts stay as they were snapshotted, so the invoice
     * can still explain itself.
     */
    @Query("UPDATE invoices SET feeSnapshot = :feeSnapshot, amountDue = :amountDue WHERE id = :invoiceId")
    suspend fun reprice(invoiceId: Long, feeSnapshot: Int, amountDue: Int)

    @Query("SELECT MAX(periodStart) FROM invoices WHERE enrollmentId = :enrollmentId")
    suspend fun latestPeriodStart(enrollmentId: Long): LocalDate?

    @Query("SELECT * FROM invoices WHERE periodStart = :periodStart")
    fun observeForPeriod(periodStart: LocalDate): Flow<List<InvoiceEntity>>

    /**
     * Takes the days-not-taught off a bill when a student leaves part-way through
     * it. `amountDue` is deliberately left alone: the bill keeps saying what the
     * parent was shown, and the reduction reads as its own line.
     */
    @Query("UPDATE invoices SET adjustment = :adjustment, adjustmentNote = :note WHERE id = :invoiceId")
    suspend fun adjust(invoiceId: Long, adjustment: Int, note: String?)

    @Query("UPDATE invoices SET lastRemindedAt = :date WHERE id = :invoiceId")
    suspend fun markReminded(invoiceId: Long, date: LocalDate)
}

@Dao
interface PaymentDao {
    @Insert suspend fun insert(payment: PaymentEntity): Long
    @Insert suspend fun insertAllocations(allocations: List<PaymentAllocationEntity>)
    @Update suspend fun update(payment: PaymentEntity)

    /** Deleting a payment cascades its allocations. Never edit an allocation in place. */
    @Query("DELETE FROM payments WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT COALESCE(MAX(receiptNo), 0) + 1 FROM payments")
    suspend fun nextReceiptNo(): Int

    @Query("SELECT COALESCE(SUM(amount), 0) FROM payment_allocations WHERE invoiceId = :invoiceId")
    suspend fun allocatedTo(invoiceId: Long): Int

    /**
     * Everything one student ever handed over, advance credit included - the
     * received side of a leaving settlement.
     *
     * Keyed on studentId, which is what receivePayment writes. A payment booked
     * against a whole family would need the sibling split that the UI does not
     * offer yet; see the Family table.
     */
    @Query("SELECT COALESCE(SUM(amount), 0) FROM payments WHERE studentId = :studentId")
    suspend fun totalForStudent(studentId: Long): Int

    @Transaction
    suspend fun receive(payment: PaymentEntity, allocations: List<PaymentAllocationEntity>): Long {
        val id = insert(payment)
        insertAllocations(allocations.map { it.copy(paymentId = id) })
        return id
    }
}

@Dao
interface AbsenceDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun mark(absence: AbsenceEntity)
    @Query("DELETE FROM absences WHERE enrollmentId = :enrollmentId AND date = :date")
    suspend fun unmark(enrollmentId: Long, date: LocalDate)
    @Query("SELECT COUNT(*) FROM absences WHERE enrollmentId = :enrollmentId AND date BETWEEN :from AND :to")
    suspend fun countBetween(enrollmentId: Long, from: LocalDate, to: LocalDate): Int
}

/**
 * What one enrollment still owes from bills that fell due before the month being
 * viewed. Grouped per enrollment, not per student, because the month list is one
 * row per enrollment - a student in two batches would otherwise have the same
 * arrears counted against both of his rows.
 */
data class Arrears(
    val enrollmentId: Long,
    val months: Int,
    val amount: Int,
    // Identity, so a student who owes from earlier months can still be shown a
    // row in a month where nothing of his falls due - see [ReportDao.observeArrears].
    val studentId: Long,
    val studentName: String,
    val batchName: String,
    val classLabel: String,
    val guardianPhone: String?,
    /** Newest unpaid bill of the group: what a reminder is recorded against. */
    val latestInvoiceId: Long,
    val lastRemindedAt: LocalDate?,
    val hasLeft: Boolean,
)

data class InvoiceWithPaid(
    @Embedded val invoice: InvoiceEntity,
    val paidSoFar: Int,
)

/** Read model for a row in the month list: student + batch + bill + what has been paid. */
data class StudentDueRow(
    @Embedded val invoice: InvoiceEntity,
    val studentId: Long,
    val studentName: String,
    val batchName: String,
    val classLabel: String,
    val guardianPhone: String?,
    val paidSoFar: Int,
    /** He is only still on this screen because he owes something. Say so on the row. */
    val hasLeft: Boolean,
)

data class RollRow(
    val enrollmentId: Long,
    val studentId: Long,
    val studentName: String,
    val absent: Boolean,
    /** Null or blank means there is nobody to tell, so the offer is not made. */
    val guardianPhone: String?,
)

data class StudentEditRow(
    val enrollmentId: Long,
    val studentId: Long,
    val name: String,
    val classLabel: String,
    val fee: Int,
    val batchId: Long,
    val batchName: String,
    val familyId: Long?,
    val guardianPhone: String?,
    val startDate: LocalDate,
    /** Shown, never edited - see [JoiningCycle] and FeeRepository.addStudent. */
    val billingAnchor: String,
    val mergeStubIntoNext: Boolean,
)

/** What is owed right now, for a reminder's one-line summary. */
data class PendingSummary(val amount: Int, val students: Int)

data class ReceiptContext(
    val studentName: String,
    val classLabel: String,
    val batchName: String,
    val guardianPhone: String?,
)

@Dao
interface ReportDao {

    @Query(
        """
        SELECT s.name AS studentName, s.classLabel AS classLabel,
               b.name AS batchName, f.guardianPhone AS guardianPhone
        FROM students s
        JOIN enrollments e ON e.studentId = s.id
        JOIN batches b     ON b.id = e.batchId
        LEFT JOIN families f ON f.id = s.familyId
        WHERE s.id = :studentId
        LIMIT 1
        """
    )
    suspend fun receiptContext(studentId: Long): ReceiptContext?

    /** The roll for one batch on one day. `absent` is computed, never stored as presence. */
    @Query(
        """
        SELECT e.id AS enrollmentId, s.id AS studentId, s.name AS studentName,
               EXISTS(SELECT 1 FROM absences a
                      WHERE a.enrollmentId = e.id AND a.date = :date) AS absent,
               f.guardianPhone AS guardianPhone
        FROM enrollments e
        JOIN students s ON s.id = e.studentId
        LEFT JOIN families f ON f.id = s.familyId
        WHERE e.batchId = :batchId
          AND s.status = 'ACTIVE'
          AND e.startDate <= :date
          AND (e.endDate IS NULL OR e.endDate >= :date)
        ORDER BY s.name
        """
    )
    fun observeRoll(batchId: Long, date: LocalDate): Flow<List<RollRow>>

    @Query(
        """
        SELECT e.id AS enrollmentId, s.id AS studentId, s.name AS name,
               s.classLabel AS classLabel, e.fee AS fee,
               e.batchId AS batchId, b.name AS batchName,
               s.familyId AS familyId, f.guardianPhone AS guardianPhone,
               e.startDate AS startDate, e.billingAnchor AS billingAnchor,
               e.mergeStubIntoNext AS mergeStubIntoNext
        FROM enrollments e
        JOIN students s ON s.id = e.studentId
        JOIN batches b  ON b.id = e.batchId
        LEFT JOIN families f ON f.id = s.familyId
        WHERE s.id = :studentId AND e.endDate IS NULL
        LIMIT 1
        """
    )
    suspend fun studentForEdit(studentId: Long): StudentEditRow?
    @Query(
        """
        SELECT i.*,
               s.id                AS studentId,
               s.name              AS studentName,
               b.name              AS batchName,
               s.classLabel        AS classLabel,
               f.guardianPhone     AS guardianPhone,
               COALESCE((SELECT SUM(a.amount) FROM payment_allocations a
                         WHERE a.invoiceId = i.id), 0) AS paidSoFar,
               (s.status != 'ACTIVE') AS hasLeft
        FROM invoices i
        JOIN enrollments e ON e.id = i.enrollmentId
        JOIN students s    ON s.id = e.studentId
        JOIN batches b     ON b.id = e.batchId
        LEFT JOIN families f ON f.id = s.familyId
        WHERE i.dueDate BETWEEN :monthStart AND :monthEnd
          -- A student who has left is off this screen the moment he owes
          -- nothing. While he still owes, he stays: removing a boy must not
          -- quietly write off the money he never paid.
          AND (s.status = 'ACTIVE'
               OR (i.amountDue + i.adjustment) >
                  COALESCE((SELECT SUM(a.amount) FROM payment_allocations a
                            WHERE a.invoiceId = i.id), 0))
        ORDER BY s.name
        """
    )
    fun observeMonth(monthStart: LocalDate, monthEnd: LocalDate): Flow<List<StudentDueRow>>

    /**
     * Unpaid bills falling due BEFORE this month: how many, and how much. Drives
     * both the "2 months due" chip and the amount carried onto the row, so that
     * an unpaid month never quietly disappears from the total.
     *
     * Together with [observeMonth], whose window starts where this one ends,
     * every bill due up to the end of the month is accounted for exactly once.
     */
    @Query(
        """
        SELECT i.enrollmentId AS enrollmentId,
               COUNT(*) AS months,
               SUM((i.amountDue + i.adjustment) -
                   COALESCE((SELECT SUM(a.amount) FROM payment_allocations a
                             WHERE a.invoiceId = i.id), 0)) AS amount,
               s.id            AS studentId,
               s.name          AS studentName,
               b.name          AS batchName,
               s.classLabel    AS classLabel,
               f.guardianPhone AS guardianPhone,
               MAX(i.id)       AS latestInvoiceId,
               MAX(i.lastRemindedAt) AS lastRemindedAt,
               (s.status != 'ACTIVE') AS hasLeft
        FROM invoices i
        JOIN enrollments e ON e.id = i.enrollmentId
        JOIN students s    ON s.id = e.studentId
        JOIN batches b     ON b.id = e.batchId
        LEFT JOIN families f ON f.id = s.familyId
        WHERE i.dueDate < :monthStart
          AND (i.amountDue + i.adjustment) >
              COALESCE((SELECT SUM(a.amount) FROM payment_allocations a
                        WHERE a.invoiceId = i.id), 0)
        GROUP BY i.enrollmentId
        """
    )
    fun observeArrears(monthStart: LocalDate): Flow<List<Arrears>>

    /** Every bill for one enrollment, with what has been paid - the re-pricing input. */
    @Query(
        """
        SELECT i.*,
               COALESCE((SELECT SUM(a.amount) FROM payment_allocations a
                         WHERE a.invoiceId = i.id), 0) AS paidSoFar
        FROM invoices i
        WHERE i.enrollmentId = :enrollmentId
        ORDER BY i.periodStart
        """
    )
    suspend fun invoicesForEnrollment(enrollmentId: Long): List<InvoiceWithPaid>

    /**
     * Every bill for one student, oldest first, with what has been paid against
     * each. Oldest first is what makes it the allocation queue; carrying the
     * settled ones too is what lets a leaving settlement add the whole ledger up.
     */
    @Query(
        """
        SELECT i.*,
               COALESCE((SELECT SUM(a.amount) FROM payment_allocations a
                         WHERE a.invoiceId = i.id), 0) AS paidSoFar
        FROM invoices i
        JOIN enrollments e ON e.id = i.enrollmentId
        WHERE e.studentId = :studentId
        ORDER BY i.periodStart
        """
    )
    suspend fun invoicesForStudent(studentId: Long): List<InvoiceWithPaid>

    @Query("SELECT COUNT(*) FROM students WHERE status = 'ACTIVE'")
    fun observeStudentCount(): Flow<Int>

    /** Due dates still ahead - what a fee-day reminder is scheduled against. */
    @Query("SELECT DISTINCT dueDate FROM invoices WHERE dueDate >= :from ORDER BY dueDate")
    suspend fun dueDatesFrom(from: LocalDate): List<LocalDate>

    /** How many students have a bill falling due on exactly this day. */
    @Query(
        """
        SELECT COUNT(DISTINCT e.studentId) FROM invoices i
        JOIN enrollments e ON e.id = i.enrollmentId
        WHERE i.dueDate = :date
        """
    )
    suspend fun studentsDueOn(date: LocalDate): Int

    /** Everything owed as of [asOf], for the line a reminder carries. */
    @Query(
        """
        SELECT COALESCE(SUM(owed), 0) AS amount, COUNT(DISTINCT studentId) AS students FROM (
            SELECT e.studentId AS studentId,
                   (i.amountDue + i.adjustment) -
                   COALESCE((SELECT SUM(a.amount) FROM payment_allocations a
                             WHERE a.invoiceId = i.id), 0) AS owed
            FROM invoices i
            JOIN enrollments e ON e.id = i.enrollmentId
            WHERE i.dueDate <= :asOf
              AND (i.amountDue + i.adjustment) >
                  COALESCE((SELECT SUM(a.amount) FROM payment_allocations a
                            WHERE a.invoiceId = i.id), 0)
        )
        """
    )
    suspend fun pendingAsOf(asOf: LocalDate): PendingSummary
}
