package com.saaf.milkshop.nativeui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.foundation.text.contextmenu.data.TextContextMenuKeys
import androidx.compose.foundation.text.contextmenu.modifier.filterTextContextMenuComponents
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.saaf.milkshop.application.more.NativeMoreSecurityStatus
import com.saaf.milkshop.nativeui.theme.nativeSectionTextStyle
import com.saaf.milkshop.nativeui.theme.nativeSupportingTextStyle
import com.saaf.milkshop.nativeui.theme.nativeValueTextStyle

/** Matches the Quick PIN verifier. Reject an invalid paste instead of silently changing the PIN. */
internal object QuickPinEntryPolicy {
    fun accept(previous: String, candidate: String): String =
        if (candidate.length <= 12 && candidate.all { it in '0'..'9' }) candidate else previous

    fun valid(pin: String): Boolean = pin.length in 4..12 && pin.all { it in '0'..'9' }
    fun valid(pin: CharArray?): Boolean = pin != null && pin.size in 4..12 && pin.all { it in '0'..'9' }
}

internal data class NativePersonalSecurityTags(
    val status: String,
    val passphrase: String,
    val pin: String,
    val savePin: String,
    val enablePhone: String,
    val remove: String,
    val confirmRemove: String,
    val error: String,
)

@Composable
internal fun NativePersonalSecurityPanel(
    identityKey: Long,
    identityName: String,
    security: NativeMoreSecurityStatus?,
    saving: Boolean,
    error: String?,
    confirmRemove: Boolean,
    tags: NativePersonalSecurityTags,
    onRequestRemove: () -> Unit,
    onCancelRemove: () -> Unit,
    onRemove: (CharArray) -> Unit,
    onEnrollPin: (CharArray, CharArray) -> Unit,
    onEnrollPhone: (CharArray, CharArray?) -> Unit,
) {
    var passphrase by remember(identityKey) { mutableStateOf("") }
    var pin by remember(identityKey) { mutableStateOf("") }
    val focus = LocalFocusManager.current
    fun clearSecrets() {
        passphrase = ""
        pin = ""
    }

    if (confirmRemove) {
        AlertDialog(
            onDismissRequest = onCancelRemove,
            title = { Text("Remove Quick unlock?") },
            text = { Text("This clears Quick PIN and phone security for $identityName on this phone.") },
            confirmButton = {
                TextButton(
                    onClick = hapticDestructiveAction {
                        val secret = passphrase.toCharArray()
                        clearSecrets()
                        onRemove(secret)
                    },
                    enabled = !saving && passphrase.isNotBlank(),
                    modifier = Modifier.testTag(tags.confirmRemove),
                ) { Text("Remove Quick unlock") }
            },
            dismissButton = {
                TextButton(onClick = hapticAction(onCancelRemove)) { Text("Keep Quick unlock") }
            },
        )
    }

    // The role host already owns the page title. Keep the credential fields
    // near that fixed header so the taller number keyboard need not pan it.
    if (security == null) {
        NativeColdLoadingPane(label = "Loading security…")
        return
    }
    NativeInformationSection(Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Change Quick unlock", style = nativeSectionTextStyle())
            Text("Confirm with your full passphrase. Quick PINs use 4–12 digits.", style = nativeSupportingTextStyle())
            // Keep normal editing/Paste actions, but omit the empty Auto-fill
            // popup from this phone-local unlock setup (not a login form).
            OutlinedTextField(
                value = passphrase,
                onValueChange = { passphrase = it },
                modifier = Modifier.fillMaxWidth().testTag(tags.passphrase)
                    .filterTextContextMenuComponents { it.key != TextContextMenuKeys.AutofillKey },
                label = { Text("Current full passphrase") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focus.moveFocus(FocusDirection.Down) }),
                enabled = !saving,
            )
            OutlinedTextField(
                value = pin,
                onValueChange = { pin = QuickPinEntryPolicy.accept(pin, it) },
                modifier = Modifier.fillMaxWidth().testTag(tags.pin)
                    .filterTextContextMenuComponents { it.key != TextContextMenuKeys.AutofillKey },
                label = { Text("Quick PIN (4–12 digits)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focus.clearFocus() }),
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                enabled = !saving,
            )
            error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.testTag(tags.error)) }
            Button(
                onClick = hapticAction {
                    if (passphrase.isBlank() || !QuickPinEntryPolicy.valid(pin)) return@hapticAction
                    val secret = passphrase.toCharArray()
                    val quickPin = pin.toCharArray()
                    clearSecrets()
                    onEnrollPin(secret, quickPin)
                },
                enabled = !saving && passphrase.isNotBlank() && QuickPinEntryPolicy.valid(pin),
                modifier = Modifier.fillMaxWidth().testTag(tags.savePin),
            ) { Text(if (saving) "Saving…" else "Save Quick PIN") }
            if (security.phoneSecurityAvailable) {
                Text(
                    if (security.quickPinEnabled) "Enter the PIN to save alongside phone security; this replaces the PIN on this phone."
                    else "You may enter a PIN to save alongside phone security.",
                    style = nativeSupportingTextStyle(),
                )
                Button(
                    onClick = hapticAction {
                        if (passphrase.isBlank() || (security.quickPinEnabled && !QuickPinEntryPolicy.valid(pin)) ||
                            (pin.isNotEmpty() && !QuickPinEntryPolicy.valid(pin))) return@hapticAction
                        val secret = passphrase.toCharArray()
                        val quickPin = pin.takeIf(QuickPinEntryPolicy::valid)?.toCharArray()
                        clearSecrets()
                        onEnrollPhone(secret, quickPin)
                    },
                    enabled = !saving && passphrase.isNotBlank() &&
                        (pin.isEmpty() && !security.quickPinEnabled || QuickPinEntryPolicy.valid(pin)),
                    modifier = Modifier.fillMaxWidth().testTag(tags.enablePhone),
                ) { Text(if (saving) "Saving…" else "Enable ${security.phoneSecurityLabel}") }
            }
            if (security.quickPinEnabled || security.phoneSecurityEnabled) {
                TextButton(
                    onClick = hapticAction(onRequestRemove),
                    enabled = !saving && passphrase.isNotBlank(),
                    modifier = Modifier.testTag(tags.remove),
                ) { Text("Remove Quick unlock") }
            }
        }
    }
    NativeInformationSection(Modifier.fillMaxWidth().testTag(tags.status)) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("On this phone", style = nativeSectionTextStyle())
            Text("Quick PIN: ${if (security.quickPinEnabled) "On" else "Off"}", style = nativeValueTextStyle())
            Text(
                "${security.phoneSecurityLabel}: ${when {
                    !security.phoneSecurityAvailable -> "Unavailable on this phone"
                    security.phoneSecurityEnabled -> "On"
                    else -> "Off"
                }}",
                style = nativeValueTextStyle(),
            )
        }
    }
}
