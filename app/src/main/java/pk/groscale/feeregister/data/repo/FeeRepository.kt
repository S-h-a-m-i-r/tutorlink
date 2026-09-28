package pk.groscale.feeregister.data.repo

import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import pk.groscale.feeregister.data.db.AppDatabase
import pk.groscale.feeregister.data.db.AbsenceEntity
import pk.groscale.feeregister.data.db.Arrears
import pk.groscale.feeregister.data.db.BatchEntity
import pk.groscale.feeregister.data.db.EnrollmentEntity
import pk.groscale.feeregister.data.db.FamilyEntity
import pk.groscale.feeregister.data.db.InvoiceEntity
import pk.groscale.feeregister.data.db.PaymentAllocationEntity
import pk.groscale.feeregister.data.db.PaymentEntity
import pk.groscale.feeregister.data.db.StudentDueRow
import pk.groscale.feeregister.data.db.StudentEntity
import pk.groscale.feeregister.data.prefs.Profile
import pk.groscale.feeregister.data.prefs.ProfileStore
import pk.groscale.feeregister.domain.BillingAnchor
import pk.groscale.feeregister.domain.EnrollmentInput
import pk.groscale.feeregister.domain.FeeCalculator
import pk.groscale.feeregister.domain.InvoiceGenerator
import pk.groscale.feeregister.domain.JoiningCycle
import pk.groscale.feeregister.domain.LeavingInvoice
import pk.groscale.feeregister.domain.Payable
import pk.groscale.feeregister.domain.Repriceable
import pk.groscale.feeregister.domain.Settlement
import pk.groscale.feeregister.domain.allocate
import pk.groscale.feeregister.domain.reprice
import pk.groscale.feeregister.domain.settleLeaving
import pk.groscale.feeregister.util.ReceiptData
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val NoteDate: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM yyyy")

class FeeRepository(
    private val db: AppDatabase,
    private val profileStore: ProfileStore,
) {
    val profile: Flow<Profile> = profileStore.profile
    fun observeBatches(): Flow<List<BatchEntity>> = db.batches().observeActive()
    fun observeStudents(): Flow<List<StudentEntity>> = db.students().observeActive()
    fun observeStudentCount(): Flow<Int> = db.reports().observeStudentCount()
    fun observeEnrollments(): Flow<List<EnrollmentEntity>> = db.enrollments().observeCurrent()

    fun observeMonth(month: LocalDate): Flow<List<StudentDueRow>> =
        db.reports().observeMonth(month.withDayOfMonth(1), month.withDayOfMonth(month.lengthOfMonth()))

    /** Keyed by enrollment, which is what a row in the month list is. */
    fun observeArrears(month: LocalDate): Flow<Map<Long, Arrears>> =
        db.reports().observeArrears(month.withDayOfMonth(1))
            .map { list -> list.associateBy(Arrears::enrollmentId) }

    // --- setup ------------------------------------------------------------

    suspend fun saveTuition(tuitionName: String, teacherName: String, phone: String) =
        profileStore.saveTuition(tuitionName.trim(), teacherName.trim(), phone.trim())

    suspend fun markSetupComplete() = profileStore.markSetupComplete()

    suspend fun setReminders(attendance: Boolean, fee: Boolean) =
        profileStore.setReminders(attendance, fee)

    suspend fun recordBackup() = profileStore.recordBackup(System.currentTimeMillis())

    suspend fun createBatch(
        name: String,
        classLabel: String,
        daysOfWeekMask: Int,
        startMinute: Int,
        defaultFee: Int,
    ): Long = db.batches().insert(
        BatchEntity(
            name = name.trim(),
            classLabel = classLabel.trim(),
            daysOfWeekMask = daysOfWeekMask,
            startMinute = startMinute,
            endMinute = startMinute + 120,
            defaultFee = defaultFee,
            createdAt = LocalDate.now(),
        ),
    )

    /**
     * Adds a student and enrolls him in one batch. The guardian's phone lives on
     * a Family even for an only child, so that adding a sibling later is just
     * pointing a second student at the same family - no migration, no re-typing.
     *
     * [cycle] is the mid-month joining answer the parents gave, and it is fixed
     * here: the periods it decides are the boundaries every later bill is cut
     * on, so changing it afterwards would strand the invoices already written.
     */
    suspend fun addStudent(
        name: String,
        classLabel: String,
        guardianPhone: String,
        batchId: Long,
        fee: Int,
        startDate: LocalDate = LocalDate.now(),
        cycle: JoiningCycle = JoiningCycle.PART_MONTH_NOW,
    ): Long {
        val familyId = db.families().insert(
            FamilyEntity(
                name = name.trim(),
                guardianName = "",
                guardianPhone = guardianPhone.trim(),
            ),
        )
        val studentId = db.students().insert(
            StudentEntity(
                name = name.trim(),
                classLabel = classLabel.trim(),
                familyId = familyId,
                joinedAt = startDate,
            ),
        )
        db.enrollments().insert(
            EnrollmentEntity(
                studentId = studentId,
                batchId = batchId,
                fee = fee,
                startDate = startDate,
                billingAnchor = cycle.anchor.name,
                mergeStubIntoNext = cycle.mergeStubIntoNext,
            ),
        )
        generateInvoices()
        return studentId
    }

    // --- invoice generation ------------------------------------------------

    /**
     * Runs on every app open. Idempotent: INSERT OR IGNORE against the unique
     * (enrollmentId, periodStart) index means re-running it can never double-bill.
     */
    suspend fun generateInvoices(today: LocalDate = LocalDate.now()) = withContext(Dispatchers.Default) {
        val p = profileStore.profile.first()
        val tuitionName = p.tuitionName
        val phone = p.teacherPhone.ifBlank { null }

        for (e in db.enrollments().billable(today)) {
            val batch = db.batches().byId(e.batchId) ?: continue
            val holidays = db.holidays().datesFor(e.batchId).toSet()

            val input = EnrollmentInput(
                enrollmentId = e.id,
                batchDaysOfWeekMask = batch.daysOfWeekMask,
                fee = e.fee,
                startDate = e.startDate,
                endDate = e.endDate,
                anchor = runCatching { BillingAnchor.valueOf(e.billingAnchor) }
                    .getOrDefault(BillingAnchor.CALENDAR),
                mergeStubIntoNext = e.mergeStubIntoNext,
                holidays = holidays,
            )

            for (g in InvoiceGenerator.generate(input, today, p.graceDays, p.roundingStep)) {
                db.invoices().insertIfAbsent(
                    InvoiceEntity(
                        enrollmentId = g.enrollmentId,
                        periodStart = g.periodStart,
                        periodEnd = g.periodEnd,
                        dueDate = g.dueDate,
                        feeSnapshot = g.feeSnapshot,
                        classDaysInPeriod = g.classDaysInPeriod,
                        classDaysEnrolled = g.classDaysEnrolled,
                        amountDue = g.amountDue,
                        tuitionNameSnapshot = tuitionName,
                        teacherPhoneSnapshot = phone,
                    ),
                )
            }
        }
    }

    // --- attendance --------------------------------------------------------

    fun observeRoll(batchId: Long, date: LocalDate) = db.reports().observeRoll(batchId, date)

    /**
     * Only absences are written. Toggling is the whole interaction: everyone is
     * present until the teacher taps someone, which is two taps on a normal day
     * instead of twenty.
     */
    suspend fun setAbsent(enrollmentId: Long, date: LocalDate, absent: Boolean) {
        if (absent) {
            db.absences().mark(AbsenceEntity(enrollmentId = enrollmentId, date = date))
        } else {
            db.absences().unmark(enrollmentId, date)
        }
    }

    // --- editing -----------------------------------------------------------

    suspend fun studentForEdit(studentId: Long) = db.reports().studentForEdit(studentId)

    /**
     * Editing a fee carries onto the bills that change can still honestly reach -
     * see [reprice]. Writing only the enrollment was the older behaviour and it
     * left the register disagreeing with itself: the student page showed the
     * corrected fee while this month's total was still built from the old one.
     *
     * One transaction, so a failure part-way cannot leave a student's name saved
     * against bills priced from a fee that was never stored.
     */
    suspend fun updateStudent(
        studentId: Long,
        enrollmentId: Long,
        name: String,
        guardianPhone: String,
        fee: Int,
        today: LocalDate = LocalDate.now(),
    ) {
        require(name.isNotBlank()) { "A student needs a name" }
        require(fee > 0) { "A fee must be more than zero" }

        val student = db.students().byId(studentId) ?: return
        // Read before the transaction opens: DataStore is not part of it.
        val roundingStep = profileStore.profile.first().roundingStep

        db.withTransaction {
            db.students().update(student.copy(name = name.trim()))
            student.familyId?.let { familyId ->
                db.families().byId(familyId)?.let { family ->
                    db.families().update(family.copy(guardianPhone = guardianPhone.trim()))
                }
            }

            val enrollment = db.enrollments().byId(enrollmentId)
            if (enrollment != null && enrollment.fee != fee) {
                db.enrollments().update(enrollment.copy(fee = fee))
                repriceBills(enrollmentId, fee, today, roundingStep)
            }
        }
    }

    /** Applies a corrected fee to the bills [reprice] allows it to reach. */
    private suspend fun repriceBills(
        enrollmentId: Long,
        fee: Int,
        today: LocalDate,
        roundingStep: Int,
    ) {
        val existing = db.reports().invoicesForEnrollment(enrollmentId).map {
            Repriceable(
                invoiceId = it.invoice.id,
                periodEnd = it.invoice.periodEnd,
                feeSnapshot = it.invoice.feeSnapshot,
                classDaysInPeriod = it.invoice.classDaysInPeriod,
                classDaysEnrolled = it.invoice.classDaysEnrolled,
                paidSoFar = it.paidSoFar,
            )
        }
        for (r in reprice(existing, fee, today, roundingStep)) {
            db.invoices().reprice(r.invoiceId, r.feeSnapshot, r.amountDue)
        }
    }

    // --- leaving ------------------------------------------------------------

    /**
     * What marking this student as left today would settle, without writing
     * anything. The teacher sees this before he decides, because "has he cleared
     * his dues?" and "do I owe him anything back?" are the two questions he
     * cannot answer once the student has walked out of the door.
     */
    suspend fun previewLeaving(
        studentId: Long,
        leavingOn: LocalDate = LocalDate.now(),
    ): Settlement = withContext(Dispatchers.Default) { settle(studentId, leavingOn) }

    /**
     * Soft: invoices and payments must survive a student leaving.
     *
     * Ends every open enrollment he has - a boy in two batches leaves both - and
     * writes the settlement's prorations, so the register afterwards shows what
     * he was really taught rather than the whole month he was billed for on the
     * 1st. Returns the settlement so the caller can tell the teacher where the
     * two of them stand.
     */
    suspend fun markStudentLeft(
        studentId: Long,
        leavingOn: LocalDate = LocalDate.now(),
    ): Settlement = withContext(Dispatchers.Default) {
        val settlement = settle(studentId, leavingOn)
        db.withTransaction {
            db.students().markLeft(studentId)
            for (e in db.enrollments().forStudent(studentId)) {
                if (e.endDate == null) db.enrollments().update(e.copy(endDate = leavingOn))
            }
            for (p in settlement.prorations) {
                db.invoices().adjust(
                    invoiceId = p.invoiceId,
                    adjustment = p.adjustment,
                    note = "Left ${leavingOn.format(NoteDate)} · " +
                        "${p.classDaysTaught} of ${p.classDaysInPeriod} days taught",
                )
            }
        }
        settlement
    }

    /** Everything the settlement needs to count class days for one enrollment. */
    private data class BatchDays(val daysOfWeekMask: Int, val holidays: Set<LocalDate>)

    /**
     * The one place the leaving numbers are worked out, so the preview the
     * teacher agreed to and the rows that get written cannot disagree.
     */
    private suspend fun settle(studentId: Long, leavingOn: LocalDate): Settlement {
        val roundingStep = profileStore.profile.first().roundingStep

        val batchDays = db.enrollments().forStudent(studentId).associate { e ->
            val batch = db.batches().byId(e.batchId)
            e.id to BatchDays(
                daysOfWeekMask = batch?.daysOfWeekMask ?: 0,
                holidays = db.holidays().datesFor(e.batchId).toSet(),
            )
        }

        val invoices = db.reports().invoicesForStudent(studentId).map { row ->
            val inv = row.invoice
            val days = batchDays[inv.enrollmentId]
            LeavingInvoice(
                invoiceId = inv.id,
                periodStart = inv.periodStart,
                periodEnd = inv.periodEnd,
                feeSnapshot = inv.feeSnapshot,
                classDaysInPeriod = inv.classDaysInPeriod,
                classDaysTaught = if (days == null) inv.classDaysEnrolled else FeeCalculator.classDays(
                    daysOfWeekMask = days.daysOfWeekMask,
                    from = inv.periodStart,
                    to = minOf(inv.periodEnd, leavingOn),
                    holidays = days.holidays,
                ),
                amountDue = inv.amountDue,
                adjustment = inv.adjustment,
            )
        }

        return settleLeaving(
            invoices = invoices,
            received = db.payments().totalForStudent(studentId),
            leavingOn = leavingOn,
            roundingStep = roundingStep,
        )
    }

    suspend fun markReminded(invoiceId: Long) =
        db.invoices().markReminded(invoiceId, LocalDate.now())

    // --- payments ----------------------------------------------------------

    /**
     * Receives money against a student's bills, oldest first. Returns the
     * receipt number so the caller can show or share it.
     */
    suspend fun receivePayment(
        studentId: Long,
        amount: Int,
        note: String? = null,
    ): ReceiptData? = withContext(Dispatchers.Default) {
        require(amount > 0) { "A payment must be more than zero" }

        val unpaid = db.reports().invoicesForStudent(studentId)
        val payables = unpaid.map {
            Payable(
                invoiceId = it.invoice.id,
                net = it.invoice.amountDue + it.invoice.adjustment,
                alreadyPaid = it.paidSoFar,
            )
        }
        val result = allocate(amount, payables)
        val receiptNo = db.payments().nextReceiptNo()

        db.payments().receive(
            payment = PaymentEntity(
                studentId = studentId,
                amount = amount,
                date = LocalDate.now(),
                note = note,
                receiptNo = receiptNo,
                unallocated = result.unallocated,
            ),
            allocations = result.allocations.map {
                PaymentAllocationEntity(paymentId = 0, invoiceId = it.invoiceId, amount = it.amount)
            },
        )

        // Build the receipt against the newest bill this payment touched: that is
        // the month the parent will recognise.
        val touched = result.allocations.map { it.invoiceId }.toSet()
        val target = unpaid.lastOrNull { it.invoice.id in touched } ?: unpaid.lastOrNull()
        val ctx = db.reports().receiptContext(studentId)
        val profile = profileStore.profile.first()

        if (target == null || ctx == null) return@withContext null

        val inv = target.invoice
        val outstandingBefore = payables.sumOf { it.outstanding }

        ReceiptData(
            tuitionName = profile.tuitionName,
            teacherPhone = profile.teacherPhone.ifBlank { null },
            receiptNo = receiptNo,
            date = LocalDate.now(),
            studentName = ctx.studentName,
            classAndBatch = listOf(ctx.classLabel, ctx.batchName).filter { it.isNotBlank() }.joinToString(" · "),
            monthLabel = inv.periodStart.format(java.time.format.DateTimeFormatter.ofPattern("MMMM yyyy")),
            classDaysEnrolled = inv.classDaysEnrolled,
            classDaysInPeriod = inv.classDaysInPeriod,
            fee = inv.feeSnapshot,
            previousBalance = (outstandingBefore - (inv.amountDue + inv.adjustment - target.paidSoFar))
                .coerceAtLeast(0),
            discount = -inv.adjustment.coerceAtMost(0),
            received = amount,
            balance = (outstandingBefore - amount).coerceAtLeast(0),
        )
    }
}
