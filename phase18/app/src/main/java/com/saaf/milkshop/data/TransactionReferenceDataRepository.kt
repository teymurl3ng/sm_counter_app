package com.saaf.milkshop.data

import com.saaf.milkshop.domain.CustomerManagementService
import com.saaf.milkshop.domain.EmployeeManagementService
import com.saaf.milkshop.domain.SettingKeys
import com.saaf.milkshop.domain.VendorManagementService

/**
 * Read-side application data boundary for transaction and management-facing reference data.
 * Existing DAOs and management services remain authoritative; this class only composes them
 * into the exact reference datasets the presentation layer already consumed.
 */
class TransactionReferenceDataRepository(
    private val database: MilkShopDatabase,
) {
    data class PurchaseSettings(
        val milkRate: Double?,
        val deliveryFee: Double?,
        val tsStandard: Double?,
    )

    data class AccountRef(val accountId: Long, val name: String)
    data class ProductRef(val productId: Long, val name: String, val unit: String)

    data class VendorData(
        val accounts: List<AccountRef>,
        val managedVendors: List<VendorManagementService.VendorInfo>,
    )

    data class EmployeeData(
        val accounts: List<AccountRef>,
        val managedEmployees: List<EmployeeManagementService.EmployeeInfo>,
    )

    data class SaleSettings(
        val milkRate: Double?,
        val yogurtRate: Double?,
        val defaultSurcharge: Double?,
    )

    data class SaleReferenceData(
        val products: List<ProductRef>,
        val customers: List<AccountRef>,
        val settings: SaleSettings,
        val managedCustomers: List<CustomerManagementService.CustomerInfo>,
    )

    suspend fun loadPurchaseSettings(): PurchaseSettings {
        val settingsDao = database.settingsDao()
        return PurchaseSettings(
            milkRate = settingsDao.getValue(SettingKeys.MILK_RATE_VENDOR),
            deliveryFee = settingsDao.getValue(SettingKeys.DELIVERY_FEE_VENDOR),
            tsStandard = settingsDao.getValue(SettingKeys.TS_STANDARD),
        )
    }

    suspend fun loadVendors(): VendorData {
        val accounts = database.accountDao().findByCategory("Vendor")
            .map { AccountRef(it.accountId, it.name) }
        return VendorData(
            accounts = accounts,
            managedVendors = VendorManagementService(database).list(),
        )
    }

    suspend fun loadPaymentAccounts(): List<AccountRef> =
        database.accountDao().findByCategory("Cash/Bank")
            .map { AccountRef(it.accountId, it.name) }

    suspend fun loadEmployees(): EmployeeData {
        val accounts = database.accountDao().findByCategory("Salary")
            .map { AccountRef(it.accountId, it.name) }
        return EmployeeData(
            accounts = accounts,
            managedEmployees = EmployeeManagementService(database).list(),
        )
    }

    suspend fun loadSaleReferenceData(): SaleReferenceData {
        val milk = database.productDao().findByName("Milk")
        val yogurt = database.productDao().findByName("Yogurt")
        val products = listOfNotNull(milk, yogurt)
            .map { ProductRef(it.productId, it.name, it.unit) }
        val customers = database.accountDao().findByCategory("Customer")
            .map { AccountRef(it.accountId, it.name) }
        val settingsDao = database.settingsDao()
        val settings = SaleSettings(
            milkRate = settingsDao.getValue(SettingKeys.RETAIL_RATE_MILK),
            yogurtRate = settingsDao.getValue(SettingKeys.RETAIL_RATE_YOGURT),
            defaultSurcharge = settingsDao.getValue(SettingKeys.DEFAULT_DELIVERY_SURCHARGE),
        )
        return SaleReferenceData(
            products = products,
            customers = customers,
            settings = settings,
            managedCustomers = CustomerManagementService(database).list(),
        )
    }
}
