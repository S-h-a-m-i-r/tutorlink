package pk.groscale.feeregister.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import pk.groscale.feeregister.ui.theme.Ink100
import pk.groscale.feeregister.ui.theme.Ink400
import pk.groscale.feeregister.ui.theme.Radius
import pk.groscale.feeregister.ui.theme.Space
import pk.groscale.feeregister.ui.theme.TapTarget

/**
 * Label ABOVE the field, never floating. A floating label that flies away the
 * moment you type is the wrong choice for someone who is not confident on a
 * phone - he needs to still see what the box is for while he fills it.
 */
@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    hint: String? = null,
    placeholder: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    focusRequester: FocusRequester? = null,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Space.sm)) {
        if (label.isNotBlank()) {
            Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
        }
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier),
            // Ink400, the palette's placeholder-only grey. In caption grey, examples
            // like "Ahmed Raza" read as data already entered, and teachers skipped
            // the field.
            placeholder = placeholder?.let {
                { Text(it, style = MaterialTheme.typography.bodyLarge, color = Ink400) }
            },
            textStyle = MaterialTheme.typography.bodyLarge,
            singleLine = true,
            shape = RoundedCornerShape(Radius.input),
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                keyboardType = keyboardType,
                imeAction = imeAction,
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Ink100,
                unfocusedContainerColor = Ink100,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
            ),
        )
        if (hint != null) {
            Text(hint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val guarded = rememberThrottled(action = onClick)
    val interaction = remember { MutableInteractionSource() }
    Button(
        onClick = guarded,
        enabled = enabled,
        interactionSource = interaction,
        modifier = modifier.fillMaxWidth().height(TapTarget).pressScale(interaction),
        shape = RoundedCornerShape(Radius.pill),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
    ) { Text(text, style = MaterialTheme.typography.labelLarge) }
}

@Composable
fun SecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val guarded = rememberThrottled(action = onClick)
    val interaction = remember { MutableInteractionSource() }
    OutlinedButton(
        onClick = guarded,
        interactionSource = interaction,
        modifier = modifier.fillMaxWidth().height(TapTarget).pressScale(interaction),
        shape = RoundedCornerShape(Radius.pill),
    ) { Text(text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary) }
}
