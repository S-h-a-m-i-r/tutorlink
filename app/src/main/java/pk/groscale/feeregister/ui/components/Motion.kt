package pk.groscale.feeregister.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

/**
 * The app's whole motion vocabulary. Three durations and one easing, so nothing
 * moves at a speed of its own.
 *
 * Motion here only ever confirms something the teacher did or shows a number
 * changing. Nothing loops, nothing bounces for attention, and every duration is
 * scaled by Android's animator setting, so "Remove animations" turns it all off.
 */
object Motion {
    const val SHORT = 220
    const val MEDIUM = 450
    const val LONG = 700
    val Easing = FastOutSlowInEasing
}

/**
 * Opens once the splash starts to fade, so entrance motion (the welcome card,
 * the collection bar's first fill) plays where it can be seen rather than
 * underneath the splash.
 *
 * A launch without a splash never opens it, hence the timeout in [await]: at
 * worst the entrance starts a moment late, never not at all.
 */
object Reveal {
    private val open = MutableStateFlow(false)

    fun markShown() {
        open.value = true
    }

    suspend fun await(timeoutMs: Long = 700L) {
        withTimeoutOrNull(timeoutMs) { open.first { it } }
    }
}

/**
 * Presses in slightly while a finger is down and springs back on release: a
 * tap that feels like a tap on a cheap phone where the ripple is easy to miss.
 */
fun Modifier.pressScale(interaction: InteractionSource): Modifier = composed {
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "pressScale",
    )
    graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/**
 * A tick that draws itself once, on a faint disc. Used where money has just
 * landed, so "saved" is something he sees happen rather than reads.
 */
@Composable
fun DrawnTick(color: Color, modifier: Modifier = Modifier) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(1f, tween(Motion.MEDIUM, delayMillis = 120, easing = Motion.Easing))
    }
    val measure = remember { PathMeasure() }
    val partial = remember { Path() }
    Canvas(modifier.size(44.dp)) {
        val w = size.width
        val h = size.height
        drawCircle(color.copy(alpha = 0.14f))
        val tick = Path().apply {
            moveTo(w * 0.28f, h * 0.53f)
            lineTo(w * 0.44f, h * 0.68f)
            lineTo(w * 0.73f, h * 0.36f)
        }
        measure.setPath(tick, forceClosed = false)
        partial.reset()
        measure.getSegment(0f, measure.length * progress.value, partial, startWithMoveTo = true)
        drawPath(
            partial,
            color,
            style = Stroke(width = w * 0.09f, cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
    }
}
