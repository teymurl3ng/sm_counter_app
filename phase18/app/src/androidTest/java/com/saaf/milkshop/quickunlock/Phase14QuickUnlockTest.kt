package com.saaf.milkshop.quickunlock

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.core.app.ApplicationProvider
import com.saaf.milkshop.application.bridge.mapBridgeFailure
import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.domain.NativeSessionManager
import com.saaf.milkshop.domain.SecurityFailure
import com.saaf.milkshop.domain.UserSecurityService
import java.util.Base64
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Phase14QuickUnlockTest {
    private lateinit var database: MilkShopDatabase
    private lateinit var sessions: NativeSessionManager
    private lateinit var security: UserSecurityService
    private lateinit var store: MemoryQuickUnlockStore
    private lateinit var gate: FakeQuickUnlockDeviceGate
    private lateinit var quickUnlock: QuickUnlockService
    private var now = 1_800_000_000_000L

    @Before
    fun setUp() {
        database = MilkShopDatabase.createInMemory()
        sessions = NativeSessionManager { now }
        security = UserSecurityService(database, sessions = sessions, nowEpochMs = { now })
        store = MemoryQuickUnlockStore()
        gate = FakeQuickUnlockDeviceGate()
        quickUnlock = QuickUnlockService(database, security, store, gate, nowEpochMs = { now })
    }

    @After
    fun tearDown() {
        quickUnlock.close()
        database.close()
    }

    @Test
    fun bridgePreservesQuickUnlockFailureMeanings() {
        assertEquals("user_cancelled", mapBridgeFailure(QuickUnlockFailure.Cancelled()).code)
        assertEquals("quick_unlock_unavailable", mapBridgeFailure(QuickUnlockFailure.Unavailable()).code)
        assertEquals("quick_unlock_invalidated", mapBridgeFailure(QuickUnlockFailure.Invalidated()).code)
        assertEquals("rate_limited", mapBridgeFailure(QuickUnlockFailure.RateLimited()).code)
        assertEquals("authentication_failed", mapBridgeFailure(QuickUnlockFailure.Authentication()).code)
    }

    @Test
    fun androidEncryptedRecordStoreRoundTripsOnThisPhone() {
        val suffix = System.nanoTime().toString()
        val store = AndroidQuickUnlockRecordStore(
            context = ApplicationProvider.getApplicationContext(),
            protector = AndroidQuickUnlockRecordProtector("saaf_milk_quick_unlock_test_$suffix"),
            directoryName = "quick_unlock_test_$suffix",
        )
        val record = QuickUnlockRecord(
            userId = 42L,
            loginName = "storage.test",
            displayName = "Storage Test",
            securityBindingBase64 = "binding",
            deviceKeyId = null,
            deviceNonceBase64 = null,
            deviceCiphertextBase64 = null,
            deviceTokenHashBase64 = null,
            biometricCanaryCreated = false,
            pinSaltBase64 = "salt",
            pinVerifierBase64 = "verifier",
        )
        try {
            store.write(listOf(record))
            assertEquals(record, store.read().single())
        } finally {
            store.clear()
        }
    }

    @Test
    fun pinEnrollmentSurvivesServiceRecreationButNotASeparatePhone() = runBlocking {
        val owner = owner()
        quickUnlock.enroll(owner.session.sessionId, PASSPHRASE.toCharArray(), false, PIN.toCharArray())
        sessions.logout(owner.session.sessionId)

        val recreated = QuickUnlockService(database, security, store, gate, nowEpochMs = { now })
        val quickSession = recreated.login(owner.user.loginName, QuickUnlockMethod.PIN, PIN.toCharArray())
        assertEquals(owner.user.userId, quickSession.userId)
        assertEquals(owner.user.loginName, recreated.status().accounts.single().loginName)

        val separatePhone = QuickUnlockService(database, security, MemoryQuickUnlockStore(), FakeQuickUnlockDeviceGate(), nowEpochMs = { now })
        assertFailure<QuickUnlockFailure.Invalidated> {
            separatePhone.login(owner.user.loginName, QuickUnlockMethod.PIN, PIN.toCharArray())
        }
        separatePhone.close()
        recreated.close()
    }

    @Test
    fun deviceAuthenticationIsStrongButPinCannotApproveDestructiveWork() = runBlocking {
        val owner = owner()
        quickUnlock.enroll(owner.session.sessionId, PASSPHRASE.toCharArray(), true, PIN.toCharArray())
        sessions.logout(owner.session.sessionId)

        val pinSession = quickUnlock.login(owner.user.loginName, QuickUnlockMethod.PIN, PIN.toCharArray())
        sessions.requireRecentAuthentication(pinSession.sessionId, allowQuickPin = true)
        assertFailure<SecurityFailure.RecentAuthenticationRequired> {
            sessions.requireRecentAuthentication(pinSession.sessionId)
        }

        val deviceSession = quickUnlock.login(owner.user.loginName, QuickUnlockMethod.DEVICE, null)
        sessions.requireRecentAuthentication(deviceSession.sessionId)
        assertNotEquals(pinSession.sessionId, deviceSession.sessionId)
    }

    @Test
    fun pinFailuresDelayThenRequireAFullPassphraseToReset() = runBlocking {
        val owner = owner()
        quickUnlock.enroll(owner.session.sessionId, PASSPHRASE.toCharArray(), false, PIN.toCharArray())
        sessions.logout(owner.session.sessionId)

        repeat(2) {
            assertFailure<QuickUnlockFailure.Authentication> {
                quickUnlock.login(owner.user.loginName, QuickUnlockMethod.PIN, WRONG_PIN.toCharArray())
            }
        }
        assertFailure<QuickUnlockFailure.RateLimited> {
            quickUnlock.login(owner.user.loginName, QuickUnlockMethod.PIN, WRONG_PIN.toCharArray())
        }
        now += 30_001
        assertFailure<QuickUnlockFailure.RateLimited> {
            quickUnlock.login(owner.user.loginName, QuickUnlockMethod.PIN, WRONG_PIN.toCharArray())
        }
        now += 120_001
        assertFailure<QuickUnlockFailure.RateLimited> {
            quickUnlock.login(owner.user.loginName, QuickUnlockMethod.PIN, WRONG_PIN.toCharArray())
        }
        assertFailure<QuickUnlockFailure.RateLimited> {
            quickUnlock.login(owner.user.loginName, QuickUnlockMethod.PIN, PIN.toCharArray())
        }

        security.login(owner.user.loginName, PASSPHRASE.toCharArray())
        quickUnlock.fullPassphraseAccepted(owner.user.userId)
        assertEquals(owner.user.userId, quickUnlock.login(owner.user.loginName, QuickUnlockMethod.PIN, PIN.toCharArray()).userId)
    }

    @Test
    fun overlappingLoginAndReauthenticationAttemptsShareThePinLimit() = runBlocking {
        val owner = owner()
        quickUnlock.enroll(owner.session.sessionId, PASSPHRASE.toCharArray(), false, PIN.toCharArray())
        val start = CompletableDeferred<Unit>()
        val attempts = (0 until 8).map { index ->
            async(Dispatchers.Default) {
                start.await()
                runCatching {
                    if (index % 2 == 0) {
                        quickUnlock.login(owner.user.loginName, QuickUnlockMethod.PIN, WRONG_PIN.toCharArray())
                    } else {
                        quickUnlock.reauthenticate(owner.session.sessionId, QuickUnlockMethod.PIN, WRONG_PIN.toCharArray())
                    }
                }.exceptionOrNull()
            }
        }
        start.complete(Unit)
        val failures = attempts.awaitAll()
        assertEquals("Only the first two failures may proceed without a delay", 2,
            failures.count { it is QuickUnlockFailure.Authentication })
        assertEquals("Every later overlapping attempt must observe the same pause", 6,
            failures.count { it is QuickUnlockFailure.RateLimited })
        assertFailure<QuickUnlockFailure.RateLimited> {
            quickUnlock.login(owner.user.loginName, QuickUnlockMethod.PIN, PIN.toCharArray())
        }
    }

    @Test
    fun passphraseChangeWhilePhonePromptIsOpenCannotGrantANewSession() = runBlocking {
        val owner = owner()
        quickUnlock.enroll(owner.session.sessionId, PASSPHRASE.toCharArray(), true, null)
        gate.onRelease = {
            security.changeOwnPassphrase(owner.session.sessionId, PASSPHRASE.toCharArray(), NEW_PASSPHRASE.toCharArray())
        }
        assertFailure<QuickUnlockFailure.Invalidated> {
            quickUnlock.login(owner.user.loginName, QuickUnlockMethod.DEVICE, null)
        }
        assertTrue(quickUnlock.status().accounts.isEmpty())
    }

    @Test
    fun removalWhilePhonePromptIsOpenCannotReauthenticate() = runBlocking {
        val owner = owner()
        quickUnlock.enroll(owner.session.sessionId, PASSPHRASE.toCharArray(), true, null)
        gate.onRelease = { quickUnlock.remove(owner.session.sessionId, PASSPHRASE.toCharArray()) }
        assertFailure<QuickUnlockFailure.Invalidated> {
            quickUnlock.reauthenticate(owner.session.sessionId, QuickUnlockMethod.DEVICE, null)
        }
        assertTrue(quickUnlock.status().accounts.isEmpty())
    }

    @Test
    fun credentialChangeRestoreAndPhoneSecurityChangeInvalidateEnrollment() = runBlocking {
        val owner = owner()
        quickUnlock.enroll(owner.session.sessionId, PASSPHRASE.toCharArray(), true, PIN.toCharArray())
        security.changeOwnPassphrase(owner.session.sessionId, PASSPHRASE.toCharArray(), NEW_PASSPHRASE.toCharArray())
        assertTrue(quickUnlock.status().accounts.isEmpty())

        val newSession = security.login(owner.user.loginName, NEW_PASSPHRASE.toCharArray())
        quickUnlock.enroll(newSession.sessionId, NEW_PASSPHRASE.toCharArray(), true, null)
        gate.valid = false
        assertTrue(quickUnlock.status().accounts.isEmpty())
        assertTrue(gate.deletedKeys.isNotEmpty())

        gate.valid = true
        quickUnlock.enroll(newSession.sessionId, NEW_PASSPHRASE.toCharArray(), false, PIN.toCharArray())
        gate.available = false
        assertTrue(quickUnlock.status().accounts.isEmpty())

        gate.available = true
        quickUnlock.enroll(newSession.sessionId, NEW_PASSPHRASE.toCharArray(), false, PIN.toCharArray())
        quickUnlock.clearAll()
        assertTrue(quickUnlock.status().accounts.isEmpty())
        assertTrue(store.cleared)
    }

    private suspend fun owner() = security.bootstrapOwner("quick.owner", "Quick Owner", PASSPHRASE.toCharArray())

    private suspend inline fun <reified T : Throwable> assertFailure(crossinline block: suspend () -> Unit) {
        var caught: Throwable? = null
        try {
            block()
        } catch (error: Throwable) {
            caught = error
        }
        assertTrue("Expected ${T::class.java.simpleName}, got ${caught?.javaClass?.simpleName}", caught is T)
    }

    companion object {
        private const val PASSPHRASE = "Quick unlock owner passphrase 2026"
        private const val NEW_PASSPHRASE = "Changed quick unlock passphrase 2026"
        private const val PIN = "642913"
        private const val WRONG_PIN = "111111"
    }
}

private class MemoryQuickUnlockStore : QuickUnlockRecordStore {
    private var records: List<QuickUnlockRecord> = emptyList()
    var cleared = false
        private set

    override fun read(): List<QuickUnlockRecord> = records.toList()

    override fun write(records: List<QuickUnlockRecord>) {
        this.records = records.toList()
    }

    override fun clear() {
        records = emptyList()
        cleared = true
    }
}

private class FakeQuickUnlockDeviceGate : QuickUnlockDeviceGate {
    private val keys = mutableSetOf<String>()
    val deletedKeys = mutableSetOf<String>()
    var valid = true
    var available = true
    var onRelease: (suspend () -> Unit)? = null

    override fun status() = DeviceAuthenticationStatus(available = available, label = "Test phone lock")

    override suspend fun protect(keyId: String, userId: Long, displayName: String, plaintext: ByteArray): DeviceProtectedQuickToken {
        keys += keyId
        return DeviceProtectedQuickToken(
            nonceBase64 = "test-nonce",
            ciphertextBase64 = Base64.getEncoder().encodeToString(plaintext),
            biometricCanaryCreated = false,
        )
    }

    override suspend fun release(keyId: String, userId: Long, displayName: String, protected: DeviceProtectedQuickToken): ByteArray {
        if (keyId !in keys || !valid) throw QuickUnlockFailure.Invalidated()
        onRelease?.invoke()
        return Base64.getDecoder().decode(protected.ciphertextBase64)
    }

    override fun enrollmentStillValid(keyId: String, biometricCanaryCreated: Boolean): Boolean = valid && keyId in keys

    override fun delete(keyId: String) {
        keys -= keyId
        deletedKeys += keyId
    }

    override fun clearAll() {
        deletedKeys += keys
        keys.clear()
    }
}
