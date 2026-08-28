package com.saaf.milkshop

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.saaf.milkshop.application.readmodels.AccountLedgerDto
import com.saaf.milkshop.application.readmodels.AuditActorDto
import com.saaf.milkshop.application.readmodels.BootstrapStatusDto
import com.saaf.milkshop.application.readmodels.BusinessSettingsDto
import com.saaf.milkshop.application.readmodels.CurrentSessionDto
import com.saaf.milkshop.application.readmodels.CostingManagementDto
import com.saaf.milkshop.application.readmodels.EventDetailDto
import com.saaf.milkshop.application.readmodels.EventHistoryDto
import com.saaf.milkshop.application.readmodels.HybridReadModelService
import com.saaf.milkshop.application.readmodels.ProfitabilityReportDto
import com.saaf.milkshop.application.readmodels.SecurityAuditDto
import com.saaf.milkshop.application.readmodels.UserAdministrationDto
import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.domain.CreateUserInput
import com.saaf.milkshop.domain.OpeningAccountBalance
import com.saaf.milkshop.domain.OpeningInventoryBalance
import com.saaf.milkshop.domain.OpeningStateInput
import com.saaf.milkshop.domain.OpeningStateService
import com.saaf.milkshop.domain.SecurityFailure
import com.saaf.milkshop.domain.StoreIdentityInput
import com.saaf.milkshop.domain.StoreIdentityService
import com.saaf.milkshop.domain.UserSecurityService
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Phase5HybridReadModelTest {
    private val database = MilkShopDatabase.createInMemory()

    @After
    fun closeDatabase() = database.close()

    @Test
    fun bootstrapAndCurrentSession_exposeStatusIdentityAndEffectivePermissionsWithoutSessionSecret() = runBlocking {
        val security = UserSecurityService(database)
        val reads = HybridReadModelService(database, security)
        assertTrue(reads.bootstrapStatus().needsOwnerBootstrap)

        val owner = security.bootstrapOwner("phase5.owner", "Phase 5 Owner", OWNER_PASSWORD.toCharArray())
        assertFalse(reads.bootstrapStatus().needsOwnerBootstrap)
        val session = reads.currentSession(owner.session.sessionId)

        assertEquals(owner.user.userId, session.userId)
        assertEquals("authenticated", session.authenticationStatusCode)
        assertEquals("Phase 5 Owner", session.displayName)
        assertEquals("owner", session.roleCode)
        assertTrue("view_full_financial" in session.effectivePermissionCodes)
        assertTrue("manage_staff_users" in session.effectivePermissionCodes)
        assertFalse(CurrentSessionDto::class.java.declaredFields.any { it.name.contains("session", ignoreCase = true) })
    }

    @Test
    fun ownerReads_usePaisaCanonicalDecimalsCorrectionsAndPkrStoreIdentity() = runBlocking {
        val security = UserSecurityService(database)
        val owner = security.bootstrapOwner("read.owner", "Read Owner", OWNER_PASSWORD.toCharArray())
        val reads = HybridReadModelService(database, security)
        StoreIdentityService(database).save(StoreIdentityInput("Saaf Test Shop", "03001234567", "Test market"))

        val milkId = database.productDao().findByName("Milk")!!.productId
        val cashId = database.accountDao().findByCategory("Cash/Bank").single { it.name == "Cash" }.accountId
        val original = OpeningStateService(database).record(
            OpeningStateInput(
                openingDate = "2026-08-28",
                inventoryBalances = listOf(OpeningInventoryBalance(milkId, 10.5)),
                accountBalances = listOf(OpeningAccountBalance(cashId, 100.12)),
            )
        )
        val corrected = OpeningStateService(database).correct(
            OpeningStateInput(
                openingDate = "2026-08-28",
                inventoryBalances = listOf(OpeningInventoryBalance(milkId, 9.75)),
                accountBalances = listOf(OpeningAccountBalance(cashId, 101.23)),
                notes = "Count corrected",
            )
        )

        val settings = reads.businessSettings(owner.session.sessionId)
        assertEquals("Saaf Test Shop", settings.store?.storeName)
        assertEquals("PKR", settings.store?.currencyCode)
        assertEquals(17_000L, settings.vendorMilkRatePaisaPerUnit)
        assertEquals("13", settings.tsStandardPercent)
        assertEquals(10_123L, settings.openingState?.accountBalances?.single()?.balancePaisa)
        assertEquals("9.75", settings.openingState?.inventoryBalances?.single()?.quantity)

        val history = reads.eventHistory(owner.session.sessionId)
        assertTrue(history.single { it.eventId == original.eventId }.isSuperseded)
        assertTrue(history.single { it.eventId == corrected.eventId }.isCorrection)
        val detail = reads.eventDetail(owner.session.sessionId, corrected.eventId)
        assertEquals(-75L, detail.inventoryMovements.single().quantity.toBigDecimal().movePointRight(2).longValueExact())
        assertEquals(111L, detail.financialMovements!!.single().amountPaisa)

        val dashboard = reads.dashboard(owner.session.sessionId, "2026-08-28", "2026-08-29")
        assertEquals("9.75", dashboard.stock.single { it.productId == milkId }.quantityOnHand)
        val cashierSummary = reads.cashierSummary(owner.session.sessionId, "2026-08-28", "2026-08-29")
        assertEquals(10_123L, cashierSummary.paymentAccounts.single { it.accountId == cashId }.canonicalBalancePaisa)

        val costing = reads.costingManagement(owner.session.sessionId)
        assertTrue("missing_basis" in costing.codes.coverageCodes)
        assertTrue("quarterly_variance" in costing.codes.reviewReasonCodes)
        assertTrue("power_failure" in costing.codes.lossReasonCodes)
    }

    @Test
    fun cashierGetsOperationalReads_butOwnerFinancialSettingsUsersAndAuditRemainDenied() = runBlocking {
        val security = UserSecurityService(database)
        val owner = security.bootstrapOwner("gate.owner", "Gate Owner", OWNER_PASSWORD.toCharArray())
        val pending = security.beginUserEnrollment(
            owner.session.sessionId,
            CreateUserInput("gate.cashier", "Gate Cashier", "cashier"),
        )
        security.completeUserEnrollment(pending.enrollmentToken, CASHIER_PASSWORD.toCharArray())
        val cashier = security.login("gate.cashier", CASHIER_PASSWORD.toCharArray())
        val operatorPending = security.beginUserEnrollment(
            owner.session.sessionId,
            CreateUserInput("gate.operator", "Gate Operator", "operator"),
        )
        security.completeUserEnrollment(operatorPending.enrollmentToken, OPERATOR_PASSWORD.toCharArray())
        val operator = security.login("gate.operator", OPERATOR_PASSWORD.toCharArray())
        val reads = HybridReadModelService(database, security)

        assertEquals("cashier", reads.currentSession(cashier.sessionId).roleCode)
        val cashierReferences = reads.transactionReferences(cashier.sessionId)
        assertTrue(cashierReferences.paymentAccounts.isNotEmpty())
        assertNull(cashierReferences.vendorMilkRatePaisaPerUnit)
        assertEquals(19_000L, cashierReferences.retailMilkRatePaisaPerUnit)
        assertTrue(reads.dashboard(cashier.sessionId, "2026-08-28", "2026-08-29").stock.isNotEmpty())
        assertTrue(reads.cashierSummary(cashier.sessionId, "2026-08-28", "2026-08-29").paymentAccounts.isNotEmpty())

        val operatorReferences = reads.transactionReferences(operator.sessionId)
        assertTrue(operatorReferences.paymentAccounts.isEmpty())
        assertTrue(operatorReferences.customers.isEmpty())
        assertEquals(17_000L, operatorReferences.vendorMilkRatePaisaPerUnit)
        assertNull(operatorReferences.retailMilkRatePaisaPerUnit)
        expectPermissionDenied { reads.cashierSummary(operator.sessionId, "2026-08-28", "2026-08-29") }

        expectPermissionDenied { reads.financialOverview(cashier.sessionId, "2026-08-28", "2026-08-29") }
        expectPermissionDenied { reads.businessSettings(cashier.sessionId) }
        expectPermissionDenied { reads.masterData(cashier.sessionId) }
        expectPermissionDenied { reads.profitabilityReport(cashier.sessionId, "2026-08-28", "2026-08-29") }
        expectPermissionDenied { reads.costingManagement(cashier.sessionId) }
        expectPermissionDenied { reads.users(cashier.sessionId) }
        expectPermissionDenied { reads.securityAudit(cashier.sessionId) }

        assertTrue(reads.users(owner.session.sessionId).any { it.loginName == "gate.cashier" })
        assertTrue(reads.securityAudit(owner.session.sessionId).any { it.actionCode == "permission_denied" })
    }

    @Test
    fun hybridDtos_haveNoCredentialRecoveryOrSessionMaterial() {
        val dtoTypes = listOf(
            BootstrapStatusDto::class.java,
            CurrentSessionDto::class.java,
            BusinessSettingsDto::class.java,
            EventHistoryDto::class.java,
            EventDetailDto::class.java,
            AccountLedgerDto::class.java,
            ProfitabilityReportDto::class.java,
            CostingManagementDto::class.java,
            UserAdministrationDto::class.java,
            AuditActorDto::class.java,
            SecurityAuditDto::class.java,
        )
        val forbidden = listOf("password", "passphrase", "credential", "salt", "verifier", "recovery", "sessionid", "sessionhandle")
        dtoTypes.forEach { type ->
            type.declaredFields.forEach { field ->
                val normalized = field.name.lowercase()
                assertTrue("Forbidden security field ${type.simpleName}.${field.name}", forbidden.none(normalized::contains))
            }
        }
    }

    private suspend fun expectPermissionDenied(block: suspend () -> Unit) {
        try {
            block()
        } catch (failure: SecurityFailure.PermissionDenied) {
            return
        }
        fail("Expected permission denial")
    }

    private companion object {
        const val OWNER_PASSWORD = "owner phase five secure phrase"
        const val CASHIER_PASSWORD = "cashier phase five secure phrase"
        const val OPERATOR_PASSWORD = "operator phase five secure phrase"
    }
}
