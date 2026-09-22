package com.saaf.milkshop

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.saaf.milkshop.application.owner.NativeOwnerUsersRolesFacade
import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.domain.AppRole
import com.saaf.milkshop.domain.CreateUserInput
import com.saaf.milkshop.domain.SecurityFailure
import com.saaf.milkshop.domain.UserSecurityService
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NativeOwnerUsersRolesFacadeTest {
    private lateinit var database: MilkShopDatabase
    private lateinit var security: UserSecurityService
    private lateinit var facade: NativeOwnerUsersRolesFacade
    private lateinit var ownerSession: String
    private lateinit var cashierSession: String

    @Before
    fun setUp() = runBlocking {
        database = MilkShopDatabase.createInMemory()
        security = UserSecurityService(database)
        facade = NativeOwnerUsersRolesFacade(database, security)
        ownerSession = security.bootstrapOwner(
            "owner.users.roles",
            "Users Roles Owner",
            OWNER_PASSPHRASE.toCharArray(),
        ).session.sessionId
        val pending = security.beginUserEnrollment(
            ownerSession,
            CreateUserInput(
                loginName = "users.roles.cashier",
                displayName = "Users Roles Cashier",
                role = AppRole.CASHIER.wireValue,
            ),
        )
        security.completeUserEnrollment(pending.enrollmentToken, CASHIER_PASSPHRASE.toCharArray())
        cashierSession = security.login("users.roles.cashier", CASHIER_PASSPHRASE.toCharArray()).sessionId
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun loadUsersListsOwnerAndCashier() = runBlocking {
        val snapshot = facade.loadUsers(ownerSession)
        assertTrue(snapshot.users.any { it.loginName == "owner.users.roles" })
        assertTrue(snapshot.users.any { it.loginName == "users.roles.cashier" })
        val cashier = snapshot.users.first { it.loginName == "users.roles.cashier" }
        assertEquals("Users Roles Cashier", cashier.displayName)
        assertEquals("Cashier", cashier.roleLabel)
        assertEquals(AppRole.CASHIER.wireValue, cashier.roleWire)
        assertTrue(cashier.enabled)
    }

    @Test
    fun loadDetailReturnsAccountFacts() = runBlocking {
        val snapshot = facade.loadUsers(ownerSession)
        val cashier = snapshot.users.first { it.loginName == "users.roles.cashier" }
        val detail = facade.loadDetail(ownerSession, cashier.userId)
        assertEquals(cashier.displayName, detail.row.displayName)
        assertEquals("Not linked", detail.employeeAccountIdLabel)
        assertTrue(detail.createdAt.isNotBlank())
    }

    @Test
    fun loadRejectsNonOwner() = runBlocking {
        val rejected = runCatching { facade.loadUsers(cashierSession) }
        assertTrue(rejected.isFailure)
    }

    @Test
    fun beginAndCompleteEnrollCreatesCashierLikeUser() = runBlocking {
        val pending = facade.beginEnroll(
            ownerSession,
            CreateUserInput(
                loginName = "users.roles.newcashier",
                displayName = "New Cashier",
                role = AppRole.CASHIER.wireValue,
            ),
        )
        assertEquals("New Cashier", pending.displayName)
        assertTrue(pending.enrollmentToken.isNotBlank())

        val secret = NEW_CASHIER_PASSPHRASE.toCharArray()
        val completed = facade.completeEnroll(pending.enrollmentToken, secret)
        assertTrue(secret.all { it == '\u0000' })
        assertEquals("users.roles.newcashier", completed.loginName)
        assertEquals(AppRole.CASHIER.wireValue, completed.roleWire)
        assertTrue(completed.enabled)
        assertFalse(completed.setupRequired)

        val snapshot = facade.loadUsers(ownerSession)
        assertTrue(snapshot.users.any { it.loginName == "users.roles.newcashier" })
    }

    @Test
    fun reauthenticateWipesPassphraseAndAllowsSensitiveWrites() = runBlocking {
        val snapshot = facade.loadUsers(ownerSession)
        val cashier = snapshot.users.first { it.loginName == "users.roles.cashier" }

        val secret = OWNER_PASSPHRASE.toCharArray()
        facade.reauthenticate(ownerSession, secret)
        assertTrue(secret.all { it == '\u0000' })

        facade.setEnabled(ownerSession, cashier.userId, false)
        var detail = facade.loadDetail(ownerSession, cashier.userId)
        assertFalse(detail.row.enabled)
        assertEquals("Disabled", detail.row.statusLabel)

        facade.setEnabled(ownerSession, cashier.userId, true)
        detail = facade.loadDetail(ownerSession, cashier.userId)
        assertTrue(detail.row.enabled)

        facade.changeRole(ownerSession, cashier.userId, AppRole.OPERATOR.wireValue)
        detail = facade.loadDetail(ownerSession, cashier.userId)
        assertEquals(AppRole.OPERATOR.wireValue, detail.row.roleWire)
        assertEquals("Operator", detail.row.roleLabel)
    }

    @Test
    fun setEnabledRejectsDisablingSelf() = runBlocking {
        val owner = facade.loadUsers(ownerSession).users.first { it.loginName == "owner.users.roles" }
        facade.reauthenticate(ownerSession, OWNER_PASSPHRASE.toCharArray())
        val rejected = runCatching {
            facade.setEnabled(ownerSession, owner.userId, false)
        }.exceptionOrNull()
        assertTrue(rejected is SecurityFailure.Validation)
        assertTrue(rejected!!.message!!.contains("cannot disable", ignoreCase = true))
    }

    @Test
    fun wrongReauthPassphraseIsWipedAndFails() = runBlocking {
        val wrong = "definitely wrong passphrase".toCharArray()
        val rejected = runCatching {
            facade.reauthenticate(ownerSession, wrong)
        }.exceptionOrNull()
        assertTrue(wrong.all { it == '\u0000' })
        assertTrue(rejected is SecurityFailure.Authentication)
    }

    companion object {
        private const val OWNER_PASSPHRASE = "owner users roles passphrase 2026!!"
        private const val CASHIER_PASSPHRASE = "users roles cashier passphrase 2026!!"
        private const val NEW_CASHIER_PASSPHRASE = "new cashier enroll passphrase 2026!!"
    }
}
