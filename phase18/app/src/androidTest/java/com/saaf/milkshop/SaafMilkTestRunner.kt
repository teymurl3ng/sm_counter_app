package com.saaf.milkshop

import android.os.Bundle
import androidx.test.runner.AndroidJUnitRunner
import com.saaf.milkshop.domain.AppRole
import com.saaf.milkshop.domain.CreateUserInput
import com.saaf.milkshop.domain.CustomerManagementService
import com.saaf.milkshop.quickunlock.AndroidQuickUnlockRecordStore
import com.saaf.milkshop.quickunlock.DeviceAuthenticationStatus
import com.saaf.milkshop.quickunlock.DeviceProtectedQuickToken
import com.saaf.milkshop.quickunlock.QuickUnlockDeviceGate
import com.saaf.milkshop.quickunlock.QuickUnlockFailure
import com.saaf.milkshop.quickunlock.QuickUnlockService
import kotlinx.coroutines.runBlocking

/** Supplies a real owner session to legacy UI tests without adding a release-build bypass. */
class SaafMilkTestRunner : AndroidJUnitRunner() {
    private var skipBootstrap = false

    override fun onCreate(arguments: Bundle) {
        skipBootstrap = arguments.getString(SKIP_BOOTSTRAP_ARGUMENT).equals("true", ignoreCase = true)
        super.onCreate(arguments)
    }

    override fun onStart() {
        if (skipBootstrap) {
            super.onStart()
            return
        }
        val app = targetContext.applicationContext as MilkShopApplication
        runBlocking {
            val ownerSession = if (app.userSecurity.needsOwnerBootstrap()) {
                app.userSecurity.bootstrapOwner(
                    TEST_LOGIN,
                    "Instrumentation Owner",
                    TEST_PASSPHRASE.toCharArray(),
                ).session
            } else {
                app.userSecurity.login(TEST_LOGIN, TEST_PASSPHRASE.toCharArray())
            }
            val cashier = app.database.appUserDao().findByLoginName(TEST_CASHIER_LOGIN)
                ?: app.userSecurity.beginUserEnrollment(
                    ownerSession.sessionId,
                    CreateUserInput(
                        loginName = TEST_CASHIER_LOGIN,
                        displayName = "Instrumentation Cashier",
                        role = AppRole.CASHIER.wireValue,
                    ),
                ).let { pending ->
                    app.userSecurity.completeUserEnrollment(
                        pending.enrollmentToken,
                        TEST_CASHIER_PASSPHRASE.toCharArray(),
                    )
                }
            val operator = app.database.appUserDao().findByLoginName(TEST_OPERATOR_LOGIN)
                ?: app.userSecurity.beginUserEnrollment(
                    ownerSession.sessionId,
                    CreateUserInput(
                        loginName = TEST_OPERATOR_LOGIN,
                        displayName = "Instrumentation Operator",
                        role = AppRole.OPERATOR.wireValue,
                    ),
                ).let { pending ->
                    app.userSecurity.completeUserEnrollment(
                        pending.enrollmentToken,
                        TEST_OPERATOR_PASSPHRASE.toCharArray(),
                    )
                }
            val cashierSession = app.userSecurity.login(cashier.loginName, TEST_CASHIER_PASSPHRASE.toCharArray())
            val operatorSession = app.userSecurity.login(operator.loginName, TEST_OPERATOR_PASSPHRASE.toCharArray())
            ensureCustomer(app, "Ahmed Store")
            ensureCustomer(app, "Bilal Dairy Point")
            val quickUnlock = QuickUnlockService(
                database = app.database,
                security = app.userSecurity,
                store = AndroidQuickUnlockRecordStore(targetContext.applicationContext),
                deviceGate = PinFixtureDeviceGate(),
            )
            try {
                enrollPin(quickUnlock, ownerSession.sessionId, TEST_PASSPHRASE, TEST_PIN)
                enrollPin(quickUnlock, cashierSession.sessionId, TEST_CASHIER_PASSPHRASE, TEST_CASHIER_PIN)
            } finally {
                quickUnlock.close()
                app.userSecurity.logout(cashierSession.sessionId)
                app.userSecurity.logout(operatorSession.sessionId)
            }
            app.currentSessionId = ownerSession.sessionId
        }
        super.onStart()
    }

    companion object {
        const val SKIP_BOOTSTRAP_ARGUMENT = "saafSkipBootstrap"
        const val TEST_LOGIN = "instrumentation.owner"
        const val TEST_PASSPHRASE = "instrumentation owner passphrase 2026"
        const val TEST_PIN = "642913"
        const val TEST_CASHIER_LOGIN = "instrumentation.cashier"
        const val TEST_CASHIER_PASSPHRASE = "instrumentation cashier passphrase 2026"
        const val TEST_CASHIER_PIN = "731406"
        const val TEST_OPERATOR_LOGIN = "instrumentation.operator"
        const val TEST_OPERATOR_PASSPHRASE = "instrumentation operator passphrase 2026"

        private suspend fun enrollPin(
            quickUnlock: QuickUnlockService,
            sessionId: String,
            passphraseValue: String,
            pinValue: String,
        ) {
            val passphrase = passphraseValue.toCharArray()
            val pin = pinValue.toCharArray()
            try {
                quickUnlock.enroll(sessionId, passphrase, enableDeviceAuthentication = false, pin = pin)
            } finally {
                passphrase.fill('\u0000')
                pin.fill('\u0000')
            }
        }

        private suspend fun ensureCustomer(app: MilkShopApplication, name: String) {
            val exists = app.database.accountDao().findByCategory("Customer").any {
                it.name.removePrefix("Customer: ").trim().equals(name, ignoreCase = true)
            }
            if (!exists) CustomerManagementService(app.database).create(name)
        }
    }
}

private class PinFixtureDeviceGate : QuickUnlockDeviceGate {
    override fun status() = DeviceAuthenticationStatus(available = true, label = "Phone security")

    override suspend fun protect(
        keyId: String,
        userId: Long,
        displayName: String,
        plaintext: ByteArray,
    ): DeviceProtectedQuickToken = throw QuickUnlockFailure.Unavailable()

    override suspend fun release(
        keyId: String,
        userId: Long,
        displayName: String,
        protected: DeviceProtectedQuickToken,
    ): ByteArray = throw QuickUnlockFailure.Unavailable()

    override fun enrollmentStillValid(keyId: String, biometricCanaryCreated: Boolean) = true
    override fun delete(keyId: String) = Unit
    override fun clearAll() = Unit
}
