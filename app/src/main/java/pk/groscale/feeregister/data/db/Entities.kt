package pk.groscale.feeregister.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

/** Schema per data-model.md. Money is always Int (whole rupees). */

@Entity(tableName = "batches")
data class BatchEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val classLabel: String,
    /** Mon = bit 0 ... Sun = bit 6. */
    val daysOfWeekMask: Int,
    val startMinute: Int,
    val endMinute: Int,
    /** Pre-fills the add-student form only. Never the source of truth for a fee. */
    val defaultFee: Int,
    val active: Boolean = true,
    val createdAt: LocalDate,
)

/**
 * Groups students for messaging and receipts only. Sibling discounts are NOT
 * modelled here - a discount is simply a lower [EnrollmentEntity.fee].
 */
@Entity(tableName = "families")
data class FamilyEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val guardianName: String,
    val guardianPhone: String,
)

@Entity(
    tableName = "students",
    foreignKeys = [
        ForeignKey(
            entity = FamilyEntity::class,
            parentColumns = ["id"],
            childColumns = ["familyId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("familyId")],
)
data class StudentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val classLabel: String,
    val familyId: Long? = null,
    /** ACTIVE | LEFT | PAUSED - stored as String so a backup file stays readable. */
    val status: String = "ACTIVE",
    val joinedAt: LocalDate,
    /** The single optional free-text field. Deliberately not an ability rating. */
    val notes: String? = null,
)

/**
 * THE fee lives here - per student, per batch. A student in two batches has two
 * enrollments and pays two fees. Two students in the same batch can pay different
 * amounts, which is the normal case: the old student keeps his old rate.
 */
@Entity(
    tableName = "enrollments",
    foreignKeys = [
        ForeignKey(entity = StudentEntity::class, parentColumns = ["id"], childColumns = ["studentId"]),
        ForeignKey(entity = BatchEntity::class, parentColumns = ["id"], childColumns = ["batchId"]),
    ],
    indices = [
        Index(value = ["studentId", "batchId", "startDate"], unique = true),
        Index("batchId"),
    ],
)
data class EnrollmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val studentId: Long,
    val batchId: Long,
    val fee: Int,
    val startDate: LocalDate,
    val endDate: LocalDate? = null,
    /** CALENDAR | JOINING_DATE */
    val billingAnchor: String = "CALENDAR",
    /** Due-date rule only: collect the mid-month stub together with next month. */
    val mergeStubIntoNext: Boolean = false,
)

@Entity(tableName = "holidays", indices = [Index(value = ["date", "batchId"], unique = true)])
data class HolidayEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: LocalDate,
    /** null = applies to every batch. */
    val batchId: Long? = null,
    val reason: String? = null,
)

/**
 * One bill for one enrollment for one period.
 *
 * The day counts and the fee are SNAPSHOT, not recomputed, for two reasons: the
 * invoice must still explain itself months later even after holidays are edited,
 * and "22 mein se 11 din" printed on a receipt is what ends an argument with a
 * parent.
 */
@Entity(
    tableName = "invoices",
    foreignKeys = [
        ForeignKey(entity = EnrollmentEntity::class, parentColumns = ["id"], childColumns = ["enrollmentId"]),
    ],
    indices = [Index(value = ["enrollmentId", "periodStart"], unique = true)],
)
data class InvoiceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val enrollmentId: Long,
    val periodStart: LocalDate,
    val periodEnd: LocalDate,
    val dueDate: LocalDate,
    val feeSnapshot: Int,
    val classDaysInPeriod: Int,
    val classDaysEnrolled: Int,
    val amountDue: Int,
    /** Signed. Negative = discount or relief the teacher chose to give. */
    val adjustment: Int = 0,
    val adjustmentNote: String? = null,
    /** So a re-shared old receipt still matches what the parent first received. */
    val tuitionNameSnapshot: String,
    val teacherPhoneSnapshot: String? = null,
    /** WhatsApp deep links open one chat at a time, so reminders are sequential. */
    val lastRemindedAt: LocalDate? = null,
)

@Entity(tableName = "payments")
data class PaymentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val familyId: Long? = null,
    val studentId: Long? = null,
    val amount: Int,
    val date: LocalDate,
    val note: String? = null,
    val receiptNo: Int,
    /** Advance credit not yet applied to an invoice. */
    val unallocated: Int = 0,
)

/**
 * Many allocations per payment. This one table is why partial payments, sibling
 * combined payments and arrears are all the same operation rather than three
 * special cases.
 */
@Entity(
    tableName = "payment_allocations",
    foreignKeys = [
        ForeignKey(
            entity = PaymentEntity::class, parentColumns = ["id"], childColumns = ["paymentId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(entity = InvoiceEntity::class, parentColumns = ["id"], childColumns = ["invoiceId"]),
    ],
    indices = [Index("paymentId"), Index("invoiceId")],
)
data class PaymentAllocationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val paymentId: Long,
    val invoiceId: Long,
    val amount: Int,
)

/** Only absences are stored. Presence is the default - never a row per student per day. */
@Entity(
    tableName = "absences",
    foreignKeys = [
        ForeignKey(
            entity = EnrollmentEntity::class, parentColumns = ["id"], childColumns = ["enrollmentId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["enrollmentId", "date"], unique = true)],
)
data class AbsenceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val enrollmentId: Long,
    val date: LocalDate,
)
