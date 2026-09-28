package pk.groscale.feeregister.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pk.groscale.feeregister.data.backup.BackupCodec
import pk.groscale.feeregister.data.prefs.Profile
import pk.groscale.feeregister.data.repo.BackupRepository
import pk.groscale.feeregister.data.repo.FeeRepository
import pk.groscale.feeregister.notify.ReminderSync
import pk.groscale.feeregister.util.safely
import pk.groscale.feeregister.util.userMessage

sealed interface SettingsEvent {
    data class Message(val text: String) : SettingsEvent
    data class WriteBackup(val text: String) : SettingsEvent
}

class SettingsViewModel(
    private val repo: FeeRepository,
    private val backups: BackupRepository,
) : ViewModel() {

    val profile: StateFlow<Profile> =
        repo.profile.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Profile())

    val studentCount: StateFlow<Int> = repo.observeStudentCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy

    private val _events = MutableSharedFlow<SettingsEvent>(
        replay = 0, extraBufferCapacity = 4, onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val events: SharedFlow<SettingsEvent> = _events

    fun saveTuition(tuitionName: String, teacherName: String, phone: String) {
        if (_busy.value) return
        if (tuitionName.isBlank()) {
            _events.tryEmit(SettingsEvent.Message("Your tuition needs a name — it goes on every receipt."))
            return
        }
        _busy.value = true
        viewModelScope.launch {
            safely("save tuition") { repo.saveTuition(tuitionName, teacherName, phone) }
                .onSuccess { _events.tryEmit(SettingsEvent.Message("Saved.")) }
                .onFailure { _events.tryEmit(SettingsEvent.Message(it.userMessage("Could not save that."))) }
            _busy.value = false
        }
    }

    /**
     * Turns reminders on or off and rebooks the alarms to match.
     *
     * The caller has already dealt with POST_NOTIFICATIONS, so a toggle only ever
     * arrives here as true once the teacher has actually allowed notifications -
     * a switch that looks on while Android silently drops every notification is
     * worse than one that stayed off.
     */
    fun setReminders(context: Context, attendance: Boolean, fee: Boolean) {
        viewModelScope.launch {
            safely("save reminders") { repo.setReminders(attendance, fee) }
                .onFailure { _events.tryEmit(SettingsEvent.Message(it.userMessage("Could not save that."))) }
            safely("schedule reminders") { ReminderSync.rescheduleAll(context) }
        }
    }

    fun notifyPermissionRefused() {
        _events.tryEmit(
            SettingsEvent.Message(
                "Reminders need notification permission. You can allow it in Android settings.",
            ),
        )
    }

    /** Builds the file contents, then hands them to the UI to write via the system picker. */
    fun prepareExport() {
        if (_busy.value) return
        _busy.value = true
        viewModelScope.launch {
            safely("export backup") { backups.export() }
                .onSuccess { _events.tryEmit(SettingsEvent.WriteBackup(it)) }
                .onFailure { _events.tryEmit(SettingsEvent.Message(it.userMessage("Could not build the backup."))) }
            _busy.value = false
        }
    }

    fun recordBackupDone() {
        viewModelScope.launch {
            safely("record backup") { repo.recordBackup() }
            _events.tryEmit(SettingsEvent.Message("Backup saved. Send it to your own WhatsApp so you can always find it."))
        }
    }

    fun restore(text: String) {
        if (_busy.value) return
        _busy.value = true
        viewModelScope.launch {
            safely("restore backup") { backups.restore(text) }
                .onSuccess { count ->
                    _events.tryEmit(SettingsEvent.Message("Restored $count students."))
                }
                .onFailure { e ->
                    val msg = when (e) {
                        is BackupCodec.IncompatibleBackup ->
                            "That backup was made by a newer version of the app. Update first."
                        is org.json.JSONException ->
                            "That file is not an Inkpot backup."
                        else -> e.userMessage("Could not restore that file. Nothing was changed.")
                    }
                    _events.tryEmit(SettingsEvent.Message(msg))
                }
            _busy.value = false
        }
    }
}
