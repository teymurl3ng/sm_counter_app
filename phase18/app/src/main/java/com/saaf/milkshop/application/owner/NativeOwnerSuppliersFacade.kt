package com.saaf.milkshop.application.owner

import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.domain.AppPermission
import com.saaf.milkshop.domain.AppRole
import com.saaf.milkshop.domain.UserSecurityService
import com.saaf.milkshop.domain.VendorManagementService
import com.saaf.milkshop.domain.VendorProfileInput
import com.saaf.milkshop.domain.VendorProfileService
import java.util.Locale

data class NativeOwnerSupplierRow(
    val accountId: Long,
    val name: String,
    val balanceLabel: String,
    val statusLabel: String,
    val phone: String?,
    val location: String?,
)

data class NativeOwnerSupplierDetail(
    val row: NativeOwnerSupplierRow,
    val notes: String?,
    val active: Boolean,
)

data class NativeOwnerSuppliersSnapshot(
    val suppliers: List<NativeOwnerSupplierRow>,
)

/**
 * Owner Suppliers boundary: vendor list + read detail + create/rename/profile
 * from [VendorManagementService] / [VendorProfileService]. Does not invent
 * balance formulas or call bridge JSON.
 */
class NativeOwnerSuppliersFacade(
    private val database: MilkShopDatabase,
    private val security: UserSecurityService,
    private val vendors: VendorManagementService = VendorManagementService(database),
    private val profiles: VendorProfileService = VendorProfileService(database),
) {
    suspend fun loadSuppliers(sessionHandle: String): NativeOwnerSuppliersSnapshot {
        requireOwnerMasterData(sessionHandle)
        val rows = vendors.list().map { info ->
            val profile = profiles.get(info.accountId)
            info.toRow(
                active = profile?.active ?: true,
                phone = profile?.phone,
                location = profile?.location,
            )
        }
        return NativeOwnerSuppliersSnapshot(suppliers = rows)
    }

    suspend fun loadDetail(
        sessionHandle: String,
        accountId: Long,
    ): NativeOwnerSupplierDetail {
        requireOwnerMasterData(sessionHandle)
        val info = vendors.list().firstOrNull { it.accountId == accountId }
            ?: error("This supplier is not in the supplier list.")
        val profile = profiles.get(accountId)
        val active = profile?.active ?: true
        val row = info.toRow(
            active = active,
            phone = profile?.phone,
            location = profile?.location,
        )
        return NativeOwnerSupplierDetail(
            row = row,
            notes = profile?.notes,
            active = active,
        )
    }

    suspend fun createSupplier(
        sessionHandle: String,
        name: String,
    ): Long {
        requireOwnerMasterData(sessionHandle)
        return vendors.create(name)
    }

    /**
     * Rename then save profile (auth once). Deactivate is [VendorProfileInput.active] = false.
     */
    suspend fun updateSupplier(
        sessionHandle: String,
        accountId: Long,
        name: String,
        profile: VendorProfileInput,
    ) {
        requireOwnerMasterData(sessionHandle)
        vendors.rename(accountId, name)
        profiles.save(accountId, profile)
    }

    private suspend fun requireOwnerMasterData(sessionHandle: String) {
        val actor = security.requirePermission(sessionHandle, AppPermission.MANAGE_MASTER_DATA)
        require(AppRole.parse(actor.role) == AppRole.OWNER) {
            "Owner Suppliers is only available to Owner."
        }
    }

    private fun VendorManagementService.VendorInfo.toRow(
        active: Boolean,
        phone: String?,
        location: String?,
    ): NativeOwnerSupplierRow = NativeOwnerSupplierRow(
        accountId = accountId,
        name = name,
        balanceLabel = formatMoney(balance),
        statusLabel = if (active) "Active" else "Inactive",
        phone = phone,
        location = location,
    )

    private fun formatMoney(amount: Double): String =
        String.format(Locale.US, "PKR %,.2f", amount)
}
