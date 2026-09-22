package com.saaf.milkshop.application.owner

import com.saaf.milkshop.application.more.NativeMoreSecurityStatus
import com.saaf.milkshop.application.readmodels.HybridReadModelService
import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.domain.AppPermission
import com.saaf.milkshop.domain.AppRole
import com.saaf.milkshop.domain.UserSecurityService
import com.saaf.milkshop.quickunlock.QuickUnlockService
import java.time.LocalDate

data class NativeOwnerMoreContext(
    val businessDate: String,
    val postingOpen: Boolean,
    val shopName: String,
)

/**
 * Owner More boundary: business-control menu context + personal Quick unlock.
 * Does not expose Cashier count cash or invent master-data / backup write UIs.
 */
class NativeOwnerMoreFacade(
    private val database: MilkShopDatabase,
    private val security: UserSecurityService,
    private val quickUnlock: QuickUnlockService? = null,
    private val reads: HybridReadModelService = HybridReadModelService(database, security),
) {
    suspend fun load(
        sessionHandle: String,
        date: LocalDate = LocalDate.now(),
    ): NativeOwnerMoreContext {
        requireOwner(sessionHandle)
        security.requirePermission(sessionHandle, AppPermission.VIEW_OPERATIONAL)
        val businessDate = date.toString()
        return NativeOwnerMoreContext(
            businessDate = businessDate,
            postingOpen = !database.dailyCloseDao().isPostingClosed(businessDate),
            shopName = database.storeProfileDao().get()?.storeName?.takeIf { it.isNotBlank() }
                ?: "Saaf Milk",
        )
    }

    suspend fun loadSecurity(sessionHandle: String): NativeMoreSecurityStatus {
        requireOwner(sessionHandle)
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

    suspend fun enrollQuickUnlock(
        sessionHandle: String,
        currentPassphrase: CharArray,
        enablePhoneSecurity: Boolean,
        pin: CharArray?,
    ): NativeMoreSecurityStatus {
        requireOwner(sessionHandle)
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
        requireOwner(sessionHandle)
        val service = quickUnlock
            ?: throw IllegalStateException("Quick unlock is unavailable in this preview.")
        try {
            service.remove(sessionHandle, currentPassphrase)
        } finally {
            currentPassphrase.fill('\u0000')
        }
        return loadSecurity(sessionHandle)
    }

    private suspend fun requireOwner(sessionHandle: String) {
        val actor = security.requirePermission(sessionHandle, AppPermission.VIEW_OPERATIONAL)
        val role = AppRole.parse(actor.role)
        // Manager / Admin reuse shop-tool + staff-directory context under their shells (bounded IA).
        require(role == AppRole.OWNER || role == AppRole.MANAGER || role == AppRole.ADMIN) {
            "More context is only available to Owner, Manager, or Administrator."
        }
    }
}
