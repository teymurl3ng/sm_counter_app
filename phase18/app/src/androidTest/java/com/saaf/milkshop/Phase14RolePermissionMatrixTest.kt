package com.saaf.milkshop

import android.net.Uri
import android.os.SystemClock
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.saaf.milkshop.application.bridge.BackupDocumentPicker
import com.saaf.milkshop.application.bridge.BridgeContractSnapshot
import com.saaf.milkshop.application.bridge.HybridBridgeDispatcher
import com.saaf.milkshop.cloud.InteractiveCloudAuthorization
import com.saaf.milkshop.cloud.InteractiveCloudAuthorizationResult
import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.domain.AppPermission
import com.saaf.milkshop.domain.AppRole
import com.saaf.milkshop.domain.CreateUserInput
import com.saaf.milkshop.domain.RolePermissions
import com.saaf.milkshop.domain.UserSecurityService
import com.saaf.milkshop.hybrid.HybridActivity
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Phase14RolePermissionMatrixTest {
    private lateinit var database: MilkShopDatabase
    private lateinit var security: UserSecurityService
    private lateinit var backupDocuments: CountingBackupDocuments
    private lateinit var dispatcher: HybridBridgeDispatcher

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = MilkShopDatabase.createInMemory()
        security = UserSecurityService(database)
        backupDocuments = CountingBackupDocuments()
        dispatcher = HybridBridgeDispatcher(
            database = database,
            security = security,
            backupManager = BackupManager(context),
            backupDocuments = backupDocuments,
        )
    }

    @After
    fun tearDown() {
        dispatcher.close()
        database.close()
    }

    @Test
    fun allFiveRolesExposeTheExactNativeMatrixAndCannotBypassDeniedPermissions() = runBlocking {
        val sessions = createAllRoleSessions(security)
        val probes = protectedPermissionProbes()
        val contractPermissions = BridgeContractSnapshot.endpoints.mapNotNull { it.permission }.toSet()

        assertEquals(AppPermission.entries.map(AppPermission::name).toSet() - AppPermission.MANAGE_PRIVILEGED_USERS.name, contractPermissions)
        assertEquals(contractPermissions, probes.map { it.permission.name }.toSet())
        probes.forEach { probe ->
            assertEquals(
                "Bridge contract drifted for ${probe.action}",
                probe.permission.name,
                BridgeContractSnapshot.endpoints.single { it.action == probe.action }.permission,
            )
        }

        AppRole.entries.forEach { role ->
            val session = sessions.getValue(role)
            val current = dispatch(dispatcher, "query.currentSession", session = session)
            assertTrue("${role.wireValue} could not read its current session: $current", current.getBoolean("ok"))
            val actualPermissions = current.getJSONObject("data").getJSONArray("effectivePermissionCodes").toStringSet()
            val expectedPermissions = RolePermissions.permissions(role).map { it.name.lowercase() }.toSet()
            assertEquals("Wrong effective permissions for ${role.wireValue}", expectedPermissions, actualPermissions)

            val deliveries = dispatch(dispatcher, "query.deliveries", session = session)
            assertTrue("${role.wireValue} lost the shared delivery permission: $deliveries", deliveries.getBoolean("ok"))

            probes.filterNot { RolePermissions.allows(role, it.permission) }.forEach { probe ->
                assertError(
                    dispatch(dispatcher, probe.action, probe.payload(), session),
                    "permission_denied",
                    "${role.wireValue} bypassed ${probe.permission.name} through ${probe.action}",
                )
            }
        }

        val adminSession = sessions.getValue(AppRole.ADMIN)
        assertError(
            dispatch(
                dispatcher,
                "command.auth.beginUserEnrollment",
                JSONObject()
                    .put("loginName", "matrix.second.admin")
                    .put("displayName", "Second Matrix Admin")
                    .put("roleCode", "admin")
                    .put("employeeAccountId", JSONObject.NULL),
                adminSession,
            ),
            "permission_denied",
            "Administrator bypassed the owner-only privileged-user boundary",
        )
        assertEquals("A denied backup call opened Android's create picker", 0, backupDocuments.createCount)
        assertEquals("A denied restore call opened Android's file picker", 0, backupDocuments.openCount)
    }

    @Test
    fun packagedUiShowsOnlyEachRolesAuthorizedAreasTasksAndSensitiveControls() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val app = instrumentation.targetContext.applicationContext as MilkShopApplication
        val suffix = System.nanoTime().toString().takeLast(8)
        val ownerSession = app.userSecurity.login(
            SaafMilkTestRunner.TEST_LOGIN,
            SaafMilkTestRunner.TEST_PASSPHRASE.toCharArray(),
        ).sessionId
        val accounts = buildList {
            add(UiRoleAccount(AppRole.OWNER, SaafMilkTestRunner.TEST_LOGIN, "Instrumentation Owner", SaafMilkTestRunner.TEST_PASSPHRASE))
            add(createPackagedAccount(app.userSecurity, ownerSession, AppRole.ADMIN, suffix))
            add(createPackagedAccount(app.userSecurity, ownerSession, AppRole.MANAGER, suffix))
            add(createPackagedAccount(app.userSecurity, ownerSession, AppRole.CASHIER, suffix))
            add(createPackagedAccount(app.userSecurity, ownerSession, AppRole.OPERATOR, suffix))
        }

        val admin = accounts.single { it.role == AppRole.ADMIN }
        val cloudDispatcher = HybridBridgeDispatcher(
            database = app.database,
            security = app.userSecurity,
            application = app,
            cloudAuthorization = FailingCloudAuthorization,
        )
        try {
            val adminSession = app.userSecurity.login(admin.loginName, admin.passphrase.toCharArray()).sessionId
            assertError(
                dispatch(cloudDispatcher, "system.cloudBackupStatus", session = adminSession),
                "permission_denied",
                "Administrator bypassed the owner-only cloud-backup boundary",
            )
        } finally {
            cloudDispatcher.close()
        }

        app.currentSessionId = null
        instrumentation.uiAutomation.executeShellCommand("input keyevent KEYCODE_WAKEUP").close()
        instrumentation.uiAutomation.executeShellCommand("wm dismiss-keyguard").close()
        SystemClock.sleep(250)
        val scenario = ActivityScenario.launch(HybridActivity::class.java)
        try {
            val driver = HybridWebViewTestDriver.attach(scenario)
            driver.waitFor("document.getElementById('login-button') !== null", 20_000L)
            accounts.forEach { account -> verifyPackagedRole(driver, account) }
        } finally {
            scenario.close()
            val replacement = app.userSecurity.login(
                SaafMilkTestRunner.TEST_LOGIN,
                SaafMilkTestRunner.TEST_PASSPHRASE.toCharArray(),
            )
            app.currentSessionId = replacement.sessionId
        }
    }

    private suspend fun createAllRoleSessions(service: UserSecurityService): Map<AppRole, String> {
        val owner = service.bootstrapOwner("matrix.owner", "Matrix Owner", MATRIX_PASSPHRASE.toCharArray())
        val sessions = linkedMapOf(AppRole.OWNER to owner.session.sessionId)
        AppRole.entries.filterNot { it == AppRole.OWNER }.forEach { role ->
            val challenge = service.beginUserEnrollment(
                owner.session.sessionId,
                CreateUserInput("matrix.${role.wireValue}", "Matrix ${role.wireValue}", role.wireValue),
            )
            service.completeUserEnrollment(challenge.enrollmentToken, MATRIX_PASSPHRASE.toCharArray())
            sessions[role] = service.login("matrix.${role.wireValue}", MATRIX_PASSPHRASE.toCharArray()).sessionId
        }
        return sessions
    }

    private suspend fun createPackagedAccount(
        service: UserSecurityService,
        ownerSession: String,
        role: AppRole,
        suffix: String,
    ): UiRoleAccount {
        val loginName = "phase14.${role.wireValue}.$suffix"
        val displayName = "Phase 14 ${role.wireValue.replaceFirstChar(Char::uppercase)}"
        val challenge = service.beginUserEnrollment(
            ownerSession,
            CreateUserInput(loginName, displayName, role.wireValue),
        )
        service.completeUserEnrollment(challenge.enrollmentToken, UI_ROLE_PASSPHRASE.toCharArray())
        return UiRoleAccount(role, loginName, displayName, UI_ROLE_PASSPHRASE)
    }

    private fun verifyPackagedRole(driver: HybridWebViewTestDriver, account: UiRoleAccount) {
        driver.setInput("login-name", account.loginName)
        driver.setInput("login-passphrase", account.passphrase)
        driver.evaluate("document.getElementById('login-button').click(); true")
        driver.waitFor(
            "document.getElementById('signed-in-user')?.innerText.includes(${JSONObject.quote(account.displayName)}) === true",
            20_000L,
        )

        val expectedAreas = expectedAreas.getValue(account.role)
        assertDomIds(driver, "[data-app-area]", expectedAreas, "${account.role.wireValue} navigation")

        driver.evaluate("document.getElementById('operations-nav').click(); true")
        driver.waitFor("document.getElementById('daily-task-panel') !== null")
        assertDomIds(
            driver,
            "#daily-task-navigation > button",
            expectedTasks.getValue(account.role),
            "${account.role.wireValue} daily tasks",
        )

        driver.evaluate("document.getElementById('access-nav').click(); true")
        driver.waitFor("document.getElementById('security-account-tab') !== null", 20_000L)
        val protectedTabs = account.role == AppRole.OWNER || account.role == AppRole.ADMIN
        assertEquals(
            "${account.role.wireValue} staff-account control visibility",
            protectedTabs.toString(),
            driver.evaluate("document.getElementById('security-staff-tab') !== null"),
        )
        assertEquals(
            "${account.role.wireValue} security-audit visibility",
            protectedTabs.toString(),
            driver.evaluate("document.getElementById('security-audit-tab') !== null"),
        )
        if (protectedTabs) {
            driver.waitFor("document.getElementById('new-user-role') !== null", 20_000L)
            val roleOptions = if (account.role == AppRole.OWNER) AppRole.entries.map(AppRole::wireValue) else ordinaryRoles
            assertSelectOptions(driver, "new-user-role", roleOptions, "${account.role.wireValue} assignable roles")
        }

        val canExport = account.role == AppRole.OWNER || account.role == AppRole.ADMIN
        if (canExport) {
            driver.evaluate("document.getElementById('backup-nav').click(); true")
            driver.waitFor("document.getElementById('encrypted-backup-form') !== null", 20_000L)
            val canRestore = account.role == AppRole.OWNER
            listOf("backup-create-tab", "backup-cloud-tab", "backup-restore-tab").forEach { id ->
                assertEquals(
                    "${account.role.wireValue} sensitive backup control $id",
                    canRestore.toString(),
                    driver.evaluate("document.getElementById(${JSONObject.quote(id)}) !== null"),
                )
            }
        }

        driver.evaluate("document.getElementById('logout-button').click(); true")
        driver.waitFor("document.getElementById('login-button') !== null", 20_000L)
    }

    private fun assertDomIds(
        driver: HybridWebViewTestDriver,
        selector: String,
        expected: List<String>,
        label: String,
    ) {
        val expectedJson = JSONArray(expected.sorted()).toString()
        val script = """(() => {
            const actual = Array.from(document.querySelectorAll(${JSONObject.quote(selector)})).map((element) => element.id).sort();
            return JSON.stringify(actual) === JSON.stringify($expectedJson);
        })()"""
        assertEquals("$label did not match $expectedJson", "true", driver.evaluate(script))
    }

    private fun assertSelectOptions(
        driver: HybridWebViewTestDriver,
        id: String,
        expected: List<String>,
        label: String,
    ) {
        val expectedJson = JSONArray(expected).toString()
        val script = """(() => {
            const actual = Array.from(document.getElementById(${JSONObject.quote(id)}).options).map((option) => option.value);
            return JSON.stringify(actual) === JSON.stringify($expectedJson);
        })()"""
        assertEquals("$label did not match $expectedJson", "true", driver.evaluate(script))
    }

    private fun protectedPermissionProbes(): List<PermissionProbe> = listOf(
        PermissionProbe(AppPermission.VIEW_OPERATIONAL, "query.currentSession"),
        PermissionProbe(AppPermission.VIEW_FULL_FINANCIAL, "query.financialOverview", ::dateRange),
        PermissionProbe(AppPermission.SALE_AND_COLLECTION, "query.cashierSummary", ::dateRange),
        PermissionProbe(AppPermission.PURCHASE, "command.recordPurchase") { JSONObject()
            .put("vendorAccountId", "1").put("occurredAt", BUSINESS_DATE)
            .put("rawLiters", "1").put("fatPercent", "4").put("lrReading", "28") },
        PermissionProbe(AppPermission.SUPPLIER_PAYMENT, "command.recordVendorPayment") { paymentPayload("vendorAccountId") },
        PermissionProbe(AppPermission.EXPENSE, "command.recordExpense") { JSONObject()
            .put("expenseCategoryAccountId", "1").put("paymentAccountId", "1")
            .put("amountPaisa", "100").put("occurredAt", BUSINESS_DATE) },
        PermissionProbe(AppPermission.SALARY_PAYMENT, "command.recordSalaryPayment") { paymentPayload("employeeAccountId") },
        PermissionProbe(AppPermission.PRODUCTION_AND_SPOILAGE, "query.spoilageHistory"),
        PermissionProbe(AppPermission.DELIVERY, "query.deliveries"),
        PermissionProbe(AppPermission.MANAGE_MASTER_DATA, "query.masterData"),
        PermissionProbe(AppPermission.ATTENDANCE, "query.attendance") { JSONObject().put("businessDate", BUSINESS_DATE) },
        PermissionProbe(AppPermission.CORRECTION, "command.correctEvent") { JSONObject()
            .put("correctionTypeCode", "monetary").put("originalEventId", "1")
            .put("occurredAt", BUSINESS_DATE).put("amountPaisa", "100") },
        PermissionProbe(AppPermission.CASH_COUNT_AND_CLOSE, "query.reconciliationArchive"),
        PermissionProbe(AppPermission.REOPEN_DAY, "command.reopenBusinessDate") { JSONObject()
            .put("businessDate", BUSINESS_DATE).put("reason", "Phase 14 permission probe") },
        PermissionProbe(AppPermission.MANAGE_BUSINESS_SETTINGS, "query.businessSettings"),
        PermissionProbe(AppPermission.CREATE_EXPORT_BACKUP, "system.exportEncryptedBackup") { JSONObject().put("passphrase", BACKUP_PASSPHRASE) },
        PermissionProbe(AppPermission.RESTORE_BACKUP, "system.inspectEncryptedBackup") { JSONObject().put("passphrase", BACKUP_PASSPHRASE) },
        PermissionProbe(AppPermission.MANAGE_STAFF_USERS, "query.users"),
        PermissionProbe(AppPermission.VIEW_SECURITY_AUDIT, "query.securityAudit"),
    )

    private fun dateRange() = JSONObject()
        .put("startDateInclusive", BUSINESS_DATE)
        .put("endDateExclusive", "2026-09-07")

    private fun paymentPayload(partyField: String) = JSONObject()
        .put(partyField, "1")
        .put("paymentAccountId", "1")
        .put("amountPaisa", "100")
        .put("occurredAt", BUSINESS_DATE)

    private suspend fun dispatch(
        target: HybridBridgeDispatcher,
        action: String,
        payload: JSONObject = JSONObject(),
        session: String? = null,
    ): JSONObject {
        val request = JSONObject()
            .put("contractVersion", BridgeContractSnapshot.VERSION)
            .put("requestId", UUID.randomUUID().toString())
            .put("action", action)
            .put("payload", payload)
        if (session != null) request.put("sessionHandle", session)
        return JSONObject(target.dispatch(request.toString()))
    }

    private fun assertError(response: JSONObject, code: String, message: String) {
        assertFalse("$message: $response", response.getBoolean("ok"))
        assertEquals(message, code, response.getJSONObject("error").getString("code"))
    }

    private fun JSONArray.toStringSet(): Set<String> = (0 until length()).map(::getString).toSet()

    private data class PermissionProbe(
        val permission: AppPermission,
        val action: String,
        val payload: () -> JSONObject = { JSONObject() },
    )

    private data class UiRoleAccount(
        val role: AppRole,
        val loginName: String,
        val displayName: String,
        val passphrase: String,
    )

    private class CountingBackupDocuments : BackupDocumentPicker {
        var createCount = 0
        var openCount = 0
        override suspend fun createDocument(suggestedFileName: String?): Uri? = null.also { createCount += 1 }
        override suspend fun openDocument(): Uri? = null.also { openCount += 1 }
    }

    private object FailingCloudAuthorization : InteractiveCloudAuthorization {
        override suspend fun requestNewRecoveryPassphrase(): CharArray = error("Cloud authorization must not open for an administrator")
        override suspend fun requestRecoveryPassphrase(): CharArray = error("Cloud recovery must not open for an administrator")
        override suspend fun connect(): InteractiveCloudAuthorizationResult = error("Cloud sign-in must not open for an administrator")
        override suspend fun revokeSelectedAccount(expectedPermissionId: String) = error("Cloud revocation must not run for an administrator")
        override suspend fun requestNotificationPermission(): Boolean = error("Notification permission must not open for an administrator")
    }

    private companion object {
        const val BUSINESS_DATE = "2026-09-06"
        const val MATRIX_PASSPHRASE = "phase fourteen native matrix private passphrase 2026"
        const val UI_ROLE_PASSPHRASE = "phase fourteen packaged role private passphrase 2026"
        const val BACKUP_PASSPHRASE = "phase fourteen backup permission passphrase 2026"

        val ordinaryRoles = listOf("manager", "cashier", "operator")
        val expectedAreas = mapOf(
            AppRole.OWNER to listOf("dashboard-nav", "operations-nav", "deliveries-nav", "accounts-nav", "history-nav", "cash-close-nav", "products-nav", "customers-nav", "suppliers-nav", "employees-nav", "costing-planning-nav", "settings-nav", "access-nav", "backup-nav"),
            AppRole.ADMIN to listOf("dashboard-nav", "operations-nav", "deliveries-nav", "accounts-nav", "history-nav", "cash-close-nav", "products-nav", "customers-nav", "suppliers-nav", "employees-nav", "costing-planning-nav", "settings-nav", "access-nav", "backup-nav"),
            AppRole.MANAGER to listOf("dashboard-nav", "operations-nav", "deliveries-nav", "accounts-nav", "history-nav", "cash-close-nav", "products-nav", "customers-nav", "suppliers-nav", "employees-nav", "settings-nav", "access-nav"),
            AppRole.CASHIER to listOf("dashboard-nav", "operations-nav", "deliveries-nav", "history-nav", "cash-close-nav", "access-nav"),
            AppRole.OPERATOR to listOf("dashboard-nav", "operations-nav", "deliveries-nav", "history-nav", "employees-nav", "access-nav"),
        )
        val allDailyTasks = listOf("purchase-work-tab", "sale-work-tab", "production-work-tab", "spoilage-work-tab", "expense-work-tab", "payments-work-tab", "salary-work-tab")
        val expectedTasks = mapOf(
            AppRole.OWNER to allDailyTasks,
            AppRole.ADMIN to allDailyTasks,
            AppRole.MANAGER to allDailyTasks,
            AppRole.CASHIER to listOf("sale-work-tab", "expense-work-tab", "payments-work-tab"),
            AppRole.OPERATOR to listOf("purchase-work-tab", "production-work-tab", "spoilage-work-tab"),
        )
    }
}
