package com.saaf.milkshop.nativeui.sale

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    com.saaf.milkshop.nativeui.common.NativeChromeBand(
        title = when (title) {
            "Cash count and close", "Cash count & close" -> "Count & close"
            else -> title
        },
        modifier = modifier,
        trailing = if (showDayChip) ({
            Surface(
                onClick = com.saaf.milkshop.nativeui.common.hapticAction { dayStatusAction?.invoke() },
                enabled = dayStatusAction != null,
                modifier = Modifier.height(48.dp),
                color = if (postingOpen) SaafYellow else MaterialTheme.colorScheme.error,
                contentColor = if (postingOpen) SaafInk else MaterialTheme.colorScheme.onError,
                shape = RoundedCornerShape(999.dp),
            ) {
                Text(if (postingOpen) "Open" else "Closed",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.sp, lineHeight = 16.sp),
                    maxLines = 1)
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
