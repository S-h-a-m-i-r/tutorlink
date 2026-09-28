package pk.groscale.feeregister.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed

/**
 * Ignores repeat taps inside [windowMs].
 *
 * Every destructive or money-moving action in this app is one tap, and a fast
 * double tap would otherwise mean two payments, two students, or two screens
 * pushed onto the back stack. Rather than remembering to guard each call site,
 * the guard lives in the components everything uses.
 */
private const val DEFAULT_WINDOW_MS = 700L

class ThrottledAction(private val windowMs: Long, private val action: () -> Unit) {
    private var lastAt = 0L
    operator fun invoke() {
        val now = System.currentTimeMillis()
        if (now - lastAt < windowMs) return
        lastAt = now
        action()
    }
}

@Composable
fun rememberThrottled(windowMs: Long = DEFAULT_WINDOW_MS, action: () -> Unit): () -> Unit {
    // `action` is re-created every recomposition, so hold it in a box the
    // throttle reads from rather than capturing a stale lambda.
    val holder = remember { arrayOfNulls<() -> Unit>(1) }
    holder[0] = action
    val throttled = remember(windowMs) { ThrottledAction(windowMs) { holder[0]?.invoke() } }
    return throttled::invoke
}

fun Modifier.throttledClickable(windowMs: Long = DEFAULT_WINDOW_MS, onClick: () -> Unit): Modifier =
    composed {
        val guarded = rememberThrottled(windowMs, onClick)
        this.clickable(onClick = guarded)
    }
