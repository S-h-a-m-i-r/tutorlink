package pk.groscale.feeregister.ui.setup

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import pk.groscale.feeregister.ui.components.AppTextField
import pk.groscale.feeregister.ui.components.PrimaryButton
import pk.groscale.feeregister.ui.theme.Space

private data class Question(
    val label: String,
    val hint: String?,
    val placeholder: String,
    val keyboard: KeyboardType = KeyboardType.Text,
)

private val questions = listOf(
    Question(
        "What is your tuition called?",
        "This appears at the top of every receipt parents get.",
        "Al-Noor Tuition",
    ),
    Question("What is your name?", null, "Umar Farooq"),
    Question(
        "Your WhatsApp number?",
        "Only shown on receipts. Nothing is sent anywhere.",
        "03001234567",
        KeyboardType.Phone,
    ),
)

/**
 * One question per screen, with answered questions fading upward above the
 * active one.
 *
 * For a teacher who is not confident on a phone this is much less intimidating
 * than a form with four fields at once, and the trail of ticked answers is
 * visible proof he is getting somewhere.
 */
@Composable
fun SetupScreen(onDone: (tuitionName: String, teacherName: String, phone: String) -> Unit) {
    var step by remember { mutableIntStateOf(0) }
    val answers = remember { mutableStateListOf3() }
    var current by remember { mutableStateOf("") }
    val focus = remember { FocusRequester() }

    LaunchedEffect(step) { runCatching { focus.requestFocus() } }

    val q = questions[step]

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .safeDrawingPadding()
            .imePadding()
            .padding(horizontal = Space.xl),
        verticalArrangement = Arrangement.spacedBy(Space.lg),
    ) {
        Spacer(Modifier.height(Space.xxl))

        // The trail of answers already given.
        for (i in 0 until step) {
            Text(
                "✓  ${answers[i]}",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Text(
            q.label,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = Space.md),
        )

        AppTextField(
            value = current,
            onValueChange = { current = it },
            label = "",
            hint = q.hint,
            placeholder = q.placeholder,
            keyboardType = q.keyboard,
            imeAction = if (step == questions.lastIndex) ImeAction.Done else ImeAction.Next,
            focusRequester = focus,
        )

        Spacer(Modifier.weight(1f))

        PrimaryButton(
            text = if (step == questions.lastIndex) "Finish" else "Continue",
            enabled = current.isNotBlank() || step == 2,
            onClick = {
                answers[step] = current.trim()
                if (step == questions.lastIndex) {
                    onDone(answers[0], answers[1], answers[2])
                } else {
                    step++
                    current = ""
                }
            },
        )
        Spacer(Modifier.height(Space.xl))
    }
}

/** Three fixed slots - simpler and clearer here than a growing list. */
private fun mutableStateListOf3() = androidx.compose.runtime.mutableStateListOf("", "", "")
