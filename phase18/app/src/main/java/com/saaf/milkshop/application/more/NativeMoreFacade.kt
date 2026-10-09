package com.saaf.milkshop.application.more

import com.saaf.milkshop.application.commands.ReconciliationBridgeCommandService
import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.domain.AppPermission
import com.saaf.milkshop.domain.PhysicalCashCountInput
import com.saaf.milkshop.domain.UserSecurityService
import com.saaf.milkshop.quickunlock.QuickUnlockService
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import com.saaf.milkshop.sync.BusinessClock

data class NativeMoreContext(
    val businessDate: String,
    val postingOpen: Boolean,
    val cashAccountId: Long,
    val cashAccountName: String,
)

data class NativeMoreSecurityStatus(
    val quickPinEnabled: Boolean,
    val phoneSecurityEnabled: Boolean,
    val phoneSecurityAvailable: Boolean,
    val phoneSecurityLabel: String,
)

data class NativeBlindCashCountReceipt(
    val countId: Long,
    val businessDate: String,
    val countedCashPaisa: Long,
)

/**
 * Cashier More boundary: blind cash count + personal Quick unlock status.
 * Never returns expected cash or variance to the presentation layer.
 */
class NativeMoreFacade(
    private val database: MilkShopDatabase,
    private val security: UserSecurityService,
    private val quickUnlock: QuickUnlockService? = null,
) {
    private val reconciliation = ReconciliationBridgeCommandService(database, security)

    suspend fun load(
        sessionHandle: String,
        date: LocalDate = BusinessClock.today(),
    ): NativeMoreContext {
        security.requirePermission(sessionHandle, AppPermission.VIEW_OPERATIONAL)
        val cash = database.accountDao().findByCategory("Cash/Bank")
            .singleOrNull { it.name == "Cash" }
            ?: throw IllegalStateException("The Cash drawer account is unavailable.")
        val businessDate = date.toString()
        return NativeMoreContext(
            businessDate = businessDate,
            postingOpen = !database.dailyCloseDao().isPostingClosed(businessDate),
            cashAccountId = cash.accountId,
            cashAccountName = cash.name,
        )
    }

    suspend fun loadSecurity(
        sessionHandle: String,
    ): NativeMoreSecurityStatus {
        val actor = security.requirePermission(sessionHandle, AppPermission.VIEW_OPERATIONAL)
        val service = quickUnlock
            ?: return NativeMoreSecurityStatus(
                quickPinEnabled = false,
                phoneSecurityEnabled = false,
                phoneSecurityAvailable = false,
                phoneSecurityLabel = "Phone security",
            )
        val status = service.status()
        val enrollment = status.accounts.singleOrNull { it.loginName == actor.loginName }
        return NativeMoreSecurityStatus(
            quickPinEnabled = enrollment?.pinEnabled == true,
            phoneSecurityEnabled = enrollment?.deviceAuthenticationEnabled == true,
            phoneSecurityAvailable = status.deviceAuthenticationAvailable,
            phoneSecurityLabel = status.deviceAuthenticationLabel,
        )
    }

    suspend fun recordBlindCashCount(
        sessionHandle: String,
        countedCashPaisa: Long,
        notes: String?,
        date: LocalDate = BusinessClock.today(),
    ): NativeBlindCashCountReceipt {
        require(countedCashPaisa >= 0L) { "Counted drawer cash cannot be negative." }
        val context = load(sessionHandle, date)
        security.requirePermission(sessionHandle, AppPermission.CASH_COUNT_AND_CLOSE)
        if (!context.postingOpen) {
            throw IllegalStateException("This business day is closed. Reopen it before counting cash.")
        }
        val result = reconciliation.recordCount(
            sessionHandle,
            PhysicalCashCountInput(
                accountId = context.cashAccountId,
                businessDate = context.businessDate,
                countedAmount = BigDecimal(countedCashPaisa).movePointLeft(2).toDouble(),
                notes = notes?.trim()?.takeIf { it.isNotEmpty() },
            ),
        )
        return NativeBlindCashCountReceipt(
            countId = result.count.countId,
            businessDate = context.businessDate,
            countedCashPaisa = moneyToPaisa(result.count.countedAmount),
        )
    }

    suspend fun enrollQuickUnlock(
        sessionHandle: String,
        currentPassphrase: CharArray,
        enablePhoneSecurity: Boolean,
        pin: CharArray?,
    ): NativeMoreSecurityStatus {
        val service = quickUnlock
            ?: throw IllegalStateException("Quick unlock is unavailable in this preview.")
        try {
            service.enroll(
                sessionId = sessionHandle,
                currentPassphrase = currentPassphrase,
                enableDeviceAuthentication = enablePhoneSecurity,
                pin = pin,
            )
        } finally {
            currentPassphrase.fill('\u0000')
            pin?.fill('\u0000')
        }
        return loadSecurity(sessionHandle)
    }

    suspend fun removeQuickUnlock(
        sessionHandle: String,
        currentPassphrase: CharArray,
    ): NativeMoreSecurityStatus {
        val service = quickUnlock
            ?: throw IllegalStateException("Quick unlock is unavailable in this preview.")
        try {
            service.remove(sessionHandle, currentPassphrase)
        } finally {
            currentPassphrase.fill('\u0000')
        }
        return loadSecurity(sessionHandle)
    }

    private fun moneyToPaisa(value: Double): Long = BigDecimal.valueOf(value)
        .movePointRight(2)
        .setScale(0, RoundingMode.HALF_UP)
        .longValueExact()
}
