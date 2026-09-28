package pk.groscale.feeregister.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Every schema change needs a migration here, and the version in [AppDatabase]
 * bumped to match.
 *
 * There is deliberately NO `fallbackToDestructiveMigration`. A missing migration
 * would then silently delete a teacher's entire fee history on update, which is
 * far worse than a crash: he would open the app to an empty register and have no
 * idea why. Crashing on a missing migration keeps the mistake ours, in testing,
 * instead of his, in production.
 *
 * Test every migration by installing the previous build, entering data, then
 * installing the new one over it.
 */

/** v2: track when a parent was last reminded, so reminders can be sequential. */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Dates are stored as epoch day, so INTEGER and nullable.
        db.execSQL("ALTER TABLE invoices ADD COLUMN lastRemindedAt INTEGER DEFAULT NULL")
    }
}

val ALL_MIGRATIONS = arrayOf(MIGRATION_1_2)
