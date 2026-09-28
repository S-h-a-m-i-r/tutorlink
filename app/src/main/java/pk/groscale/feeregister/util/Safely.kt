package pk.groscale.feeregister.util

import android.util.Log
import kotlinx.coroutines.CancellationException

private const val TAG = "TutorLink"

/**
 * Every database write goes through this.
 *
 * The app holds a teacher's fee records and is the only copy of them. A crash in
 * the middle of a write is far worse than a message saying it did not work, so
 * nothing is allowed to reach the default handler.
 *
 * CancellationException is rethrown - swallowing it breaks structured concurrency
 * and leaves coroutines running after their scope is gone.
 */
suspend fun <T> safely(what: String, block: suspend () -> T): Result<T> =
    try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        Log.e(TAG, "Failed: $what", e)
        Result.failure(e)
    }

/** What the teacher sees. Never a stack trace, never "null". */
fun Throwable.userMessage(fallback: String): String = when (this) {
    is android.database.sqlite.SQLiteConstraintException -> "That already exists."
    is android.database.sqlite.SQLiteFullException -> "The phone is out of storage."
    is java.io.IOException -> "Could not read or write the file."
    else -> fallback
}
