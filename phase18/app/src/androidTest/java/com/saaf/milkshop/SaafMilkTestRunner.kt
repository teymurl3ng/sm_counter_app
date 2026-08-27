package com.saaf.milkshop

import androidx.test.runner.AndroidJUnitRunner
import kotlinx.coroutines.runBlocking

/** Supplies a real owner session to legacy UI tests without adding a release-build bypass. */
class SaafMilkTestRunner : AndroidJUnitRunner() {
    override fun onStart() {
        val app = targetContext.applicationContext as MilkShopApplication
        runBlocking {
            val session = if (app.userSecurity.needsOwnerBootstrap()) {
                app.userSecurity.bootstrapOwner(
                    TEST_LOGIN,
                    "Instrumentation Owner",
                    TEST_PASSPHRASE.toCharArray(),
                ).session
            } else {
                app.userSecurity.login(TEST_LOGIN, TEST_PASSPHRASE.toCharArray())
            }
            app.currentSessionId = session.sessionId
        }
        super.onStart()
    }

    companion object {
        const val TEST_LOGIN = "instrumentation.owner"
        const val TEST_PASSPHRASE = "instrumentation owner passphrase 2026"
    }
}
