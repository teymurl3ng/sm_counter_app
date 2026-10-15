package com.saaf.milkshop.nativeui.sale

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.saaf.milkshop.application.sale.NativeSaleCreditSummary
import com.saaf.milkshop.nativeui.NativeUiTags
import com.saaf.milkshop.nativeui.common.NativeInformationSection
import com.saaf.milkshop.nativeui.common.NativeInformationFact

internal fun creditBalanceText(paisa: Long?): String = when {
    paisa == null -> "Unavailable"
    paisa < 0 -> "Advance PKR ${formatAmount(-paisa / 100.0)}"
    else -> "PKR ${formatAmount(paisa / 100.0)}"
}

@Composable
internal fun CreditSummaryContent(summary: NativeSaleCreditSummary?, loading: Boolean) {
    NativeInformationSection(Modifier.fillMaxWidth().testTag(NativeUiTags.SaleCreditSummary)) {
        if (loading) Text("Checking credit…", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
        else if (summary == null) Text("Credit summary unavailable. The credit check will run when you save.", color = MaterialTheme.colorScheme.onSurface)
        else {
            summary.currentStatus?.let { NativeInformationFact("Current credit",it) }
            summary.paymentTerms?.let { NativeInformationFact("New sale terms",it) }
            summary.statusExplanation?.let { Text(it,style=MaterialTheme.typography.bodyMedium) }
            NativeInformationFact(if ((summary.balancePaisa ?: 0) < 0) "Customer balance" else "Owes now",
                creditBalanceText(summary.balancePaisa), emphasized = true)
            NativeInformationFact("After sale", creditBalanceText(summary.projectedBalancePaisa), emphasized = true)
            NativeInformationFact("Credit limit", when {
                summary.checkUnavailable -> "Check unavailable"
                summary.limitPaisa == null -> "Not set"
                else -> creditBalanceText(summary.limitPaisa)
            })
            Text(summary.age.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            if (summary.warningKey != null) Text(if (summary.checkUnavailable) "Credit check unavailable. Review before saving."
                else "This sale goes above the advisory credit limit.", color = MaterialTheme.colorScheme.error)
        }
        Text("Based on records on this phone.", style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
