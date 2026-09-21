package com.saaf.milkshop

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.saaf.milkshop.application.owner.NativeOwnerBackupFacade
import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.domain.AppRole
import com.saaf.milkshop.domain.CreateUserInput
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
class NativeOwnerBackupFacadeTest {
    private lateinit var database: MilkShopDatabase
    private lateinit var security: UserSecurityService
    private lateinit var facade: NativeOwnerBackupFacade
    private lateinit var ownerSession: String
    private lateinit var cashierSession: String

    @Before
    fun setUp() = runBlocking {
        database = MilkShopDatabase.createInMemory()
        security = UserSecurityService(database)
        facade = NativeOwnerBackupFacade(
            database = database,
            security = security,
            cloudBackupSettings = null,
        )
        ownerSession = security.bootstrapOwner(
            "owner.backup",
            "Backup Owner",
            OWNER_PASSPHRASE.toCharArray(),
        ).session.sessionId
        val pending = security.beginUserEnrollment(
            ownerSession,
            CreateUserInput(
                loginName = "backup.cashier",
                displayName = "Backup Cashier",
                role = AppRole.CASHIER.wireValue,
            ),
        )
        security.completeUserEnrollment(pending.enrollmentToken, CASHIER_PASSPHRASE.toCharArray())
        cashierSession = security.login("backup.cashier", CASHIER_PASSPHRASE.toCharArray()).sessionId
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun loadWithNullCloudSettingsReportsNotConfigured() = runBlocking {
        val snapshot = facade.load(ownerSession)
        assertFalse(snapshot.configured)
        assertFalse(snapshot.enabled)
        assertEquals("Backup not configured", snapshot.statusHeadline)
        assertTrue(snapshot.scheduleDescription.contains("not configured"))
    }

    @Test
    fun loadRejectsNonOwner() = runBlocking {
        val rejected = runCatching { facade.load(cashierSession) }
        assertTrue(rejected.isFailure)
    }

    companion object {
        private const val OWNER_PASSPHRASE = "owner backup passphrase 2026!!"
        private const val CASHIER_PASSPHRASE = "backup cashier passphrase 2026!!"
    }
}
