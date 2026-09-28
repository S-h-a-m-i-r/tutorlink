package pk.groscale.feeregister.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "profile")

/**
 * The teacher's own profile and settings.
 *
 * There is no User table: one install = one teacher, so there is nobody to
 * distinguish him from. This is DataStore rather than Room because nothing ever
 * JOINs to the profile - it is read for display (receipt header, message
 * signature) and never queried against. See onboarding-and-auth.md.
 */
data class Profile(
    val tuitionName: String = "",
    val teacherName: String = "",
    val teacherPhone: String = "",
    /** The "is logged in" check. Splash routes on this single boolean. */
    val setupComplete: Boolean = false,
    val appLockEnabled: Boolean = false,
    /** Fee due window: 1st to 10th. */
    val graceDays: Int = 10,
    val roundingStep: Int = 50,
    val defaultAnchor: String = "CALENDAR",
    val lastBackupAt: Long = 0L,
    /**
     * Reminders are off until the teacher turns them on. POST_NOTIFICATIONS is
     * asked for at that moment and never at first launch - play-store-policy.md
     * section 1.
     */
    val attendanceReminder: Boolean = false,
    val feeReminder: Boolean = false,
    /** Hour of the morning a fee-day reminder lands. */
    val feeReminderHour: Int = 9,
)

class ProfileStore(private val context: Context) {

    private object Keys {
        val TUITION_NAME = stringPreferencesKey("tuition_name")
        val TEACHER_NAME = stringPreferencesKey("teacher_name")
        val TEACHER_PHONE = stringPreferencesKey("teacher_phone")
        val SETUP_COMPLETE = booleanPreferencesKey("setup_complete")
        val APP_LOCK = booleanPreferencesKey("app_lock")
        val GRACE_DAYS = intPreferencesKey("grace_days")
        val ROUNDING_STEP = intPreferencesKey("rounding_step")
        val DEFAULT_ANCHOR = stringPreferencesKey("default_anchor")
        val LAST_BACKUP_AT = longPreferencesKey("last_backup_at")
        val ATTENDANCE_REMINDER = booleanPreferencesKey("attendance_reminder")
        val FEE_REMINDER = booleanPreferencesKey("fee_reminder")
        val FEE_REMINDER_HOUR = intPreferencesKey("fee_reminder_hour")
    }

    val profile: Flow<Profile> = context.dataStore.data.map { p ->
        val defaults = Profile()
        Profile(
            tuitionName = p[Keys.TUITION_NAME] ?: defaults.tuitionName,
            teacherName = p[Keys.TEACHER_NAME] ?: defaults.teacherName,
            teacherPhone = p[Keys.TEACHER_PHONE] ?: defaults.teacherPhone,
            setupComplete = p[Keys.SETUP_COMPLETE] ?: defaults.setupComplete,
            appLockEnabled = p[Keys.APP_LOCK] ?: defaults.appLockEnabled,
            graceDays = p[Keys.GRACE_DAYS] ?: defaults.graceDays,
            roundingStep = p[Keys.ROUNDING_STEP] ?: defaults.roundingStep,
            defaultAnchor = p[Keys.DEFAULT_ANCHOR] ?: defaults.defaultAnchor,
            lastBackupAt = p[Keys.LAST_BACKUP_AT] ?: defaults.lastBackupAt,
            attendanceReminder = p[Keys.ATTENDANCE_REMINDER] ?: defaults.attendanceReminder,
            feeReminder = p[Keys.FEE_REMINDER] ?: defaults.feeReminder,
            feeReminderHour = p[Keys.FEE_REMINDER_HOUR] ?: defaults.feeReminderHour,
        )
    }

    suspend fun saveTuition(tuitionName: String, teacherName: String, teacherPhone: String) {
        context.dataStore.edit {
            it[Keys.TUITION_NAME] = tuitionName
            it[Keys.TEACHER_NAME] = teacherName
            it[Keys.TEACHER_PHONE] = teacherPhone
        }
    }

    suspend fun setReminders(attendance: Boolean, fee: Boolean) {
        context.dataStore.edit {
            it[Keys.ATTENDANCE_REMINDER] = attendance
            it[Keys.FEE_REMINDER] = fee
        }
    }

    suspend fun markSetupComplete() {
        context.dataStore.edit { it[Keys.SETUP_COMPLETE] = true }
    }

    suspend fun setAppLock(enabled: Boolean) {
        context.dataStore.edit { it[Keys.APP_LOCK] = enabled }
    }

    /** Used by restore: writes the whole profile in one edit. */
    suspend fun restore(profile: Profile) {
        context.dataStore.edit {
            it[Keys.TUITION_NAME] = profile.tuitionName
            it[Keys.TEACHER_NAME] = profile.teacherName
            it[Keys.TEACHER_PHONE] = profile.teacherPhone
            it[Keys.SETUP_COMPLETE] = true
            it[Keys.GRACE_DAYS] = profile.graceDays
            it[Keys.ROUNDING_STEP] = profile.roundingStep
            it[Keys.DEFAULT_ANCHOR] = profile.defaultAnchor
        }
    }

    suspend fun recordBackup(atEpochMillis: Long) {
        context.dataStore.edit { it[Keys.LAST_BACKUP_AT] = atEpochMillis }
    }
}
