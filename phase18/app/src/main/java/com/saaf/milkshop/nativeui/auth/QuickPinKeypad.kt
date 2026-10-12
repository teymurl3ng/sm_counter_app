package com.saaf.milkshop.nativeui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.saaf.milkshop.nativeui.NativeUiTags
import com.saaf.milkshop.nativeui.common.hapticTap

/** Touch keypad with hardware-key support; deliberately has no editable field or IME. */
@Composable
internal fun QuickPinKeypad(
    count: Int,
    enabled: Boolean,
    onDigit: (Char) -> Unit,
    onClear: () -> Unit,
    onDelete: () -> Unit,
    onSubmit: () -> Unit,
) {
    val focus = remember { FocusRequester() }
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        withFrameNanos { }; focus.requestFocus()
    }
    Column(
        Modifier.fillMaxWidth().testTag(NativeUiTags.AuthQuickPinKeypad)
            .focusRequester(focus)
            .onPreviewKeyEvent { event ->
                val character = event.nativeKeyEvent.unicodeChar.toChar()
                val digit = character in '0'..'9'
                val delete = event.key == Key.Backspace
                val submit = event.key == Key.Enter || event.key == Key.NumPadEnter
                if (!digit && !delete && !submit) false
                else {
                    if (enabled && event.type == KeyEventType.KeyDown) {
                        when {
                            digit && count < 12 -> { context.hapticTap(); onDigit(character) }
                            delete && count > 0 -> { context.hapticTap(); onDelete() }
                            submit && count in 4..12 -> onSubmit()
                        }
                    }
                    true
                }
            }.focusable(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(Modifier.fillMaxWidth().height(40.dp).testTag(NativeUiTags.AuthQuickPinInput)
            .clearAndSetSemantics {
                password()
                contentDescription = "PIN entry"
                stateDescription = "$count digits entered"
            }, contentAlignment = Alignment.Center) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(count) { Box(Modifier.size(10.dp).background(MaterialTheme.colorScheme.onSurface, CircleShape)) }
            }
        }
        for (row in listOf(listOf(1, 2, 3), listOf(4, 5, 6), listOf(7, 8, 9), listOf(-1, 0, -2))) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { digit ->
                    val editing = digit < 0
                    val keyEnabled = enabled && if (editing) count > 0 else count < 12
                    val tag = when (digit) {
                        -1 -> NativeUiTags.AuthQuickPinClear
                        -2 -> NativeUiTags.AuthQuickPinDelete
                        else -> NativeUiTags.authQuickPinDigit(digit)
                    }
                    OutlinedButton(
                        onClick = {
                            context.hapticTap()
                            when (digit) { -1 -> onClear(); -2 -> onDelete(); else -> onDigit('0' + digit) }
                        },
                        enabled = keyEnabled,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                        ),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 12.dp),
                        modifier = Modifier.weight(1f).heightIn(min = 64.dp).testTag(tag),
                    ) {
                        when (digit) {
                            -1 -> Text("Clear", style = MaterialTheme.typography.labelLarge)
                            -2 -> Icon(Icons.AutoMirrored.Outlined.Backspace, "Delete last digit")
                            else -> Text(digit.toString(), style = MaterialTheme.typography.titleLarge.copy(fontSize = 24.sp, lineHeight = 32.sp))
                        }
                    }
                }
            }
        }
    }
}
