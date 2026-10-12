package com.saaf.milkshop

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.saaf.milkshop.data.AccountEntity
import com.saaf.milkshop.data.CustomerProfileEntity
import com.saaf.milkshop.data.EmployeeProfileEntity
import com.saaf.milkshop.data.ExpenseCategoryProfileEntity
import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.data.ProductEntity
import com.saaf.milkshop.data.ProductProfileEntity
import com.saaf.milkshop.data.VendorProfileEntity
import com.saaf.milkshop.domain.ProductCatalogService
import com.saaf.milkshop.domain.ExpenseCategoryManagementService
import com.saaf.milkshop.sync.SyncApplyEngine
import com.saaf.milkshop.sync.SyncEnvelopeFactory
import com.saaf.milkshop.sync.SyncPulledRecord
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SyncProfileReceiverTest {
    private val createdAt = "2026-10-01T08:00:00Z"
    private val productUid = "u1_profile_test_product"
    private val customerUid = "u1_profile_test_customer"
    private val vendorUid = "u1_profile_test_vendor"
    private val employeeUid = "u1_profile_test_employee"
    private val expenseUid = "u1_profile_test_expense"
    private val uids = setOf(productUid, customerUid, vendorUid, employeeUid, expenseUid)

    @Test
    fun allFiveProfiles_insertUpdateAndReplayWithDifferentLocalIds() = runBlocking {
        val source = MilkShopDatabase.createInMemory()
        val target = MilkShopDatabase.createInMemory()
        try {
            target.productDao().insert(ProductEntity(name = "Receiver only", unit = "Kg", uid = "u1_receiver_only_product"))
            target.accountDao().insert(AccountEntity(name = "Receiver cash", type = "Asset", category = "Cash/Bank", uid = "u1_receiver_only_account"))
            val productId = source.productDao().insert(ProductEntity(name = "Cream", unit = "Kg", uid = productUid))
            val customerId = source.accountDao().insert(AccountEntity(name = "Customer: A", type = "Asset", category = "Customer", uid = customerUid))
            val vendorId = source.accountDao().insert(AccountEntity(name = "Vendor: B", type = "Liability", category = "Vendor", uid = vendorUid))
            val employeeId = source.accountDao().insert(AccountEntity(name = "Salary: C", type = "Expense", category = "Salary", uid = employeeUid))
            val expenseId = source.accountDao().insert(AccountEntity(name = "Expense: Cold Storage", type = "Expense", category = "Operating Expense", uid = expenseUid))
            source.productProfileDao().save(ProductProfileEntity(productId, 275.5, false, true, "Rich", createdAt))
            source.customerProfileDao().save(CustomerProfileEntity(customerId, "0300", "North", "Street 2", "Morning", 500.0, 3.5, false, createdAt))
            source.vendorProfileDao().save(VendorProfileEntity(vendorId, "0311", "Market", "Weekly", false, createdAt))
            source.employeeProfileDao().save(EmployeeProfileEntity(employeeId, "Driver", "0322", 40000.0, "month", "2026-09-01", false, createdAt))
            source.expenseCategoryProfileDao().insert(ExpenseCategoryProfileEntity(expenseId, "Cold Storage", false, createdAt))

            val first = envelopes(source).mapIndexed { index, envelope -> record(envelope, index + 1L) }
            assertEquals(5, first.size)
            val report = SyncApplyEngine(target).apply(first)
            assertEquals(5, report.applied)
            assertEquals(0, report.skipped)
            val receivedProduct = target.syncEnvelopeDao().productsByUid(listOf(productUid)).single()
            val receivedCustomer = target.syncEnvelopeDao().accountsByUid(listOf(customerUid)).single()
            val receivedVendor = target.syncEnvelopeDao().accountsByUid(listOf(vendorUid)).single()
            val receivedEmployee = target.syncEnvelopeDao().accountsByUid(listOf(employeeUid)).single()
            val receivedExpense = target.syncEnvelopeDao().accountsByUid(listOf(expenseUid)).single()
            assertNotEquals(productId, receivedProduct.productId)
            assertNotEquals(customerId, receivedCustomer.accountId)
            assertEquals(ProductProfileEntity(receivedProduct.productId, 275.5, false, true, "Rich", createdAt), target.productProfileDao().findByProductId(receivedProduct.productId))
            assertEquals(CustomerProfileEntity(receivedCustomer.accountId, "0300", "North", "Street 2", "Morning", 500.0, 3.5, false, createdAt), target.customerProfileDao().findByAccountId(receivedCustomer.accountId))
            assertEquals(VendorProfileEntity(receivedVendor.accountId, "0311", "Market", "Weekly", false, createdAt), target.vendorProfileDao().findByAccountId(receivedVendor.accountId))
            assertEquals(EmployeeProfileEntity(receivedEmployee.accountId, "Driver", "0322", 40000.0, "month", "2026-09-01", false, createdAt), target.employeeProfileDao().findByAccountId(receivedEmployee.accountId))
            assertEquals(ExpenseCategoryProfileEntity(receivedExpense.accountId, "Cold Storage", false, createdAt), target.expenseCategoryProfileDao().findByAccountId(receivedExpense.accountId))

            source.productProfileDao().save(ProductProfileEntity(productId, null, true, false, null, createdAt))
            source.customerProfileDao().save(CustomerProfileEntity(customerId, null, null, null, null, null, null, true, createdAt))
            source.vendorProfileDao().save(VendorProfileEntity(vendorId, null, null, null, true, createdAt))
            source.employeeProfileDao().save(EmployeeProfileEntity(employeeId, null, null, null, null, null, true, createdAt))
            source.expenseCategoryProfileDao().update(expenseId, "Cold Room", true, createdAt)
            val updated = envelopes(source).mapIndexed { index, envelope -> record(envelope, index + 10L) }
            assertEquals(5, SyncApplyEngine(target).apply(updated).updated)
            assertEquals(ProductProfileEntity(receivedProduct.productId, null, true, false, null, createdAt), target.productProfileDao().findByProductId(receivedProduct.productId))
            assertEquals(CustomerProfileEntity(receivedCustomer.accountId, null, null, null, null, null, null, true, createdAt), target.customerProfileDao().findByAccountId(receivedCustomer.accountId))
            assertEquals(VendorProfileEntity(receivedVendor.accountId, null, null, null, true, createdAt), target.vendorProfileDao().findByAccountId(receivedVendor.accountId))
            assertEquals(EmployeeProfileEntity(receivedEmployee.accountId, null, null, null, null, null, true, createdAt), target.employeeProfileDao().findByAccountId(receivedEmployee.accountId))
            assertEquals(ExpenseCategoryProfileEntity(receivedExpense.accountId, "Cold Room", true, createdAt),
                target.expenseCategoryProfileDao().findByAccountId(receivedExpense.accountId))
            assertEquals(null, ProductCatalogService(target).list().single { it.productId == receivedProduct.productId }.defaultSaleRate)
            assertEquals("Cold Room", ExpenseCategoryManagementService(target).list().single { it.accountId == receivedExpense.accountId }.displayName)
            SyncApplyEngine(target).apply(updated)
            assertEquals(5, target.backupDataDao().accounts().count { it.uid in uids } + target.backupDataDao().products().count { it.uid in uids })
            uids.forEach { uid ->
                val table = if (uid == productUid) "products" else "accounts"
                assertEquals(0, target.syncOutboxDao().countByTableAndUid(table, uid))
            }
        } finally {
            source.close()
            target.close()
        }
    }

    @Test
    fun legacyMissingProfilePreservesSatelliteAndMalformedObjectRollsBackParent() = runBlocking {
        val target = MilkShopDatabase.createInMemory()
        try {
            val id = target.productDao().insert(ProductEntity(name = "Cream", unit = "Kg", uid = productUid))
            target.productProfileDao().save(ProductProfileEntity(id, 250.0, true, false, "Local", createdAt))
            target.syncOutboxDao().deleteEntry("products", productUid)
            val parentOnly = JSONObject().put("name", "Cream renamed").put("unit", "Kg")
            assertEquals(1, SyncApplyEngine(target).apply(listOf(SyncPulledRecord(1, null, "products", productUid, parentOnly, "sender"))).updated)
            assertEquals(250.0, target.productProfileDao().findByProductId(id)?.defaultSaleRate)

            val invalid = JSONObject(parentOnly.toString()).put("name", "Must roll back")
                .put("profile", JSONObject().put("default_sale_rate", -2.0))
            assertEquals(1, SyncApplyEngine(target).apply(listOf(SyncPulledRecord(2, null, "products", productUid, invalid, "sender"))).skipped)
            assertEquals("Cream renamed", target.syncEnvelopeDao().productsByUid(listOf(productUid)).single().name)
            assertEquals(250.0, target.productProfileDao().findByProductId(id)?.defaultSaleRate)
            val explicitNull = JSONObject(parentOnly.toString()).put("profile", JSONObject.NULL)
            assertEquals(1, SyncApplyEngine(target).apply(listOf(SyncPulledRecord(3, null, "products", productUid, explicitNull, "sender"))).skipped)
            assertNotNull(target.productProfileDao().findByProductId(id))

            target.productDao().updateIdentity(id, "Cream local edit", "Kg")
            assertEquals(1, target.syncOutboxDao().countByTableAndUid("products", productUid))
            val remote = JSONObject(parentOnly.toString()).put("profile", JSONObject()
                .put("default_sale_rate", JSONObject.NULL).put("inventory_tracking", false)
                .put("archived", true).put("notes", JSONObject.NULL).put("created_at", createdAt))
            val pending = SyncApplyEngine(target).apply(listOf(SyncPulledRecord(4, null, "products", productUid, remote, "sender")))
            assertEquals(1, pending.parked)
            assertEquals("Cream local edit", target.syncEnvelopeDao().productsByUid(listOf(productUid)).single().name)
            assertEquals(250.0, target.productProfileDao().findByProductId(id)?.defaultSaleRate)
            assertNotNull(target.syncParkedRowDao().find("products", productUid))
        } finally {
            target.close()
        }
    }

    private suspend fun envelopes(database: MilkShopDatabase): List<JSONObject> =
        SyncEnvelopeFactory(database).build(database.syncOutboxDao().nextBatch(100)).envelopes
            .filter { it.getString("uid") in uids }

    private fun record(envelope: JSONObject, seq: Long) = SyncPulledRecord(
        serverSeq = seq,
        shopId = null,
        tableName = envelope.getString("table"),
        rowUid = envelope.getString("uid"),
        payload = envelope.getJSONObject("payload"),
        originDevice = "sender",
    )
}
