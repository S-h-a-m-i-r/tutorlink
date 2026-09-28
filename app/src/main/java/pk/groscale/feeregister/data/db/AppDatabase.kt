package pk.groscale.feeregister.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        BatchEntity::class, FamilyEntity::class, StudentEntity::class,
        EnrollmentEntity::class, HolidayEntity::class, InvoiceEntity::class,
        PaymentEntity::class, PaymentAllocationEntity::class, AbsenceEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun batches(): BatchDao
    abstract fun families(): FamilyDao
    abstract fun students(): StudentDao
    abstract fun enrollments(): EnrollmentDao
    abstract fun holidays(): HolidayDao
    abstract fun invoices(): InvoiceDao
    abstract fun payments(): PaymentDao
    abstract fun absences(): AbsenceDao
    abstract fun reports(): ReportDao
    abstract fun backup(): BackupDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "tutorlink.db",
            )
                .addMigrations(*ALL_MIGRATIONS)
                // No fallbackToDestructiveMigration: see Migrations.kt. Losing a
                // teacher's register silently is worse than failing loudly.
                .build()
                .also { instance = it }
        }
    }
}
