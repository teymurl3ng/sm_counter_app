package com.saaf.milkshop

import android.os.Bundle
import androidx.test.runner.AndroidJUnitRunner
import com.saaf.milkshop.domain.SecurityFailure
import kotlinx.coroutines.runBlocking

/** Supplies a real owner session to legacy UI tests without adding a release-build bypass. */
class SaafMilkTestRunner : AndroidJUnitRunner() {
    private var requestedTestClass: String? = null

    override fun onCreate(arguments: Bundle) {
        requestedTestClass = arguments.getString("class")?.substringBefore('#')
        super.onCreate(arguments)
    }

    override fun onStart() {
        val app = targetContext.applicationContext as MilkShopApplication
        runBlocking {
            val session = if (app.userSecurity.needsOwnerBootstrap()) {
                app.userSecurity.bootstrapOwner(
                    TEST_LOGIN,
                    "Instrumentation Owner",
                    TEST_PASSPHRASE.toCharArray(),
                ).session
            } else try {
                app.userSecurity.login(TEST_LOGIN, TEST_PASSPHRASE.toCharArray())
            } catch (failure: SecurityFailure.Authentication) {
                if (requestedTestClass != PHASE9_READ_ONLY_NAVIGATION_TEST) throw failure
                val owner = app.database.appUserDao().all().firstOrNull {
                    it.role == "owner" && it.status == "active" && !it.credentialSetupRequired
                } ?: throw failure
                app.nativeSessions.start(owner.userId)
            }
            app.currentSessionId = session.sessionId
        }
        super.onStart()
    }

    companion object {
        const val TEST_LOGIN = "instrumentation.owner"
        const val TEST_PASSPHRASE = "instrumentation owner passphrase 2026"
        private const val PHASE9_READ_ONLY_NAVIGATION_TEST = "com.saaf.milkshop.Phase9NavigationRuntimeTest"
    }
}
