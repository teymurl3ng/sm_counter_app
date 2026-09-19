package com.saaf.milkshop

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.saaf.milkshop.application.authentication.NativeAuthenticationFacade
import com.saaf.milkshop.data.AppUserEntity
import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.domain.AppRole
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
class NativeAuthenticationFacadeTest {
    private lateinit var database: MilkShopDatabase
    private lateinit var facade: NativeAuthenticationFacade

    @Before
    fun setUp() {
        database = MilkShopDatabase.createInMemory()
        facade = NativeAuthenticationFacade(database, UserSecurityService(database))
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun ownerBootstrapAndFullPassphraseSignInReturnTypedMemorySessions() = runBlocking {
        assertTrue(facade.startup().needsOwnerBootstrap)
        val bootstrapSecret = OWNER_PASSPHRASE.toCharArray()

        val bootstrap = facade.bootstrapOwner("native.owner", "Native Owner", bootstrapSecret)

        assertTrue(bootstrapSecret.all { it == '\u0000' })
        assertEquals(AppRole.OWNER, bootstrap.session.identity.role)
        assertTrue(bootstrap.oneTimeRecoveryCode.isNotBlank())
        assertTrue(bootstrap.session.sessionHandle.isNotBlank())

        val startup = facade.startup()
        assertFalse(startup.needsOwnerBootstrap)
        assertEquals(listOf("native.owner"), startup.accounts.map { it.loginName })

        val wrong = "wrong full passphrase".toCharArray()
        val rejected = runCatching { facade.signIn(startup.accounts.single(), wrong) }.exceptionOrNull()
        assertTrue(rejected is SecurityFailure.Authentication)
        assertTrue(wrong.all { it == '\u0000' })

        val valid = OWNER_PASSPHRASE.toCharArray()
        val signedIn = facade.signIn(startup.accounts.single(), valid)
        assertTrue(valid.all { it == '\u0000' })
        assertEquals("Native Owner", signedIn.identity.displayName)
        assertEquals(AppRole.OWNER, signedIn.identity.role)
    }

    @Test
    fun startupExposesOnlyActiveAccountsWithCompletedCredentialSetup() = runBlocking {
        facade.bootstrapOwner("visible.owner", "Visible Owner", OWNER_PASSPHRASE.toCharArray())
        val now = "2026-09-19T00:00:00Z"
        database.appUserDao().insert(
            AppUserEntity(
                loginName = "pending.cashier",
                displayName = "Pending Cashier",
                role = AppRole.CASHIER.wireValue,
                credentialSetupRequired = true,
                createdAt = now,
                updatedAt = now,
            ),
        )
        database.appUserDao().insert(
            AppUserEntity(
                loginName = "disabled.cashier",
                displayName = "Disabled Cashier",
                role = AppRole.CASHIER.wireValue,
                status = "disabled",
                createdAt = now,
                updatedAt = now,
            ),
        )

        val accounts = facade.startup().accounts

        assertEquals(listOf("visible.owner"), accounts.map { it.loginName })
    }

    private companion object {
        const val OWNER_PASSPHRASE = "native owner private passphrase 2026"
    }
}
