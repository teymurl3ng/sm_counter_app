package com.saaf.milkshop

import androidx.lifecycle.ViewModelStore
import androidx.test.platform.app.InstrumentationRegistry
import com.saaf.milkshop.application.authentication.*
import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.domain.UserSecurityService
import com.saaf.milkshop.nativeui.auth.*
import com.saaf.milkshop.quickunlock.*
import java.util.Base64
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*

/** In-memory credentials/sessions; the system phone prompt is controlled. */
class NativeQuickPinRoutingTest {
    private lateinit var database: MilkShopDatabase
    private lateinit var quick: QuickUnlockService
    private lateinit var facade: NativeAuthenticationFacade
    private lateinit var model: NativeAuthenticationViewModel
    private lateinit var owner: NativeOwnerBootstrap
    private val gate = RoutingPhoneGate()
    private val models = ViewModelStore()
    private var handle: String? = null
    private val passphrase = "Keypad routing owner passphrase 2026"

    @Before fun setup(): Unit = runBlocking {
        database = MilkShopDatabase.createInMemory()
        val security = UserSecurityService(database)
        facade = NativeAuthenticationFacade(database, security)
        owner = facade.bootstrapOwner("keypad.routing", "Routing Owner", passphrase.toCharArray())
        quick = QuickUnlockService(database, security, RoutingRecordStore(), gate)
        main {
            model = NativeAuthenticationViewModel(facade) { handle = it }
            models.put("routing", model)
            model.attachQuickUnlock(NativeQuickUnlockFacade(quick, facade))
        }
        await { it is NativeAuthenticationState.AccountPicker }
        Unit
    }
    @After fun cleanup() { main { models.clear() }; quick.close(); database.close() }
    private fun main(block: () -> Unit) = InstrumentationRegistry.getInstrumentation().runOnMainSync(block)
    private fun await(predicate: (NativeAuthenticationState) -> Boolean): NativeAuthenticationState {
        val until = android.os.SystemClock.uptimeMillis() + 15_000
        while (android.os.SystemClock.uptimeMillis() < until) {
            val state = model.state.value
            if (predicate(state)) return state
            android.os.SystemClock.sleep(20)
        }
        throw AssertionError("Unexpected route: ${model.state.value}")
    }
    private fun select() {
        val account = (model.state.value as NativeAuthenticationState.AccountPicker).accounts.single()
        main { model.selectAccount(account) }
    }
    private fun enroll(phone: Boolean, pin: String?) = runBlocking {
        quick.clearAll()
        quick.enroll(owner.session.sessionHandle, passphrase.toCharArray(), phone, pin?.toCharArray())
        main { model.refreshStartup() }; await { it is NativeAuthenticationState.AccountPicker }
    }

    @Test fun routingMatrixAndPassphraseBack() {
        select(); assertTrue(model.state.value is NativeAuthenticationState.Passphrase)
        main { model.backFromPassphrase() }; await { it is NativeAuthenticationState.AccountPicker }
        enroll(true, null); select(); assertTrue(model.state.value is NativeAuthenticationState.MethodPicker)
        main { model.useFullPassphrase(); model.backFromPassphrase() }
        assertTrue(model.state.value is NativeAuthenticationState.MethodPicker)
        enroll(false, "0123"); select(); assertTrue(model.state.value is NativeAuthenticationState.QuickPin)
        main { model.useFullPassphrase(); model.backFromPassphrase() }
        assertTrue(model.state.value is NativeAuthenticationState.QuickPin)
        main { model.backFromQuickPin() }; await { it is NativeAuthenticationState.AccountPicker }
        enroll(true, "0123"); select(); assertTrue(model.state.value is NativeAuthenticationState.QuickPin)
    }
    @Test fun longLeadingZeroPinAndSwitchCancellationPreserveSession() {
        enroll(false, "012345678901"); select()
        main { model.signInWithQuickPin("012345678901".toCharArray()) }
        val signed = await { it is NativeAuthenticationState.Authenticated } as NativeAuthenticationState.Authenticated
        assertEquals(signed.sessionHandle, handle)
        main { model.beginAccountSwitch() }; await { it is NativeAuthenticationState.AccountPicker }
        select(); assertEquals(signed, model.state.value)
        main { model.beginAccountSwitch() }; await { it is NativeAuthenticationState.AccountPicker }
        main { model.cancelAccountSwitch() }; assertEquals(signed, model.state.value)
        assertEquals(signed.sessionHandle, handle)
    }
    @Test fun phoneCancelUnavailableAndSuccessKeepOriginatingAccount() {
        enroll(true, "0123"); select()
        gate.onRelease = { throw QuickUnlockFailure.Cancelled() }
        main { model.signInWithPhoneSecurity() }
        val cancelled = await { it is NativeAuthenticationState.QuickPin && it.error != null } as NativeAuthenticationState.QuickPin
        assertEquals("Quick unlock was cancelled", cancelled.error); assertFalse(cancelled.busy)
        gate.onRelease = { throw QuickUnlockFailure.Unavailable() }
        main { model.signInWithPhoneSecurity() }
        await { it is NativeAuthenticationState.QuickPin && !it.busy && it.error != null }
        gate.onRelease = null
        main { model.signInWithPhoneSecurity(); model.signInWithPhoneSecurity() }
        val signed = await { it is NativeAuthenticationState.Authenticated } as NativeAuthenticationState.Authenticated
        assertEquals(cancelled.account.userId, signed.identity.userId); assertEquals(3, gate.releaseCalls)
    }
    @Test fun latePhoneSuccessCannotReplaceANewerRoute() {
        enroll(true, "0123"); select()
        val entered = CompletableDeferred<Unit>(); val released = CompletableDeferred<Unit>()
        gate.onRelease = { entered.complete(Unit); released.await() }
        main { model.signInWithPhoneSecurity() }; runBlocking { entered.await() }
        main { model.refreshStartup() }; await { it is NativeAuthenticationState.AccountPicker }
        released.complete(Unit)
        val until = android.os.SystemClock.uptimeMillis() + 5_000
        while (!gate.finished && android.os.SystemClock.uptimeMillis() < until) android.os.SystemClock.sleep(20)
        android.os.SystemClock.sleep(250)
        assertTrue(model.state.value is NativeAuthenticationState.AccountPicker); assertNull(handle)
    }
}
private class RoutingRecordStore : QuickUnlockRecordStore {
    private var records = emptyList<QuickUnlockRecord>()
    override fun read() = records.toList()
    override fun write(records: List<QuickUnlockRecord>) { this.records = records.toList() }
    override fun clear() { records = emptyList() }
}
private class RoutingPhoneGate : QuickUnlockDeviceGate {
    private val keys = mutableSetOf<String>()
    var onRelease: (suspend () -> Unit)? = null
    @Volatile var finished = false
    var releaseCalls = 0
    override fun status() = DeviceAuthenticationStatus(true, "Test phone lock")
    override suspend fun protect(keyId: String, userId: Long, displayName: String, plaintext: ByteArray): DeviceProtectedQuickToken {
        keys += keyId
        return DeviceProtectedQuickToken("test-nonce", Base64.getEncoder().encodeToString(plaintext), false)
    }
    override suspend fun release(keyId: String, userId: Long, displayName: String, protected: DeviceProtectedQuickToken): ByteArray {
        releaseCalls++; onRelease?.invoke(); finished = true
        return Base64.getDecoder().decode(protected.ciphertextBase64)
    }
    override fun enrollmentStillValid(keyId: String, biometricCanaryCreated: Boolean) = keyId in keys
    override fun delete(keyId: String) { keys -= keyId }
    override fun clearAll() { keys.clear() }
}
