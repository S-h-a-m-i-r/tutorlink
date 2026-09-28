package pk.groscale.feeregister.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/**
 * Whole-table reads and writes, used only by export and restore.
 *
 * Inserts keep the original primary keys: Room's autoGenerate only fires when the
 * id is 0, so a restore reproduces the exact same graph and every foreign key
 * still points where it did.
 */
@Dao
interface BackupDao {
    @Query("SELECT * FROM batches") suspend fun batches(): List<BatchEntity>
    @Query("SELECT * FROM families") suspend fun families(): List<FamilyEntity>
    @Query("SELECT * FROM students") suspend fun students(): List<StudentEntity>
    @Query("SELECT * FROM enrollments") suspend fun enrollments(): List<EnrollmentEntity>
    @Query("SELECT * FROM holidays") suspend fun holidays(): List<HolidayEntity>
    @Query("SELECT * FROM invoices") suspend fun invoices(): List<InvoiceEntity>
    @Query("SELECT * FROM payments") suspend fun payments(): List<PaymentEntity>
    @Query("SELECT * FROM payment_allocations") suspend fun allocations(): List<PaymentAllocationEntity>
    @Query("SELECT * FROM absences") suspend fun absences(): List<AbsenceEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putBatches(rows: List<BatchEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putFamilies(rows: List<FamilyEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putStudents(rows: List<StudentEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putEnrollments(rows: List<EnrollmentEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putHolidays(rows: List<HolidayEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putInvoices(rows: List<InvoiceEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putPayments(rows: List<PaymentEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putAllocations(rows: List<PaymentAllocationEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putAbsences(rows: List<AbsenceEntity>)

    // Deleted children-first so foreign keys never block the wipe.
    @Query("DELETE FROM payment_allocations") suspend fun wipeAllocations()
    @Query("DELETE FROM payments") suspend fun wipePayments()
    @Query("DELETE FROM absences") suspend fun wipeAbsences()
    @Query("DELETE FROM invoices") suspend fun wipeInvoices()
    @Query("DELETE FROM enrollments") suspend fun wipeEnrollments()
    @Query("DELETE FROM holidays") suspend fun wipeHolidays()
    @Query("DELETE FROM students") suspend fun wipeStudents()
    @Query("DELETE FROM families") suspend fun wipeFamilies()
    @Query("DELETE FROM batches") suspend fun wipeBatches()
}
