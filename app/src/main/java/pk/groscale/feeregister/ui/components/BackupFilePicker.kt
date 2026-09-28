package pk.groscale.feeregister.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext

/**
 * Picks a backup file through the system picker and reads it off the main thread.
 *
 * Shared by Settings and the welcome screen: the teacher who most needs to
 * restore is the one holding a new phone, and he has no Settings tab to go to
 * yet. Only the file handling is shared - what each screen asks before replacing
 * a register differs, because one of them has a register to lose and the other
 * does not.
 *
 * A backup written by this app is JSON, but teachers move these files through
 * WhatsApp and file managers that rewrite the MIME type, so the picker accepts
 * anything and the decoder is what decides whether the file is really ours.
 */
@Composable
fun rememberBackupFilePicker(onFileRead: (String) -> Unit): () -> Unit {
    val context = LocalContext.current
    val handler = rememberUpdatedState(onFileRead)

    val open = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        // A register from a long-running tuition is not a small file, and this
        // is content-provider IO: never on the main thread.
        Thread {
            val text = runCatching {
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            }.getOrNull()
            if (text != null) handler.value(text)
        }.start()
    }

    return remember { { open.launch(arrayOf("application/json", "text/plain", "*/*")) } }
}
