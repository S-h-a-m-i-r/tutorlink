package pk.groscale.feeregister.data.repo

import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import pk.groscale.feeregister.data.backup.BackupCodec
import pk.groscale.feeregister.data.db.AppDatabase
import pk.groscale.feeregister.data.prefs.ProfileStore

class BackupRepository(
    private val db: AppDatabase,
    private val profileStore: ProfileStore,
) {

    suspend fun export(): String = withContext(Dispatchers.IO) {
        val d = db.backup()
        BackupCodec.encode(
            profile = profileStore.profile.first(),
            batches = d.batches(),
            families = d.families(),
            students = d.students(),
            enrollments = d.enrollments(),
            holidays = d.holidays(),
            invoices = d.invoices(),
            payments = d.payments(),
            allocations = d.allocations(),
            absences = d.absences(),
        )
    }

    /**
     * Replace-all, in one transaction.
     *
     * Restore is never a merge. Merging two registers without a shared identity
     * would mean guessing which of two rows is the real one - and guessing with
     * someone's fee records is not acceptable. Either the whole file lands or
     * nothing changes.
     *
     * Returns how many students were restored, so the caller can say something
     * concrete rather than "done".
     */
    suspend fun restore(text: String): Int = withContext(Dispatchers.IO) {
        val decoded = BackupCodec.decode(text)

        db.withTransaction {
            val d = db.backup()
            // Children first: foreign keys are enforced, so order matters.
            d.wipeAllocations(); d.wipePayments(); d.wipeAbsences()
            d.wipeInvoices(); d.wipeEnrollments(); d.wipeHolidays()
            d.wipeStudents(); d.wipeFamilies(); d.wipeBatches()

            // Parents first on the way back in, for the same reason.
            d.putBatches(decoded.batches)
            d.putFamilies(decoded.families)
            d.putStudents(decoded.students)
            d.putEnrollments(decoded.enrollments)
            d.putHolidays(decoded.holidays)
            d.putInvoices(decoded.invoices)
            d.putPayments(decoded.payments)
            d.putAllocations(decoded.allocations)
            d.putAbsences(decoded.absences)
        }

        profileStore.restore(decoded.profile)
        decoded.students.count { it.status == "ACTIVE" }
    }
}
