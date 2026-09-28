package pk.groscale.feeregister.ui.setup

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import pk.groscale.feeregister.domain.formatRs
import pk.groscale.feeregister.ui.theme.Money
import pk.groscale.feeregister.ui.theme.Radius
import pk.groscale.feeregister.ui.components.Motion
import pk.groscale.feeregister.ui.components.Reveal
import pk.groscale.feeregister.ui.components.pressScale
import pk.groscale.feeregister.ui.components.throttledClickable
import pk.groscale.feeregister.ui.theme.Space
import pk.groscale.feeregister.ui.theme.TapTarget
import pk.groscale.feeregister.ui.theme.Teal100
import pk.groscale.feeregister.ui.theme.Teal600
import pk.groscale.feeregister.ui.theme.Teal700
import pk.groscale.feeregister.ui.theme.TutorLinkTheme

/**
 * "Get started" / "Restore from backup" sits in exactly the slot of
 * "Sign up" / "Log in" - but there is no backend, and the secondary action is the
 * genuinely useful one: the new-phone case.
 *
 * The visual is the product's own hero moment, not an illustration. Fintech
 * onboarding sells by showing the number.
 */
@Composable
fun WelcomeScreen(onGetStarted: () -> Unit, onRestore: () -> Unit) {
    // One entrance, once: the card is set down on the desk, then the words
    // arrive. Everything is in place in well under a second.
    val card = remember { Animatable(0f) }
    val words = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        Reveal.await()
        card.animateTo(1f, tween(Motion.LONG, easing = Motion.Easing))
    }
    LaunchedEffect(Unit) {
        Reveal.await()
        words.animateTo(1f, tween(Motion.MEDIUM, delayMillis = 250, easing = Motion.Easing))
    }
    val getStarted = remember { MutableInteractionSource() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Teal600)
            .safeDrawingPadding()
            .padding(horizontal = Space.xl, vertical = Space.xl),
    ) {
        Spacer(Modifier.weight(0.6f))

        // The card is deliberately off-axis: it reads as a thing lying on a desk
        // rather than a UI element, which is what makes it feel like a product
        // shot instead of a screenshot.
        Column(
            modifier = Modifier
                .fillMaxWidth(0.86f)
                .graphicsLayer {
                    alpha = card.value
                    translationY = (1f - card.value) * 24.dp.toPx()
                    rotationZ = -2.5f - (1f - card.value) * 3f
                }
                .background(Color.White, RoundedCornerShape(Radius.card))
                .padding(Space.screen),
            verticalArrangement = Arrangement.spacedBy(Space.xs),
        ) {
            Text("Pending", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(formatRs(14000), style = Money.large, color = Teal700)
            Text("from 4 students", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Spacer(Modifier.height(Space.xxxl))

        Column(
            modifier = Modifier.graphicsLayer {
                alpha = words.value
                translationY = (1f - words.value) * 12.dp.toPx()
            },
        ) {
            Text(
                "Know who has paid.\nAlways.",
                style = MaterialTheme.typography.headlineLarge,
                color = Color.White,
            )
            Spacer(Modifier.height(Space.md))
            Text(
                "Your tuition fee register — on your phone.",
                style = MaterialTheme.typography.bodyLarge,
                color = Teal100,
            )
        }

        Spacer(Modifier.weight(1f))

        Button(
            onClick = onGetStarted,
            interactionSource = getStarted,
            modifier = Modifier.fillMaxWidth().height(TapTarget).pressScale(getStarted),
            shape = RoundedCornerShape(Radius.pill),
            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Teal700),
        ) { Text("Get started", style = MaterialTheme.typography.labelLarge) }

        Spacer(Modifier.height(Space.lg))
        // A real tap target, not a caption that looks like one: this is the only
        // way back in for a teacher holding a new phone, and he has no Settings
        // tab yet. Padded out to a full-width row so it can actually be hit.
        Text(
            "Already using this? Restore from backup",
            style = MaterialTheme.typography.labelLarge,
            color = Teal100,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .throttledClickable(onClick = onRestore)
                .padding(vertical = Space.lg),
        )
    }
}

@Preview(showBackground = true, heightDp = 780)
@Composable
private fun WelcomePreview() {
    TutorLinkTheme { WelcomeScreen({}, {}) }
}
