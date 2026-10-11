package com.saaf.milkshop.nativeui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext

/** Process-local receipt deduplication: returning to a result is not another save. */
class ReceiptOutcomeMemory {
    private val seen = LinkedHashSet<String>()
    @Synchronized fun accept(workflow: String, id: Long): Boolean {
        val fresh = seen.add("$workflow:$id")
        if (seen.size > 1024) seen.remove(seen.first())
        return fresh
    }
}

private val receiptOutcomes = ReceiptOutcomeMemory()

@Composable
fun NativeReceiptFeedback(workflow: String, id: Long?) {
    val context = LocalContext.current
    val active = LocalPaneActive.current
    LaunchedEffect(workflow, id, active) {
        if (active && id != null && receiptOutcomes.accept(workflow, id)) context.hapticConfirm()
    }
}
