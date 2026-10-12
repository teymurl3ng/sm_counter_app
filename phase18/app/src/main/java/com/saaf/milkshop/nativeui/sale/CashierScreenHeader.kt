package com.saaf.milkshop.nativeui.sale

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import com.saaf.milkshop.nativeui.theme.SaafInk
import com.saaf.milkshop.nativeui.common.LocalDayStatusAction
import com.saaf.milkshop.nativeui.theme.SaafYellow
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Static-chrome switch: hosts that draw the single green band themselves set
 * this to false around their panes, so any legacy in-pane header call inside
 * those panes renders nothing instead of doubling the band.
 */
val LocalPaneHeaderEnabled = compositionLocalOf { true }

/**
 * Shared Cashier chrome: green identity band used by Sell, Collect, and Activity.
 * Draws under the status bar (edge-to-edge) while keeping title/identity below it.
 */
@Composable
fun CashierScreenHeader(
    title: String,
    displayName: String,
    roleLabel: String,
    businessDate: String,
    postingOpen: Boolean,
    modifier: Modifier = Modifier,
    onDayStatusClick: (() -> Unit)? = null,
    showDayChip: Boolean = true,
) {
    if (!LocalPaneHeaderEnabled.current) return
    val dayStatusAction = onDayStatusClick ?: LocalDayStatusAction.current
    val statusStyle = MaterialTheme.typography.labelLarge.copy(
        fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold)
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    // Both states reserve the same space at the current global text setting.
    val labelWidth = maxOf(
        measurer.measure("Open", statusStyle, maxLines = 1, softWrap = false).size.width,
        measurer.measure("Closed", statusStyle, maxLines = 1, softWrap = false).size.width,
    )
    // Padding rounds each edge separately; combining 24dp can lose one physical pixel.
    val pillWidth = with(density) {
        (labelWidth + 12.dp.roundToPx() * 2 + 1).toDp()
    }.coerceAtLeast(72.dp)
    val actionWidth = pillWidth.coerceAtLeast(96.dp)
    val label = if (postingOpen) "Open" else "Closed"
    val click = com.saaf.milkshop.nativeui.common.hapticAction { dayStatusAction?.invoke() }
    com.saaf.milkshop.nativeui.common.NativeChromeBand(
        title = when (title) {
            "Cash count and close", "Cash count & close" -> "Count & close"
            else -> title
        },
        modifier = modifier,
        actionWidth = actionWidth,
        trailing = if (showDayChip) ({
            Box(Modifier.width(pillWidth).heightIn(min = 48.dp)
                .clip(RoundedCornerShape(50))
                .semantics(mergeDescendants = true) { contentDescription = "Day status: $label" }
                .then(if (dayStatusAction != null) Modifier.clickable(onClick = click) else Modifier),
                contentAlignment = Alignment.Center) {
            Surface(
                modifier = Modifier.width(pillWidth).heightIn(min = 32.dp),
                color = if (postingOpen) SaafYellow else MaterialTheme.colorScheme.error,
                contentColor = if (postingOpen) SaafInk else MaterialTheme.colorScheme.onError,
                shape = RoundedCornerShape(50),
            ) {
                Box(Modifier.padding(horizontal = 12.dp, vertical = 6.dp), contentAlignment = Alignment.Center) {
                    Text(label, modifier = Modifier.fillMaxWidth(), style = statusStyle,
                        textAlign = TextAlign.Center, maxLines = 1, softWrap = false)
                }
            }
            }
        }) else null,
    ) {
        com.saaf.milkshop.nativeui.common.NativeChromeContext(com.saaf.milkshop.nativeui.common.LocalShopName.current)
        com.saaf.milkshop.nativeui.common.NativeChromeContext(displayName, prominent = true)
        com.saaf.milkshop.nativeui.common.NativeChromeContext("$roleLabel · ${formatCashierBusinessDate(businessDate)}")
    }
}

fun formatCashierRoleLabel(wireValue: String): String =
    wireValue.replaceFirstChar { it.titlecase(Locale.US) }

fun formatCashierBusinessDate(value: String): String = runCatching {
    LocalDate.parse(value).format(DateTimeFormatter.ofPattern("d MMM uuuu", Locale.getDefault()))
}.getOrDefault(value)
