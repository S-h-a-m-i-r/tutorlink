package pk.groscale.feeregister.ui.setup

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import pk.groscale.feeregister.notify.ReminderNotifications
import pk.groscale.feeregister.ui.components.PrimaryButton
import pk.groscale.feeregister.ui.components.SecondaryButton
import pk.groscale.feeregister.ui.theme.Space

/**
 * The last step of setup: offering the two reminders, once, in the one moment
 * the teacher can see what they are for.
 *
 * Asked here rather than at first launch because at first launch the app is a
 * stranger asking for something. By now he has typed his tuition's name, his
 * batch and his first student, so "when Evening Batch starts, a reminder to mark
 * who did not come" describes something he recognises. That is also what
 * play-store-policy.md section 1 requires - the permission is requested at the
 * moment he turns reminders on, and turning them on is his tap.
 *
 * Buried in Settings this would be found by almost nobody, and a fee register is
 * exactly the kind of app that gets forgotten between months.
 */
@Composable
fun RemindersScreen(
    batchName: String,
    onDone: (attendance: Boolean, fee: Boolean) -> Unit,
) {
    val context = LocalContext.current

    // Whatever Android answers, setup finishes. A refusal is a "not now", not a
    // dead end - both reminders can be switched on later from Settings.
    val ask = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> onDone(granted, granted) }

    fun turnOn() {
        val mustAsk = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !ReminderNotifications.allowed(context)
        if (mustAsk) ask.launch(Manifest.permission.POST_NOTIFICATIONS) else onDone(true, true)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Space.xl),
        verticalArrangement = Arrangement.spacedBy(Space.lg),
    ) {
        Spacer(Modifier.height(Space.xxl))
        Text(
            "Two reminders, so you do not have to remember",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            "Your register is on this phone only. A reminder is the app tapping you " +
                "on the shoulder — it never messages a parent by itself.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(Space.sm))
        Point(
            title = if (batchName.isBlank()) "When your batch starts" else "When $batchName starts",
            detail = "A reminder to mark who did not come, and what is still pending.",
        )
        Point(
            title = "On fee day",
            detail = "The morning a student's fee falls due, so nobody is chased late.",
        )

        Spacer(Modifier.height(Space.lg))
        PrimaryButton(text = "Turn on reminders", onClick = { turnOn() })
        SecondaryButton(text = "Not now", onClick = { onDone(false, false) })
        Text(
            "You can change this any time in Settings.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(Space.xxl))
    }
}

@Composable
private fun Point(title: String, detail: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Space.md),
    ) {
        Text("•", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
        Column(verticalArrangement = Arrangement.spacedBy(Space.xs)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(
                detail,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
