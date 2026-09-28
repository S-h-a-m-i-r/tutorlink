package pk.groscale.feeregister.ui.setup

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import pk.groscale.feeregister.domain.MON_TO_SAT
import pk.groscale.feeregister.ui.components.AppTextField
import pk.groscale.feeregister.ui.components.DayOfWeekPicker
import pk.groscale.feeregister.ui.components.PrimaryButton
import pk.groscale.feeregister.ui.components.StartTimePicker
import pk.groscale.feeregister.ui.theme.Space

data class BatchDraft(
    val name: String,
    val classLabel: String,
    val daysOfWeekMask: Int,
    val startMinute: Int,
    val defaultFee: Int,
)

@Composable
fun BatchFormScreen(
    title: String,
    subtitle: String?,
    ctaText: String,
    onSave: (BatchDraft) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var classLabel by remember { mutableStateOf("") }
    var mask by remember { mutableIntStateOf(MON_TO_SAT) }
    var startMinute by remember { mutableIntStateOf(16 * 60) }
    var fee by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .safeDrawingPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Space.xl),
        verticalArrangement = Arrangement.spacedBy(Space.xl),
    ) {
        Spacer(Modifier.height(Space.lg))
        Column(verticalArrangement = Arrangement.spacedBy(Space.sm)) {
            Text(title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        AppTextField(
            value = name,
            onValueChange = { name = it },
            label = "Batch name",
            placeholder = "Evening Batch",
        )
        AppTextField(
            value = classLabel,
            onValueChange = { classLabel = it },
            label = "Class",
            placeholder = "Class 9",
        )
        DayOfWeekPicker(mask = mask, onChange = { mask = it })
        StartTimePicker(startMinute = startMinute, onChange = { startMinute = it })
        AppTextField(
            value = fee,
            onValueChange = { new -> fee = new.filter { it.isDigit() }.take(6) },
            label = "Usual fee",
            hint = "Just a starting point — you can set a different fee for each student.",
            placeholder = "2500",
            keyboardType = KeyboardType.Number,
        )

        PrimaryButton(
            text = ctaText,
            enabled = name.isNotBlank() && mask != 0 && fee.isNotBlank(),
            onClick = {
                onSave(
                    BatchDraft(
                        name = name,
                        classLabel = classLabel,
                        daysOfWeekMask = mask,
                        startMinute = startMinute,
                        defaultFee = fee.toIntOrNull() ?: 0,
                    ),
                )
            },
        )
        Spacer(Modifier.height(Space.xl))
    }
}
