package com.saaf.milkshop

import android.content.pm.ApplicationInfo
import androidx.annotation.XmlRes
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.domain.AppPasswordPolicy
import com.saaf.milkshop.domain.AppPermission
import com.saaf.milkshop.domain.AppRole
import com.saaf.milkshop.domain.Argon2idPasswordHasher
import com.saaf.milkshop.domain.ConversionCostComponentInput
import com.saaf.milkshop.domain.CostingPolicyInput
import com.saaf.milkshop.domain.CostingPolicyService
import com.saaf.milkshop.domain.CreateUserInput
import com.saaf.milkshop.domain.DailyCloseService
import com.saaf.milkshop.domain.NativeSessionManager
import com.saaf.milkshop.domain.RolePermissions
import com.saaf.milkshop.domain.SecurityFailure
import com.saaf.milkshop.domain.UserSecurityService
import com.saaf.milkshop.domain.VerifiedDailyReopenInput
import java.math.BigDecimal
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.xmlpull.v1.XmlPullParser

@RunWith(AndroidJUnit4::class)
class Phase4SecurityCoverageTest {
    private val database = MilkShopDatabase.createInMemory()

    @After
    fun closeDatabase() = database.close()

    @Test
    fun roleMatrix_matchesEveryApprovedPermission() {
        val all = AppPermission.entries.toSet()
        val expected = mapOf(
            AppRole.OWNER to all,
            AppRole.ADMIN to all - AppPermission.RESTORE_BACKUP - AppPermission.MANAGE_PRIVILEGED_USERS,
            AppRole.MANAGER to setOf(
                AppPermission.VIEW_OPERATIONAL,
                AppPermission.VIEW_FULL_FINANCIAL,
                AppPermission.SALE_AND_COLLECTION,
                AppPermission.PURCHASE,
                AppPermission.SUPPLIER_PAYMENT,
                AppPermission.EXPENSE,
                AppPermission.SALARY_PAYMENT,
                AppPermission.PRODUCTION_AND_SPOILAGE,
                AppPermission.DELIVERY,
                AppPermission.MANAGE_MASTER_DATA,
                AppPermission.ATTENDANCE,
                AppPermission.CORRECTION,
                AppPermission.CASH_COUNT_AND_CLOSE,
            ),
            AppRole.CASHIER to setOf(
                AppPermission.VIEW_OPERATIONAL,
                AppPermission.SALE_AND_COLLECTION,
                AppPermission.EXPENSE,
                AppPermission.DELIVERY,
                AppPermission.CASH_COUNT_AND_CLOSE,
            ),
            AppRole.OPERATOR to setOf(
                AppPermission.VIEW_OPERATIONAL,
                AppPermission.PURCHASE,
                AppPermission.PRODUCTION_AND_SPOILAGE,
                AppPermission.DELIVERY,
                AppPermission.ATTENDANCE,
            ),
        )

        expected.forEach { (role, permissions) ->
            assertEquals("Unexpected permissions for ${role.wireValue}", permissions, RolePermissions.permissions(role))
        }
    }

    @Test
    fun loginAndPassphrasePolicy_normalizeUnicodeRejectDuplicatesAndKeepErrorsGeneric() = runBlocking {
        val service = UserSecurityService(database)
        val decomposed = "Cafe\u0301 owner secure phrase".toCharArray()
        val composed = "Café owner secure phrase".toCharArray()
        val owner = service.bootstrapOwner("  Shop.Owner  ", "Shop Owner", decomposed)
        assertEquals("shop.owner", owner.user.loginName)
        assertTrue(service.login("SHOP.OWNER", composed).sessionId.isNotBlank())

        assertSuspendFailure {
            service.beginUserEnrollment(
                owner.session.sessionId,
                CreateUserInput(" SHOP.OWNER ", "Duplicate", "cashier"),
            )
        }
        assertFailure { AppPasswordPolicy.validatePassphrase("too short".toCharArray(), "shop.owner") }
        assertFailure { AppPasswordPolicy.validatePassphrase("passwordpassword".toCharArray(), "shop.owner") }

        val known = expectSecurityFailure { service.login("shop.owner", WRONG_PASSWORD.toCharArray()) }
        val unknown = expectSecurityFailure { service.login("missing.user", WRONG_PASSWORD.toCharArray()) }
        val malformed = expectSecurityFailure { service.login("?", WRONG_PASSWORD.toCharArray()) }
        assertEquals(SecurityFailure.GENERIC_AUTH_FAILURE, known.message)
        assertEquals(known.message, unknown.message)
        assertEquals(known.message, malformed.message)
    }

    @Test
    fun successfulLogin_upgradesAnOlderApprovedArgon2Verifier() = runBlocking {
        val setup = UserSecurityService(database)
        val owner = setup.bootstrapOwner("upgrade.owner", "Upgrade Owner", OWNER_PASSWORD.toCharArray())
        val before = database.userCredentialDao().find(owner.user.userId)!!
        assertEquals(Argon2idPasswordHasher.MEMORY_KIB, before.memoryKib)
        assertEquals(Argon2idPasswordHasher.ITERATIONS, before.iterations)

        val strongerMemoryKib = Argon2idPasswordHasher.MEMORY_KIB + 1_024
        val strongerIterations = Argon2idPasswordHasher.ITERATIONS + 1
        val strongerHasher = Argon2idPasswordHasher(
            targetMemoryKib = strongerMemoryKib,
            targetIterations = strongerIterations,
            targetParallelism = 1,
        )
        val current = UserSecurityService(database, passwordHasher = strongerHasher)
        assertTrue(current.login("upgrade.owner", OWNER_PASSWORD.toCharArray()).sessionId.isNotBlank())
        val after = database.userCredentialDao().find(owner.user.userId)!!
        assertEquals(strongerMemoryKib, after.memoryKib)
        assertEquals(strongerIterations, after.iterations)
        assertEquals(Argon2idPasswordHasher.PARALLELISM, after.parallelism)
        assertNotEquals(before.salt.toList(), after.salt.toList())
    }

    @Test
    fun passwordLogin_throttlesLocksAndSuccessfulLoginClearsFailures() = runBlocking {
        var now = 1_000_000L
        val service = UserSecurityService(database, nowEpochMs = { now })
        val owner = service.bootstrapOwner("lock.owner", "Lock Owner", OWNER_PASSWORD.toCharArray())

        repeat(10) { index ->
            val failure = expectSecurityFailure {
                service.login("lock.owner", WRONG_PASSWORD.toCharArray())
            }
            assertEquals(SecurityFailure.GENERIC_AUTH_FAILURE, failure.message)
            val credential = database.userCredentialDao().find(owner.user.userId)!!
            assertEquals(index + 1, credential.failedAttemptCount)
            if (index in 4..8) now = credential.lockedUntilEpochMs!! + 1L
        }

        val locked = database.userCredentialDao().find(owner.user.userId)!!
        assertEquals(now + 1_800_000L, locked.lockedUntilEpochMs)
        expectSecurityFailure { service.login("lock.owner", OWNER_PASSWORD.toCharArray()) }
        now = locked.lockedUntilEpochMs!! + 1L
        assertTrue(service.login("lock.owner", OWNER_PASSWORD.toCharArray()).sessionId.isNotBlank())
        val cleared = database.userCredentialDao().find(owner.user.userId)!!
        assertEquals(0, cleared.failedAttemptCount)
        assertNull(cleared.lockedUntilEpochMs)
    }

    @Test
    fun ownerRecovery_hasIndependentThrottleAndRemainsAvailableDuringPasswordLockout() = runBlocking {
        var now = 2_000_000L
        val service = UserSecurityService(database, nowEpochMs = { now })
        val owner = service.bootstrapOwner("recoverable.owner", "Recoverable Owner", OWNER_PASSWORD.toCharArray())
        repeat(5) {
            expectSecurityFailure { service.login("recoverable.owner", WRONG_PASSWORD.toCharArray()) }
        }
        assertTrue(database.userCredentialDao().find(owner.user.userId)!!.lockedUntilEpochMs!! > now)

        val recovered = service.recoverOwner(
            "recoverable.owner",
            owner.recoveryCode,
            RECOVERED_PASSWORD.toCharArray(),
        )
        assertTrue(recovered.first.sessionId.isNotBlank())
        assertNotEquals(owner.recoveryCode, recovered.second)

        repeat(5) {
            expectSecurityFailure {
                service.recoverOwner("recoverable.owner", "incorrect-recovery-code", OTHER_PASSWORD.toCharArray())
            }
        }
        val saved = database.savedRecoveryCodeDao().current(owner.user.userId)!!
        assertEquals(5, saved.failedAttemptCount)
        assertTrue(saved.lockedUntilEpochMs!! > now)
        now = saved.lockedUntilEpochMs!! + 1L
        assertTrue(
            service.recoverOwner("recoverable.owner", recovered.second, OTHER_PASSWORD.toCharArray())
                .first.sessionId.isNotBlank()
        )
    }

    @Test
    fun sessions_endOnLogoutBackgroundTimeoutOverallTimeoutAndProcessRestart() = runBlocking {
        var now = 3_000_000L
        val sessions = NativeSessionManager { now }
        val service = UserSecurityService(database, sessions = sessions, nowEpochMs = { now })
        val owner = service.bootstrapOwner("session.owner", "Session Owner", OWNER_PASSWORD.toCharArray())

        service.logout(owner.session.sessionId)
        expectSecurityFailure {
            service.requirePermission(owner.session.sessionId, AppPermission.VIEW_OPERATIONAL)
        }

        val background = service.login("session.owner", OWNER_PASSWORD.toCharArray())
        sessions.onBackground()
        now += NativeSessionManager.BACKGROUND_TIMEOUT_MS
        sessions.onForeground()
        expectSecurityFailure { service.requirePermission(background.sessionId, AppPermission.VIEW_OPERATIONAL) }

        val beforeRestart = service.login("session.owner", OWNER_PASSWORD.toCharArray())
        val restarted = UserSecurityService(
            database,
            sessions = NativeSessionManager { now },
            nowEpochMs = { now },
        )
        expectSecurityFailure {
            restarted.requirePermission(beforeRestart.sessionId, AppPermission.VIEW_OPERATIONAL)
        }
        assertTrue(restarted.login("session.owner", OWNER_PASSWORD.toCharArray()).sessionId.isNotBlank())

        now = 0L
        val rolling = NativeSessionManager { now }
        val shift = rolling.start(owner.user.userId)
        while (now + NativeSessionManager.INACTIVITY_TIMEOUT_MS - 1L < NativeSessionManager.OVERALL_TIMEOUT_MS) {
            now += NativeSessionManager.INACTIVITY_TIMEOUT_MS - 1L
            rolling.require(shift.sessionId)
        }
        now = NativeSessionManager.OVERALL_TIMEOUT_MS
        assertFailure { rolling.require(shift.sessionId) }
    }

    @Test
    fun sensitiveActionsRequireRecentAuthentication_andRestrictedUsersCannotBypassServices() = runBlocking {
        var now = 4_000_000L
        val sessions = NativeSessionManager { now }
        val security = UserSecurityService(database, sessions = sessions, nowEpochMs = { now })
        val owner = security.bootstrapOwner("policy.owner", "Policy Owner", OWNER_PASSWORD.toCharArray())
        val pending = security.beginUserEnrollment(
            owner.session.sessionId,
            CreateUserInput("policy.cashier", "Policy Cashier", "cashier"),
        )
        security.completeUserEnrollment(pending.enrollmentToken, CASHIER_PASSWORD.toCharArray())
        val cashier = security.login("policy.cashier", CASHIER_PASSWORD.toCharArray())

        now += NativeSessionManager.RECENT_AUTH_TIMEOUT_MS
        val stale = expectSecurityFailure {
            CostingPolicyService(database, security).approve(owner.session.sessionId, policy("2026-09"))
        }
        assertTrue(stale is SecurityFailure.RecentAuthenticationRequired)
        assertNull(database.costingPolicyDao().activeForMonth("2026-09"))

        expectSecurityFailure { security.reauthenticate(owner.session.sessionId, WRONG_PASSWORD.toCharArray()) }
        security.reauthenticate(owner.session.sessionId, OWNER_PASSWORD.toCharArray())
        CostingPolicyService(database, security).approve(owner.session.sessionId, policy("2026-09"))

        val denied = expectSecurityFailure {
            CostingPolicyService(database, security).approve(cashier.sessionId, policy("2026-10"))
        }
        assertTrue(denied is SecurityFailure.PermissionDenied)
        expectSecurityFailure {
            DailyCloseService(database).reopenAuthenticated(
                cashier.sessionId,
                security,
                VerifiedDailyReopenInput("2026-08-28", "Attempted bypass"),
            )
        }
        expectSecurityFailure { security.listUsers(cashier.sessionId) }
        expectSecurityFailure { security.requirePermission("invented-session", AppPermission.VIEW_OPERATIONAL) }
        assertNull(database.costingPolicyDao().activeForMonth("2026-10"))

        val deniedAudits = database.securityAuditDao().all().filter {
            it.action == UserSecurityService.ACTION_PERMISSION_DENIED && it.actorUserId == pending.user.userId
        }
        assertTrue(deniedAudits.size >= 3)
        assertTrue(deniedAudits.all { it.actorNameSnapshot == "Policy Cashier" && it.actorRoleSnapshot == "cashier" })
    }

    @Test
    fun disableRoleChangeAndCredentialReset_revokeEveryActiveSessionAndAreAudited() = runBlocking {
        val security = UserSecurityService(database)
        val owner = security.bootstrapOwner("audit.owner", "Audit Owner", OWNER_PASSWORD.toCharArray())
        val pending = security.beginUserEnrollment(
            owner.session.sessionId,
            CreateUserInput("audit.cashier", "Audit Cashier", "cashier"),
        )
        security.completeUserEnrollment(pending.enrollmentToken, CASHIER_PASSWORD.toCharArray())
        val first = security.login("audit.cashier", CASHIER_PASSWORD.toCharArray())
        val second = security.login("audit.cashier", CASHIER_PASSWORD.toCharArray())

        security.setUserEnabled(owner.session.sessionId, pending.user.userId, false)
        expectSecurityFailure { security.requirePermission(first.sessionId, AppPermission.VIEW_OPERATIONAL) }
        expectSecurityFailure { security.requirePermission(second.sessionId, AppPermission.VIEW_OPERATIONAL) }

        security.setUserEnabled(owner.session.sessionId, pending.user.userId, true)
        val beforeRoleChange = security.login("audit.cashier", CASHIER_PASSWORD.toCharArray())
        security.changeRole(owner.session.sessionId, pending.user.userId, "operator")
        expectSecurityFailure { security.requirePermission(beforeRoleChange.sessionId, AppPermission.VIEW_OPERATIONAL) }

        val beforeReset = security.login("audit.cashier", CASHIER_PASSWORD.toCharArray())
        security.beginCredentialReset(owner.session.sessionId, pending.user.userId)
        expectSecurityFailure { security.requirePermission(beforeReset.sessionId, AppPermission.VIEW_OPERATIONAL) }

        val actions = database.securityAuditDao().all().filter { it.targetUserId == pending.user.userId }.map { it.action }
        assertTrue(UserSecurityService.ACTION_CREATE_USER in actions)
        assertTrue(UserSecurityService.ACTION_DISABLE_USER in actions)
        assertTrue(UserSecurityService.ACTION_ENABLE_USER in actions)
        assertTrue(UserSecurityService.ACTION_CHANGE_ROLE in actions)
        assertTrue(UserSecurityService.ACTION_RESET_CREDENTIAL in actions)
    }

    @Test
    fun androidManagedBackup_excludesDatabasePreferencesAndPrivateFiles() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val appInfo = context.packageManager.getApplicationInfo(context.packageName, 0)
        assertEquals(0, appInfo.flags and ApplicationInfo.FLAG_ALLOW_BACKUP)

        val requiredDomains = setOf("root", "file", "database", "sharedpref", "external")
        assertEquals(requiredDomains, excludedDomains(R.xml.backup_rules))
        assertEquals(requiredDomains, excludedDomains(R.xml.data_extraction_rules))
    }

    private fun policy(month: String) = CostingPolicyInput(
        calendarMonth = month,
        normalCapacityKg = BigDecimal("100"),
        reviewReason = "monthly",
        components = listOf(
            ConversionCostComponentInput("culture", 1_000, BigDecimal.ONE),
            ConversionCostComponentInput("packaging", 0, BigDecimal.ONE),
            ConversionCostComponentInput("variable_energy", 0, BigDecimal.ONE),
            ConversionCostComponentInput("direct_labor", 0, BigDecimal.ONE),
            ConversionCostComponentInput("fixed_production_overhead", 0, BigDecimal("100")),
        ),
    )

    private suspend fun expectSecurityFailure(block: suspend () -> Unit): SecurityFailure {
        try {
            block()
        } catch (failure: SecurityFailure) {
            return failure
        }
        fail("Expected a security failure")
        throw AssertionError("unreachable")
    }

    private suspend fun assertSuspendFailure(block: suspend () -> Unit) {
        assertTrue(runCatching { block() }.isFailure)
    }

    private fun assertFailure(block: () -> Unit) {
        assertTrue(runCatching { block() }.isFailure)
    }

    private fun excludedDomains(@XmlRes resourceId: Int): Set<String> {
        val parser = ApplicationProvider.getApplicationContext<android.content.Context>().resources.getXml(resourceId)
        return buildSet {
            parser.use {
                while (it.eventType != XmlPullParser.END_DOCUMENT) {
                    if (it.eventType == XmlPullParser.START_TAG && it.name == "exclude" && it.getAttributeValue(null, "path") == ".") {
                        it.getAttributeValue(null, "domain")?.let(::add)
                    }
                    it.next()
                }
            }
        }
    }

    private companion object {
        const val OWNER_PASSWORD = "correct horse battery staple"
        const val CASHIER_PASSWORD = "cashier mango counter evening"
        const val WRONG_PASSWORD = "incorrect secure phrase value"
        const val RECOVERED_PASSWORD = "recovered owner passphrase safely"
        const val OTHER_PASSWORD = "another secure passphrase here"
    }
}
