package com.saaf.milkshop.nativeui.common

import androidx.compose.runtime.compositionLocalOf

/**
 * Role-scoped action that opens the day close/reopen flow, provided by the
 * preview activity around each role host. `null` for roles without close-day
 * permission (Cashier, Operations, Administrator) — their header day chip
 * stays a passive indicator.
 */
val LocalDayStatusAction = compositionLocalOf<(() -> Unit)?> { null }
