package com.saaf.milkshop.nativeui.owner

import com.saaf.milkshop.application.owner.NativeOwnerBackupFacade
import com.saaf.milkshop.application.owner.NativeOwnerCostingPlansFacade
import com.saaf.milkshop.application.owner.NativeOwnerCustomersFacade
import com.saaf.milkshop.application.owner.NativeOwnerEmployeesFacade
import com.saaf.milkshop.application.owner.NativeOwnerMoreFacade
import com.saaf.milkshop.application.owner.NativeOwnerProductsFacade
import com.saaf.milkshop.application.owner.NativeOwnerSecurityHistoryFacade
import com.saaf.milkshop.application.owner.NativeOwnerSyncFacade
import com.saaf.milkshop.application.owner.NativeOwnerShopSettingsFacade
import com.saaf.milkshop.application.owner.NativeOwnerSuppliersFacade
import com.saaf.milkshop.application.owner.NativeOwnerUsersRolesFacade

/** Construction boundary for the Owner More feature family. */
internal data class OwnerMoreDependencies(
    val more: NativeOwnerMoreFacade,
    val products: NativeOwnerProductsFacade,
    val customers: NativeOwnerCustomersFacade,
    val suppliers: NativeOwnerSuppliersFacade,
    val employees: NativeOwnerEmployeesFacade,
    val costing: NativeOwnerCostingPlansFacade,
    val shopSettings: NativeOwnerShopSettingsFacade,
    val usersRoles: NativeOwnerUsersRolesFacade,
    val securityHistory: NativeOwnerSecurityHistoryFacade,
    val backup: NativeOwnerBackupFacade,
    val sync: NativeOwnerSyncFacade,
)
