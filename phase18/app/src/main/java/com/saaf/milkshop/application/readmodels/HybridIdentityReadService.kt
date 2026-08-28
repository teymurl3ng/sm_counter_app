package com.saaf.milkshop.application.readmodels

import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.domain.AppPermission
import com.saaf.milkshop.domain.AppRole
import com.saaf.milkshop.domain.BusinessSettingsService
import com.saaf.milkshop.domain.OpeningStateService
import com.saaf.milkshop.domain.RolePermissions
import com.saaf.milkshop.domain.UserSecurityService

internal class HybridIdentityReadService(
    private val database: MilkShopDatabase,
    private val security: UserSecurityService,
) {
    suspend fun bootstrapStatus(): BootstrapStatusDto = BootstrapStatusDto(security.needsOwnerBootstrap())

    suspend fun currentSession(sessionHandle: String): CurrentSessionDto {
        val user = security.requirePermission(sessionHandle, AppPermission.VIEW_OPERATIONAL)
        val role = AppRole.parse(user.role)
        return CurrentSessionDto(
            authenticationStatusCode = "authenticated",
            userId = user.userId,
            loginName = user.loginName,
            displayName = user.displayName,
            roleCode = role.wireValue,
            employeeAccountId = user.employeeAccountId,
            effectivePermissionCodes = RolePermissions.permissions(role).map { it.name.lowercase() }.sorted(),
        )
    }

    suspend fun businessSettings(sessionHandle: String): BusinessSettingsDto {
        security.requirePermission(sessionHandle, AppPermission.MANAGE_BUSINESS_SETTINGS)
        val settings = BusinessSettingsService(database).get()
        val store = database.storeProfileDao().get()?.let {
            StoreIdentityDto(it.storeName, it.currencyCode, it.phone, it.address, it.updatedAt)
        }
        val opening = OpeningStateService(database).current()?.let { snapshot ->
            OpeningStateDto(
                eventId = snapshot.eventId,
                openingDate = snapshot.openingDate,
                inventoryBalances = snapshot.inventoryBalances.map {
                    OpeningInventoryBalanceDto(it.productId, decimal(it.quantity))
                },
                accountBalances = snapshot.accountBalances.map {
                    OpeningAccountBalanceDto(it.accountId, money(it.balance))
                },
                notes = snapshot.notes,
            )
        }
        return BusinessSettingsDto(
            store = store,
            vendorMilkRatePaisaPerUnit = money(settings.milkRateVendor),
            vendorDeliveryFeePaisaPerUnit = money(settings.deliveryFeeVendor),
            tsStandardPercent = decimal(settings.tsStandard),
            retailMilkRatePaisaPerUnit = money(settings.retailRateMilk),
            retailYogurtRatePaisaPerUnit = money(settings.retailRateYogurt),
            defaultDeliverySurchargePaisa = money(settings.defaultDeliverySurcharge),
            openingState = opening,
            expenseCategories = database.expenseCategoryProfileDao().all().map {
                ExpenseCategoryDto(it.accountId, it.displayName, it.active)
            },
        )
    }

    suspend fun masterData(sessionHandle: String): MasterDataDto {
        security.requirePermission(sessionHandle, AppPermission.MANAGE_MASTER_DATA)
        val stock = database.readModelDao().currentStock().associateBy { it.productId }
        val balances = database.readModelDao().accountBalances().associateBy { it.accountId }
        val productProfiles = database.productProfileDao().all().associateBy { it.productId }
        val customerProfiles = database.customerProfileDao().all().associateBy { it.accountId }
        val vendorProfiles = database.vendorProfileDao().all().associateBy { it.accountId }
        val employeeProfiles = database.employeeProfileDao().all().associateBy { it.accountId }
        val products = database.productDao().all().map { product ->
            val profile = productProfiles[product.productId]
            ProductDto(
                productId = product.productId,
                name = product.name,
                unit = product.unit,
                quantityOnHand = decimal(stock[product.productId]?.quantityOnHand ?: 0.0),
                defaultSaleRatePaisaPerUnit = profile?.defaultSaleRate?.let(::money),
                inventoryTracking = profile?.inventoryTracking ?: true,
                archived = profile?.archived ?: false,
                notes = profile?.notes,
            )
        }
        return MasterDataDto(
            products = products,
            customers = database.accountDao().findByCategory("Customer").map { account ->
                val profile = customerProfiles[account.accountId]
                CustomerDto(
                    accountId = account.accountId,
                    name = account.name.removePrefix("Customer: ").trim(),
                    canonicalBalancePaisa = money(balances[account.accountId]?.balance ?: 0.0),
                    phone = profile?.phone,
                    areaRoute = profile?.areaRoute,
                    address = profile?.address,
                    notes = profile?.notes,
                    creditLimitPaisa = profile?.creditLimit?.let(::money),
                    dailyQuota = profile?.dailyQuota?.let(::decimal),
                    active = profile?.active ?: true,
                )
            },
            vendors = database.accountDao().findByCategory("Vendor").map { account ->
                val profile = vendorProfiles[account.accountId]
                VendorDto(
                    accountId = account.accountId,
                    name = account.name.removePrefix("Vendor: ").trim(),
                    canonicalBalancePaisa = money(balances[account.accountId]?.balance ?: 0.0),
                    phone = profile?.phone,
                    location = profile?.location,
                    notes = profile?.notes,
                    active = profile?.active ?: true,
                )
            },
            employees = database.accountDao().findByCategory("Salary").map { account ->
                val profile = employeeProfiles[account.accountId]
                EmployeeDto(
                    accountId = account.accountId,
                    name = account.name,
                    canonicalBalancePaisa = money(balances[account.accountId]?.balance ?: 0.0),
                    roleLabel = profile?.role,
                    phone = profile?.phone,
                    referenceSalaryPaisa = profile?.referenceSalary?.let(::money),
                    salaryPeriodCode = profile?.salaryPeriod,
                    joinDate = profile?.joinDate,
                    active = profile?.active ?: true,
                )
            },
            expenseCategories = database.expenseCategoryProfileDao().all().map {
                ExpenseCategoryDto(it.accountId, it.displayName, it.active)
            },
        )
    }
}
