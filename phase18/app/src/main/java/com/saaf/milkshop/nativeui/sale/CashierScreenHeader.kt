package com.saaf.milkshop.nativeui.sale

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.saaf.milkshop.nativeui.theme.SaafInk
import com.saaf.milkshop.nativeui.theme.SaafYellow
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

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
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
            )
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.labelLarge,
                    )
                    Text(
                        text = "$roleLabel · ${formatCashierBusinessDate(businessDate)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.82f),
                    )
                }
                Surface(
                    onClick = { onDayStatusClick?.invoke() },
                    enabled = onDayStatusClick != null,
                    color = if (postingOpen) SaafYellow else MaterialTheme.colorScheme.error,
                    contentColor = if (postingOpen) SaafInk else MaterialTheme.colorScheme.onError,
                    shape = RoundedCornerShape(999.dp),
                ) {
                    Text(
                        text = if (postingOpen) "Open" else "Closed",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
    }
}

fun formatCashierRoleLabel(wireValue: String): String =
    wireValue.replaceFirstChar { it.titlecase(Locale.US) }

fun formatCashierBusinessDate(value: String): String = runCatching {
    LocalDate.parse(value).format(DateTimeFormatter.ofPattern("d MMM uuuu", Locale.getDefault()))
}.getOrDefault(value)
