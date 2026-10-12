package com.saaf.milkshop.nativeui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.saaf.milkshop.nativeui.theme.nativeVisualColors
import com.saaf.milkshop.nativeui.theme.nativeSupportingTextStyle
import com.saaf.milkshop.nativeui.theme.nativeValueTextStyle
import com.saaf.milkshop.nativeui.theme.nativeMetricTextStyle
import com.saaf.milkshop.nativeui.theme.nativeSectionTextStyle

/** Coherent information, with the existing Section surface and one owned inset. */
@Composable
fun NativeInformationSection(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier.nativeListItem(NativeListStyle.Section).nativeListItemContent(NativeListStyle.Section),
        content = content,
    )
}

/** Actions move below long titles instead of squeezing either into a fixed slot. */
@Composable
fun NativeInformationHeading(
    title: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    actionTag: String = "",
    onAction: (() -> Unit)? = null,
) {
    val density = LocalDensity.current
    val measurer = rememberTextMeasurer()
    val headingStyle = nativeSectionTextStyle()
    val actionStyle = MaterialTheme.typography.labelLarge
    val hasAction = actionLabel != null && onAction != null
    val actionPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val available = with(density) { maxWidth.toPx() }
        val titleWidth = measurer.measure(title, style = headingStyle, softWrap = false).size.width
        val actionWidth = if (hasAction) {
            maxOf(with(density) { ButtonDefaults.MinWidth.toPx() },
                measurer.measure(actionLabel!!, style = actionStyle, softWrap = false).size.width + with(density) { 24.dp.toPx() })
        } else 0f
        val stacked = hasAction && (density.fontScale >= 1.5f ||
            titleWidth + actionWidth + with(density) { 12.dp.toPx() } > available)
        val heading: @Composable (Modifier) -> Unit = { textModifier ->
            Text(title, textModifier.semantics { heading() }, style = headingStyle,
                color = MaterialTheme.colorScheme.onSurface)
        }
        val action: @Composable (Modifier) -> Unit = { actionModifier ->
            if (actionLabel != null && onAction != null) {
                TextButton(
                    onClick = hapticAction(onAction),
                    modifier = actionModifier.heightIn(min = 48.dp).testTag(actionTag),
                    contentPadding = actionPadding,
                ) { Text(actionLabel, style = actionStyle) }
            }
        }
        if (stacked) {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                heading(Modifier.fillMaxWidth())
                action(Modifier.align(Alignment.End))
            }
        } else {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                heading(Modifier.weight(1f).alignByBaseline())
                action(Modifier.alignByBaseline())
            }
        }
    }
}

/** Read-only facts wrap as a pair, not as a clipped or misleading button row. */
@Composable
fun NativeInformationFact(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    stacked: Boolean = false,
    emphasized: Boolean = false,
    prose: Boolean = false,
) {
    val density = LocalDensity.current
    val measurer = rememberTextMeasurer()
    val labelStyle = nativeSupportingTextStyle()
    val valueStyle = when {
        emphasized -> nativeMetricTextStyle()
        prose -> MaterialTheme.typography.bodyLarge
        else -> nativeValueTextStyle()
    }
    val valueColor = if (emphasized) nativeVisualColors.metric else MaterialTheme.colorScheme.onSurface
    BoxWithConstraints(modifier.fillMaxWidth().padding(vertical = 8.dp).semantics(mergeDescendants = true) {}) {
        val gap = with(density) { 12.dp.toPx() }
        val available = with(density) { maxWidth.toPx() } - gap
        val labelWidth = measurer.measure(label, style = labelStyle, softWrap = false).size.width
        val valueWidth = measurer.measure(value, style = valueStyle, softWrap = false).size.width
        val useStacked = stacked || density.fontScale >= 1.5f || label.contains('\n') || value.contains('\n') ||
            labelWidth > available * .48f || valueWidth > available * .52f
        val labelText: @Composable (Modifier) -> Unit = { textModifier ->
            Text(label, textModifier, style = labelStyle, color = nativeVisualColors.supporting)
        }
        val valueText: @Composable (Modifier, TextAlign) -> Unit = { textModifier, alignment ->
            Text(value, textModifier, style = valueStyle, color = valueColor, textAlign = alignment)
        }
        if (useStacked) {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                labelText(Modifier.fillMaxWidth())
                valueText(Modifier.fillMaxWidth(), TextAlign.Start)
            }
        } else {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top) {
                labelText(Modifier.weight(.48f).alignByBaseline())
                valueText(Modifier.weight(.52f).alignByBaseline(), TextAlign.End)
            }
        }
    }
}
