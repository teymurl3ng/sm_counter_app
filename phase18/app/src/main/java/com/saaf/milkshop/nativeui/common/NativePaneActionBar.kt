package com.saaf.milkshop.nativeui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/** A section's primary action never changes the tab/content baseline. */
@Composable
internal fun NativePaneActionBar(
    label: String,
    onClick: () -> Unit,
    actionTag: String?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val style = MaterialTheme.typography.labelLarge
    val metrics = rememberTextMeasurer().measure("Ag", style, maxLines = 1, softWrap = false)
    val height = with(LocalDensity.current) { metrics.size.height.toDp() + 24.dp }.coerceAtLeast(56.dp)
    Row(
        modifier.fillMaxWidth().height(height).padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(
            onClick = hapticAction(onClick),
            enabled = enabled,
            modifier = actionTag?.let { Modifier.testTag(it) } ?: Modifier,
        ) {
            Text(label, style = style, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
        }
    }
}
