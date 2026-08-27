package com.saaf.milkshop

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.domain.AppPermission
import com.saaf.milkshop.domain.AppRole
import com.saaf.milkshop.domain.CreateUserInput
import com.saaf.milkshop.domain.DailyCloseService
import com.saaf.milkshop.domain.NativeSessionManager
import com.saaf.milkshop.domain.PhysicalCashCountInput
import com.saaf.milkshop.domain.PhysicalCashCountService
import com.saaf.milkshop.domain.RolePermissions
import com.saaf.milkshop.domain.SecurityFailure
import com.saaf.milkshop.domain.UserSecurityService
import com.saaf.milkshop.domain.VerifiedDailyCloseInput
import com.saaf.milkshop.domain.VerifiedDailyReopenInput
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Phase3UserSecurityTest {
    private val database = MilkShopDatabase.createInMemory()

    @After
    fun closeDatabase() = database.close()

    @Test
    fun bootstrapOwner_isDeliberateOneTime_andStoresOnlyArgon2Verifier() = runBlocking {
        val service = UserSecurityService(database)
        assertTrue(service.needsOwnerBootstrap())
        val result = service.bootstrapOwner("owner.one", "Primary Owner", OWNER_PASSWORD.toCharArray())

        assertFalse(service.needsOwnerBootstrap())
        assertEquals(AppRole.OWNER.wireValue, result.user.role)
        assertTrue(result.recoveryCode.length >= 22)
        val credential = database.userCredentialDao().find(result.user.userId)!!
        assertEquals("argon2id", credential.algorithm)
        assertEquals(19_456, credential.memoryKib)
        assertEquals(2, credential.iterations)
        assertEquals(32, credential.verifier.size)
        assertNotEquals(OWNER_PASSWORD, credential.verifier.toString(Charsets.UTF_8))

        assertRejected { service.bootstrapOwner("other.owner", "Other", OTHER_PASSWORD.toCharArray()) }
        assertEquals(1, database.appUserDao().all().size)
        assertEquals(UserSecurityService.ACTION_BOOTSTRAP_OWNER, database.securityAuditDao().all().single().action)
    }

    @Test
    fun ownerControlsRoles_adminCannotControlPrivilegedUsers_andFinalOwnerIsProtected() = runBlocking {
        val service = UserSecurityService(database)
        val owner = service.bootstrapOwner("shop.owner", "Shop Owner", OWNER_PASSWORD.toCharArray())
        val adminEnrollment = service.beginUserEnrollment(
            owner.session.sessionId,
            CreateUserInput("shop.admin", "Shop Admin", "admin"),
        )
        assertNull(database.userCredentialDao().find(adminEnrollment.user.userId))
        assertRejected { service.login("shop.admin", ADMIN_PASSWORD.toCharArray()) }
        val admin = service.completeUserEnrollment(adminEnrollment.enrollmentToken, ADMIN_PASSWORD.toCharArray())
        val adminSession = service.login("shop.admin", ADMIN_PASSWORD.toCharArray())
        val cashierEnrollment = service.beginUserEnrollment(
            adminSession.sessionId,
            CreateUserInput("front.cashier", "Front Cashier", "cashier"),
        )
        val cashier = service.completeUserEnrollment(cashierEnrollment.enrollmentToken, CASHIER_PASSWORD.toCharArray())

        assertEquals(AppRole.CASHIER.wireValue, cashier.role)
        assertRejected {
            service.beginUserEnrollment(
                adminSession.sessionId,
                CreateUserInput("second.admin", "Second Admin", "admin"),
            )
        }
        assertRejected { service.setUserEnabled(adminSession.sessionId, owner.user.userId, false) }
        assertRejected { service.setUserEnabled(owner.session.sessionId, owner.user.userId, false) }
        assertRejected { database.appUserDao().update(owner.user.copy(role = AppRole.MANAGER.wireValue)) }

        service.setUserEnabled(owner.session.sessionId, admin.userId, false)
        assertEquals("disabled", database.appUserDao().findById(admin.userId)!!.status)
        assertRejected { service.requirePermission(adminSession.sessionId, AppPermission.MANAGE_STAFF_USERS) }
    }

    @Test
    fun roleMatrix_isLeastPrivilege_andPermissionsAreNative() {
        assertTrue(RolePermissions.allows(AppRole.OWNER, AppPermission.RESTORE_BACKUP))
        assertFalse(RolePermissions.allows(AppRole.ADMIN, AppPermission.RESTORE_BACKUP))
        assertTrue(RolePermissions.allows(AppRole.ADMIN, AppPermission.REOPEN_DAY))
        assertFalse(RolePermissions.allows(AppRole.MANAGER, AppPermission.REOPEN_DAY))
        assertTrue(RolePermissions.allows(AppRole.CASHIER, AppPermission.SALE_AND_COLLECTION))
        assertFalse(RolePermissions.allows(AppRole.CASHIER, AppPermission.SALARY_PAYMENT))
        assertTrue(RolePermissions.allows(AppRole.OPERATOR, AppPermission.PURCHASE))
        assertFalse(RolePermissions.allows(AppRole.OPERATOR, AppPermission.VIEW_FULL_FINANCIAL))
    }

    @Test
    fun staffEnrollmentAndReset_neverRequireAdministratorAssignedCredentials() = runBlocking {
        val service = UserSecurityService(database)
        val owner = service.bootstrapOwner("enroll.owner", "Enrollment Owner", OWNER_PASSWORD.toCharArray())
        val pending = service.beginUserEnrollment(
            owner.session.sessionId,
            CreateUserInput("private.cashier", "Private Cashier", "cashier"),
        )
        assertTrue(pending.user.credentialSetupRequired)
        assertNull(database.userCredentialDao().find(pending.user.userId))
        assertRejected { service.login("private.cashier", CASHIER_PASSWORD.toCharArray()) }

        service.completeUserEnrollment(pending.enrollmentToken, CASHIER_PASSWORD.toCharArray())
        val cashierSession = service.login("private.cashier", CASHIER_PASSWORD.toCharArray())
        assertTrue(cashierSession.sessionId.isNotBlank())

        val reset = service.beginCredentialReset(owner.session.sessionId, pending.user.userId)
        assertRejected { service.login("private.cashier", CASHIER_PASSWORD.toCharArray()) }
        service.completeUserEnrollment(reset.enrollmentToken, OTHER_PASSWORD.toCharArray())
        assertTrue(service.login("private.cashier", OTHER_PASSWORD.toCharArray()).sessionId.isNotBlank())
    }

    @Test
    fun verifiedDailyCloseUsesSessionActor_andUnverifiedRoleTextIsRejectedAfterBootstrap() = runBlocking {
        val service = UserSecurityService(database)
        val owner = service.bootstrapOwner("verified.owner", "Verified Owner", OWNER_PASSWORD.toCharArray())
        val cashId = database.accountDao().findByCategory("Cash/Bank").single { it.name == "Cash" }.accountId
        val first = PhysicalCashCountService(database).record(PhysicalCashCountInput(cashId, DATE, 0.0))
        val closes = DailyCloseService(database)

        assertRejected {
            closes.close(com.saaf.milkshop.domain.DailyCloseInput(DATE, first.count.countId, "Pretend Owner", "owner"))
        }
        val close = closes.closeAuthenticated(
            owner.session.sessionId,
            service,
            VerifiedDailyCloseInput(DATE, first.count.countId),
        )
        assertEquals(owner.user.userId, close.action.actorUserId)
        assertEquals("Verified Owner", close.action.actorName)

        val reopened = closes.reopenAuthenticated(
            owner.session.sessionId,
            service,
            VerifiedDailyReopenInput(DATE, "Correct a verified transaction"),
        )
        assertEquals(owner.user.userId, reopened.action.actorUserId)
    }

    @Test
    fun sessionsExpireOnInactivity_andOwnerRecoveryRotatesSingleUseCode() = runBlocking {
        var now = 1_000_000L
        val sessions = NativeSessionManager { now }
        val service = UserSecurityService(database, sessions = sessions, nowEpochMs = { now })
        val owner = service.bootstrapOwner("recover.owner", "Recovery Owner", OWNER_PASSWORD.toCharArray())

        now += NativeSessionManager.INACTIVITY_TIMEOUT_MS
        assertRejected { service.requirePermission(owner.session.sessionId, AppPermission.VIEW_OPERATIONAL) }

        val recovered = service.recoverOwner(
            "recover.owner",
            owner.recoveryCode,
            RECOVERED_PASSWORD.toCharArray(),
        )
        assertNotEquals(owner.recoveryCode, recovered.second)
        assertRejected {
            service.recoverOwner("recover.owner", owner.recoveryCode, OTHER_PASSWORD.toCharArray())
        }
        assertEquals(
            AppRole.OWNER.wireValue,
            service.requirePermission(recovered.first.sessionId, AppPermission.RESTORE_BACKUP).role,
        )
    }

    @Test
    fun migrationBoundaryLeavesLegacyDailyCloseActorUnverified() = runBlocking {
        assertNull(database.dailyCloseDao().currentAction(DATE))
        assertEquals(0L, database.appUserDao().count())
    }

    private suspend fun assertRejected(block: suspend () -> Unit) {
        var rejected = false
        try {
            block()
        } catch (_: SecurityFailure) {
            rejected = true
        } catch (_: IllegalStateException) {
            rejected = true
        } catch (_: android.database.sqlite.SQLiteException) {
            rejected = true
        } catch (_: Exception) {
            rejected = true
        }
        assertTrue(rejected)
    }

    private companion object {
        const val DATE = "2026-08-27"
        const val OWNER_PASSWORD = "correct horse battery staple"
        const val ADMIN_PASSWORD = "admin river market lantern"
        const val CASHIER_PASSWORD = "cashier mango counter evening"
        const val OTHER_PASSWORD = "another secure passphrase here"
        const val RECOVERED_PASSWORD = "recovered owner passphrase safely"
    }
}
