package pk.groscale.feeregister

import android.app.Application
import android.os.StrictMode
import pk.groscale.feeregister.data.db.AppDatabase
import pk.groscale.feeregister.data.prefs.ProfileStore

class TutorLinkApp : Application() {
    val db by lazy { AppDatabase.get(this) }
    val profileStore by lazy { ProfileStore(this) }

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            // Surfaces main-thread disk and network work during development, which
            // is the usual cause of a jank or an ANR on a cheap phone. Log only -
            // it must never crash a build a teacher is holding.
            StrictMode.setThreadPolicy(
                StrictMode.ThreadPolicy.Builder()
                    .detectDiskReads()
                    .detectDiskWrites()
                    .detectNetwork()
                    .detectCustomSlowCalls()
                    .penaltyLog()
                    .build(),
            )
            StrictMode.setVmPolicy(
                StrictMode.VmPolicy.Builder()
                    .detectLeakedClosableObjects()
                    .detectLeakedSqlLiteObjects()
                    .penaltyLog()
                    .build(),
            )
        }
    }
}
