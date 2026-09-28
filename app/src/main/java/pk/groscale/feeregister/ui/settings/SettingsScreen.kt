package pk.groscale.feeregister.ui.settings

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Switch
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.os.Build
import pk.groscale.feeregister.notify.ReminderNotifications
import pk.groscale.feeregister.ui.components.AppTextField
import pk.groscale.feeregister.ui.components.PrimaryButton
import pk.groscale.feeregister.ui.components.rememberBackupFilePicker
import pk.groscale.feeregister.ui.components.SecondaryButton
import pk.groscale.feeregister.ui.theme.Ink200
import pk.groscale.feeregister.ui.theme.Space
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun SettingsScreen(vm: SettingsViewModel) {
    val context = LocalContext.current
    val profile by vm.profile.collectAsState()
    val busy by vm.busy.collectAsState()
    val students by vm.studentCount.collectAsState()
    val snackbar = remember { SnackbarHostState() }

    var tuition by remember(profile.tuitionName) { mutableStateOf(profile.tuitionName) }
    var teacher by remember(profile.teacherName) { mutableStateOf(profile.teacherName) }
    var phone by remember(profile.teacherPhone) { mutableStateOf(profile.teacherPhone) }

    var pendingExport by remember { mutableStateOf<String?>(null) }
    var confirmRestore by remember { mutableStateOf<String?>(null) }

    val createDoc = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        val text = pendingExport
        pendingExport = null
        if (uri == null || text == null) return@rememberLauncherForActivityResult
        // Writing happens off the main thread; a big register on a slow phone
        // would otherwise stutter here.
        vm.recordBackupDone()
        Thread {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use { it.write(text.toByteArray()) }
            }
        }.start()
    }

    val pickBackup = rememberBackupFilePicker { text -> confirmRestore = text }

    // Which switch the teacher just tapped, held while Android asks him about
    // notifications so the answer can be applied to the right one.
    var awaitingPermissionFor by remember { mutableStateOf<String?>(null) }

    val askNotifications = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        val which = awaitingPermissionFor
        awaitingPermissionFor = null
        if (granted) {
            vm.setReminders(
                context,
                attendance = profile.attendanceReminder || which == "attendance",
                fee = profile.feeReminder || which == "fee",
            )
        } else {
            // Leave both switches as they were. Turning one on while Android
            // drops every notification would be a switch that lies.
            vm.notifyPermissionRefused()
        }
    }

    /** Turns a reminder on, asking Android first if it has not been asked yet. */
    fun setReminder(which: String, on: Boolean) {
        val attendance = if (which == "attendance") on else profile.attendanceReminder
        val fee = if (which == "fee") on else profile.feeReminder
        val needsAsking = on &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !ReminderNotifications.allowed(context)
        if (needsAsking) {
            awaitingPermissionFor = which
            askNotifications.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        } else {
            vm.setReminders(context, attendance, fee)
        }
    }

    LaunchedEffect(vm) {
        vm.events.collect { event ->
            when (event) {
                is SettingsEvent.Message -> snackbar.showSnackbar(event.text)
                is SettingsEvent.WriteBackup -> {
                    pendingExport = event.text
                    createDoc.launch(defaultBackupName())
                }
            }
        }
    }

    confirmRestore?.let { text ->
        AlertDialog(
            onDismissRequest = { confirmRestore = null },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Replace everything?", style = MaterialTheme.typography.titleMedium) },
            text = {
                Text(
                    "Restoring replaces all $students students, their fees and every payment on " +
                        "this phone with what is in the file. This cannot be undone.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            confirmButton = {
                TextButton(
                    enabled = !busy,
                    onClick = { confirmRestore = null; vm.restore(text) },
                ) { Text("Replace") }
            },
            dismissButton = { TextButton(onClick = { confirmRestore = null }) { Text("Cancel") } },
        )
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .imePadding()
                .padding(horizontal = Space.screen),
            verticalArrangement = Arrangement.spacedBy(Space.xl),
        ) {
            Spacer(Modifier.height(Space.lg))
            Text("Settings", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)

            SectionTitle("Your tuition")
            Text(
                "This is what parents see at the top of every receipt.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            AppTextField(value = tuition, onValueChange = { tuition = it }, label = "Tuition name")
            AppTextField(value = teacher, onValueChange = { teacher = it }, label = "Your name")
            AppTextField(
                value = phone,
                onValueChange = { new -> phone = new.filter { it.isDigit() || it == '+' }.take(15) },
                label = "Your WhatsApp number",
                keyboardType = KeyboardType.Phone,
            )
            PrimaryButton(
                text = "Save",
                enabled = !busy,
                onClick = { vm.saveTuition(tuition, teacher, phone) },
            )

            HorizontalDivider(color = Ink200)

            SectionTitle("Reminders")
            Text(
                buildString {
                    append("Both are off until you turn them on, and nothing is ever sent to a ")
                    append("parent by itself — a reminder only opens the app.")
                },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            SwitchRow(
                title = "When a batch starts",
                detail = "A reminder to mark who did not come, on the days that batch meets.",
                checked = profile.attendanceReminder,
                onChange = { setReminder("attendance", it) },
            )
            SwitchRow(
                title = "On fee day",
                detail = "The morning a student's fee falls due.",
                checked = profile.feeReminder,
                onChange = { setReminder("fee", it) },
            )

            HorizontalDivider(color = Ink200)

            SectionTitle("Backup")
            Text(
                buildString {
                    append("Your register lives only on this phone. ")
                    append("Save a backup file and send it to your own WhatsApp — then a lost or ")
                    append("broken phone can never take your record with it.")
                },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                lastBackupLine(profile.lastBackupAt),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            PrimaryButton(text = "Save a backup", enabled = !busy, onClick = { vm.prepareExport() })
            SecondaryButton(
                text = "Restore from a backup",
                onClick = pickBackup,
            )

            Spacer(Modifier.height(Space.xxl))
        }

        SnackbarHost(
            hostState = snackbar,
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(Space.lg),
        )
    }
}

/** Label and consequence on the left, switch on the right. */
@Composable
private fun SwitchRow(
    title: String,
    detail: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.lg),
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Space.xs)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(
                detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
}

private fun defaultBackupName(): String {
    val stamp = DateTimeFormatter.ofPattern("yyyy-MM-dd").format(java.time.LocalDate.now())
    return "tutorlink-backup-$stamp.json"
}

private fun lastBackupLine(atMillis: Long): String =
    if (atMillis <= 0L) {
        "You have not saved a backup yet."
    } else {
        val d = Instant.ofEpochMilli(atMillis).atZone(ZoneId.systemDefault()).toLocalDate()
        "Last backup: ${DateTimeFormatter.ofPattern("d MMM yyyy").format(d)}"
    }
