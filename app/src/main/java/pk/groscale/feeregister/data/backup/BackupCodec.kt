package pk.groscale.feeregister.data.backup

import org.json.JSONArray
import org.json.JSONObject
import pk.groscale.feeregister.data.db.AbsenceEntity
import pk.groscale.feeregister.data.db.BatchEntity
import pk.groscale.feeregister.data.db.EnrollmentEntity
import pk.groscale.feeregister.data.db.FamilyEntity
import pk.groscale.feeregister.data.db.HolidayEntity
import pk.groscale.feeregister.data.db.InvoiceEntity
import pk.groscale.feeregister.data.db.PaymentAllocationEntity
import pk.groscale.feeregister.data.db.PaymentEntity
import pk.groscale.feeregister.data.db.StudentEntity
import pk.groscale.feeregister.data.prefs.Profile
import java.time.LocalDate

/**
 * The backup file: plain JSON, human-readable, versioned.
 *
 * Mapping is written out by hand rather than generated. That is deliberate: it
 * means adding a column to an entity cannot silently change the file format, and
 * when a teacher's data looks wrong he can send you the file and you can read it.
 *
 * `formatVersion` exists from day one so a future import can migrate rather than
 * fail on a file someone kept for a year.
 */
object BackupCodec {

    const val FORMAT_VERSION = 1

    class IncompatibleBackup(val found: Int) :
        Exception("This backup was made by a newer version of the app (format $found).")

    // --- helpers ----------------------------------------------------------
    private fun JSONObject.putDate(key: String, d: LocalDate?) =
        apply { if (d == null) put(key, JSONObject.NULL) else put(key, d.toString()) }

    private fun JSONObject.date(key: String): LocalDate = LocalDate.parse(getString(key))
    private fun JSONObject.dateOrNull(key: String): LocalDate? =
        if (isNull(key)) null else LocalDate.parse(getString(key))

    private fun JSONObject.strOrNull(key: String): String? = if (isNull(key)) null else getString(key)
    private fun JSONObject.longOrNull(key: String): Long? = if (isNull(key)) null else getLong(key)

    private inline fun <T> JSONArray.map(transform: (JSONObject) -> T): List<T> =
        (0 until length()).map { transform(getJSONObject(it)) }

    private fun <T> array(rows: List<T>, encode: (T) -> JSONObject) =
        JSONArray().also { a -> rows.forEach { a.put(encode(it)) } }

    // --- encode -----------------------------------------------------------

    fun encode(
        profile: Profile,
        batches: List<BatchEntity>,
        families: List<FamilyEntity>,
        students: List<StudentEntity>,
        enrollments: List<EnrollmentEntity>,
        holidays: List<HolidayEntity>,
        invoices: List<InvoiceEntity>,
        payments: List<PaymentEntity>,
        allocations: List<PaymentAllocationEntity>,
        absences: List<AbsenceEntity>,
    ): String = JSONObject().apply {
        put("formatVersion", FORMAT_VERSION)
        put("exportedAt", java.time.Instant.now().toString())
        put(
            "profile",
            JSONObject().apply {
                put("tuitionName", profile.tuitionName)
                put("teacherName", profile.teacherName)
                put("teacherPhone", profile.teacherPhone)
                put("graceDays", profile.graceDays)
                put("roundingStep", profile.roundingStep)
                put("defaultAnchor", profile.defaultAnchor)
            },
        )
        put(
            "batches",
            array(batches) {
                JSONObject().apply {
                    put("id", it.id); put("name", it.name); put("classLabel", it.classLabel)
                    put("daysOfWeekMask", it.daysOfWeekMask)
                    put("startMinute", it.startMinute); put("endMinute", it.endMinute)
                    put("defaultFee", it.defaultFee); put("active", it.active)
                    putDate("createdAt", it.createdAt)
                }
            },
        )
        put(
            "families",
            array(families) {
                JSONObject().apply {
                    put("id", it.id); put("name", it.name)
                    put("guardianName", it.guardianName); put("guardianPhone", it.guardianPhone)
                }
            },
        )
        put(
            "students",
            array(students) {
                JSONObject().apply {
                    put("id", it.id); put("name", it.name); put("classLabel", it.classLabel)
                    put("familyId", it.familyId ?: JSONObject.NULL)
                    put("status", it.status); putDate("joinedAt", it.joinedAt)
                    put("notes", it.notes ?: JSONObject.NULL)
                }
            },
        )
        put(
            "enrollments",
            array(enrollments) {
                JSONObject().apply {
                    put("id", it.id); put("studentId", it.studentId); put("batchId", it.batchId)
                    put("fee", it.fee); putDate("startDate", it.startDate); putDate("endDate", it.endDate)
                    put("billingAnchor", it.billingAnchor); put("mergeStubIntoNext", it.mergeStubIntoNext)
                }
            },
        )
        put(
            "holidays",
            array(holidays) {
                JSONObject().apply {
                    put("id", it.id); putDate("date", it.date)
                    put("batchId", it.batchId ?: JSONObject.NULL)
                    put("reason", it.reason ?: JSONObject.NULL)
                }
            },
        )
        put(
            "invoices",
            array(invoices) {
                JSONObject().apply {
                    put("id", it.id); put("enrollmentId", it.enrollmentId)
                    putDate("periodStart", it.periodStart); putDate("periodEnd", it.periodEnd)
                    putDate("dueDate", it.dueDate)
                    put("feeSnapshot", it.feeSnapshot)
                    put("classDaysInPeriod", it.classDaysInPeriod)
                    put("classDaysEnrolled", it.classDaysEnrolled)
                    put("amountDue", it.amountDue); put("adjustment", it.adjustment)
                    put("adjustmentNote", it.adjustmentNote ?: JSONObject.NULL)
                    put("tuitionNameSnapshot", it.tuitionNameSnapshot)
                    put("teacherPhoneSnapshot", it.teacherPhoneSnapshot ?: JSONObject.NULL)
                    putDate("lastRemindedAt", it.lastRemindedAt)
                }
            },
        )
        put(
            "payments",
            array(payments) {
                JSONObject().apply {
                    put("id", it.id)
                    put("familyId", it.familyId ?: JSONObject.NULL)
                    put("studentId", it.studentId ?: JSONObject.NULL)
                    put("amount", it.amount); putDate("date", it.date)
                    put("note", it.note ?: JSONObject.NULL)
                    put("receiptNo", it.receiptNo); put("unallocated", it.unallocated)
                }
            },
        )
        put(
            "paymentAllocations",
            array(allocations) {
                JSONObject().apply {
                    put("id", it.id); put("paymentId", it.paymentId)
                    put("invoiceId", it.invoiceId); put("amount", it.amount)
                }
            },
        )
        put(
            "absences",
            array(absences) {
                JSONObject().apply {
                    put("id", it.id); put("enrollmentId", it.enrollmentId); putDate("date", it.date)
                }
            },
        )
    }.toString(2)

    // --- decode -----------------------------------------------------------

    data class Decoded(
        val profile: Profile,
        val batches: List<BatchEntity>,
        val families: List<FamilyEntity>,
        val students: List<StudentEntity>,
        val enrollments: List<EnrollmentEntity>,
        val holidays: List<HolidayEntity>,
        val invoices: List<InvoiceEntity>,
        val payments: List<PaymentEntity>,
        val allocations: List<PaymentAllocationEntity>,
        val absences: List<AbsenceEntity>,
    )

    fun decode(text: String): Decoded {
        val root = JSONObject(text)
        val version = root.optInt("formatVersion", 0)
        if (version > FORMAT_VERSION) throw IncompatibleBackup(version)

        val p = root.optJSONObject("profile") ?: JSONObject()
        val defaults = Profile()

        fun arr(name: String) = root.optJSONArray(name) ?: JSONArray()

        return Decoded(
            profile = Profile(
                tuitionName = p.optString("tuitionName", defaults.tuitionName),
                teacherName = p.optString("teacherName", defaults.teacherName),
                teacherPhone = p.optString("teacherPhone", defaults.teacherPhone),
                setupComplete = true,
                graceDays = p.optInt("graceDays", defaults.graceDays),
                roundingStep = p.optInt("roundingStep", defaults.roundingStep),
                defaultAnchor = p.optString("defaultAnchor", defaults.defaultAnchor),
            ),
            batches = arr("batches").map {
                BatchEntity(
                    id = it.getLong("id"), name = it.getString("name"),
                    classLabel = it.optString("classLabel", ""),
                    daysOfWeekMask = it.getInt("daysOfWeekMask"),
                    startMinute = it.getInt("startMinute"), endMinute = it.getInt("endMinute"),
                    defaultFee = it.getInt("defaultFee"), active = it.optBoolean("active", true),
                    createdAt = it.date("createdAt"),
                )
            },
            families = arr("families").map {
                FamilyEntity(
                    id = it.getLong("id"), name = it.optString("name", ""),
                    guardianName = it.optString("guardianName", ""),
                    guardianPhone = it.optString("guardianPhone", ""),
                )
            },
            students = arr("students").map {
                StudentEntity(
                    id = it.getLong("id"), name = it.getString("name"),
                    classLabel = it.optString("classLabel", ""),
                    familyId = it.longOrNull("familyId"),
                    status = it.optString("status", "ACTIVE"),
                    joinedAt = it.date("joinedAt"), notes = it.strOrNull("notes"),
                )
            },
            enrollments = arr("enrollments").map {
                EnrollmentEntity(
                    id = it.getLong("id"), studentId = it.getLong("studentId"),
                    batchId = it.getLong("batchId"), fee = it.getInt("fee"),
                    startDate = it.date("startDate"), endDate = it.dateOrNull("endDate"),
                    billingAnchor = it.optString("billingAnchor", "CALENDAR"),
                    mergeStubIntoNext = it.optBoolean("mergeStubIntoNext", false),
                )
            },
            holidays = arr("holidays").map {
                HolidayEntity(
                    id = it.getLong("id"), date = it.date("date"),
                    batchId = it.longOrNull("batchId"), reason = it.strOrNull("reason"),
                )
            },
            invoices = arr("invoices").map {
                InvoiceEntity(
                    id = it.getLong("id"), enrollmentId = it.getLong("enrollmentId"),
                    periodStart = it.date("periodStart"), periodEnd = it.date("periodEnd"),
                    dueDate = it.date("dueDate"), feeSnapshot = it.getInt("feeSnapshot"),
                    classDaysInPeriod = it.getInt("classDaysInPeriod"),
                    classDaysEnrolled = it.getInt("classDaysEnrolled"),
                    amountDue = it.getInt("amountDue"), adjustment = it.optInt("adjustment", 0),
                    adjustmentNote = it.strOrNull("adjustmentNote"),
                    tuitionNameSnapshot = it.optString("tuitionNameSnapshot", ""),
                    teacherPhoneSnapshot = it.strOrNull("teacherPhoneSnapshot"),
                    lastRemindedAt = it.dateOrNull("lastRemindedAt"),
                )
            },
            payments = arr("payments").map {
                PaymentEntity(
                    id = it.getLong("id"), familyId = it.longOrNull("familyId"),
                    studentId = it.longOrNull("studentId"), amount = it.getInt("amount"),
                    date = it.date("date"), note = it.strOrNull("note"),
                    receiptNo = it.getInt("receiptNo"), unallocated = it.optInt("unallocated", 0),
                )
            },
            allocations = arr("paymentAllocations").map {
                PaymentAllocationEntity(
                    id = it.getLong("id"), paymentId = it.getLong("paymentId"),
                    invoiceId = it.getLong("invoiceId"), amount = it.getInt("amount"),
                )
            },
            absences = arr("absences").map {
                AbsenceEntity(
                    id = it.getLong("id"), enrollmentId = it.getLong("enrollmentId"),
                    date = it.date("date"),
                )
            },
        )
    }
}
