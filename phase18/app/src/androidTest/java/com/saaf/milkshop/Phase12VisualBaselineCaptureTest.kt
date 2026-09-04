package com.saaf.milkshop

import android.os.Build
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import android.webkit.WebView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.saaf.milkshop.data.EventActorLinkEntity
import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.domain.AttendanceInput
import com.saaf.milkshop.domain.AttendanceService
import com.saaf.milkshop.domain.CreateUserInput
import com.saaf.milkshop.domain.CreditCollectionInput
import com.saaf.milkshop.domain.CreditCollectionService
import com.saaf.milkshop.domain.CustomerManagementService
import com.saaf.milkshop.domain.CustomerProfileInput
import com.saaf.milkshop.domain.CustomerProfileService
import com.saaf.milkshop.domain.DeliveryInput
import com.saaf.milkshop.domain.DeliveryService
import com.saaf.milkshop.domain.EmployeeManagementService
import com.saaf.milkshop.domain.EmployeeProfileInput
import com.saaf.milkshop.domain.EmployeeProfileService
import com.saaf.milkshop.domain.ExpenseCategoryManagementService
import com.saaf.milkshop.domain.ExpenseInput
import com.saaf.milkshop.domain.ExpenseService
import com.saaf.milkshop.domain.PhysicalCashCountInput
import com.saaf.milkshop.domain.PhysicalCashCountService
import com.saaf.milkshop.domain.ProductCatalogInput
import com.saaf.milkshop.domain.ProductCatalogService
import com.saaf.milkshop.domain.SalaryPaymentInput
import com.saaf.milkshop.domain.SalaryPaymentService
import com.saaf.milkshop.domain.SaleInput
import com.saaf.milkshop.domain.SalePayment
import com.saaf.milkshop.domain.SaleService
import com.saaf.milkshop.domain.SpoilageInput
import com.saaf.milkshop.domain.SpoilageService
import com.saaf.milkshop.domain.StoreIdentityInput
import com.saaf.milkshop.domain.StoreIdentityService
import com.saaf.milkshop.domain.VendorManagementService
import com.saaf.milkshop.domain.VendorPaymentInput
import com.saaf.milkshop.domain.VendorPaymentService
import com.saaf.milkshop.domain.VendorProfileInput
import com.saaf.milkshop.domain.VendorProfileService
import com.saaf.milkshop.domain.VendorPurchaseInput
import com.saaf.milkshop.domain.VendorPurchaseService
import com.saaf.milkshop.domain.YogurtProductionInput
import com.saaf.milkshop.domain.YogurtProductionService
import com.saaf.milkshop.hybrid.HybridActivity
import java.io.File
import java.io.FileOutputStream
import java.time.Instant
import java.time.LocalDate
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Captures the Phase 12 before-state from the real packaged UI on a physical device.
 *
 * Run only against the isolated QA identity after clearing com.saaf.milkshop.qa, with
 * -Pandroid.testInstrumentationRunnerArguments.saafSkipBootstrap=true. The output is
 * written to /sdcard/Download/phase12-baseline for adb pull before Gradle removes the
 * isolated QA package.
 */
@RunWith(AndroidJUnit4::class)
class Phase12VisualBaselineCaptureTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private lateinit var outputDirectory: File
    private lateinit var manifestFile: File
    private val manifestRows = mutableListOf<String>()
    private var captureNumber = 0

    @Test
    fun captureEveryRetainedScreenAndMeaningfulState() = runBlocking {
        wakeDevice()
        val app = instrumentation.targetContext.applicationContext as MilkShopApplication
        assertTrue(
            "Clear com.saaf.milkshop.qa and pass ${SaafMilkTestRunner.SKIP_BOOTSTRAP_ARGUMENT}=true before baseline capture",
            app.userSecurity.needsOwnerBootstrap(),
        )
        outputDirectory = File(requireNotNull(instrumentation.targetContext.getExternalFilesDir(null)), OUTPUT_DIRECTORY)
        if (outputDirectory.exists()) outputDirectory.deleteRecursively()
        assertTrue(outputDirectory.mkdirs())
        manifestFile = File(outputDirectory, "manifest.tsv")
        preparePublicEvidenceDirectory()
        manifestRows += "capture\tstate\tposition\tdevice\tandroid"
        syncManifest()

        val scenario = ActivityScenario.launch(HybridActivity::class.java)
        try {
            var driver = HybridWebViewTestDriver.attach(scenario)
            driver.waitFor("document.getElementById('create-owner-button') !== null", 30_000L)
            capturePage(driver, "auth-owner-setup")

            driver.setInput("owner-display-name", OWNER_DISPLAY_NAME)
            driver.setInput("owner-login-name", OWNER_LOGIN)
            driver.setInput("owner-passphrase", OWNER_PASSPHRASE)
            driver.setInput("owner-passphrase-confirm", OWNER_PASSPHRASE)
            click(driver, "create-owner-button", "document.getElementById('recovery-saved') !== null", 30_000L)
            driver.evaluate("document.getElementById('owner-recovery-code').textContent = 'RECOVERY CODE HIDDEN IN REVIEW EVIDENCE'; true")
            capturePage(driver, "auth-owner-recovery-code-redacted")

            val seeded = seedRepresentativeShop(app)
            click(driver, "recovery-saved", "document.getElementById('app-navigation') !== null", 30_000L)
            driver.waitFor("document.getElementById('native-product-list') !== null", 30_000L)

            click(driver, "logout-button", "document.getElementById('login-button') !== null", 20_000L)
            capturePage(driver, "auth-login")
            driver.setInput("login-name", OWNER_LOGIN)
            driver.setInput("login-passphrase", "incorrect visual baseline passphrase")
            click(driver, "login-button", "document.body.innerText.includes('incorrect')", 20_000L)
            capturePage(driver, "auth-login-error")
            click(driver, "recover-owner-link", "document.getElementById('recover-owner-button') !== null")
            capturePage(driver, "auth-owner-recovery-form")
            click(driver, "cancel-recovery", "document.getElementById('login-button') !== null")
            driver.setInput("login-name", OWNER_LOGIN)
            driver.setInput("login-passphrase", OWNER_PASSPHRASE)
            click(driver, "login-button", "document.getElementById('app-navigation') !== null", 30_000L)

            captureDashboardAndNavigation(driver)
            captureDailyWork(driver)
            navigate(driver, "deliveries-nav", "deliveries", "document.getElementById('delivery-list') !== null")
            capturePage(driver, "deliveries-pending-and-create")
            captureAccounts(driver, seeded)
            captureHistory(driver, seeded)
            navigate(driver, "cash-close-nav", "cash-close", "document.getElementById('cash-count-form') !== null")
            capturePage(driver, "cash-close-count-and-history")

            scenario.recreate()
            driver = HybridWebViewTestDriver.attach(scenario)
            driver.waitFor("document.getElementById('app-navigation') !== null", 30_000L)
            captureProducts(driver, seeded)
            captureCustomers(driver, seeded)
            captureSuppliers(driver, seeded)
            captureEmployees(driver, seeded)
            captureCostingAndPlanning(driver)

            scenario.recreate()
            driver = HybridWebViewTestDriver.attach(scenario)
            driver.waitFor("document.getElementById('app-navigation') !== null", 30_000L)
            captureSettings(driver)
            captureAccess(driver)
            captureBackup(driver)

            manifestRows += "complete\t${captureNumber}-screenshots\tcomplete\t${Build.MANUFACTURER} ${Build.MODEL}\t${Build.VERSION.RELEASE}"
            syncManifest()
        } finally {
            scenario.close()
        }
    }

    private fun captureDashboardAndNavigation(driver: HybridWebViewTestDriver) {
        navigate(driver, "dashboard-nav", "dashboard", "document.getElementById('run-report-button') !== null")
        capturePage(driver, "dashboard-reports")
        click(driver, "navigation-more-button", "document.getElementById('navigation-more-layer').getAttribute('aria-hidden') === 'false'")
        capturePanel(driver, "navigation-more-menu", "#navigation-more-panel")
        click(driver, "close-navigation-more", "document.getElementById('navigation-more-layer').getAttribute('aria-hidden') === 'true'")
    }

    private fun captureDailyWork(driver: HybridWebViewTestDriver) {
        navigate(driver, "operations-nav", "operations", "document.getElementById('purchase-work-tab') !== null")
        val states = listOf(
            Triple("purchase-work-tab", "daily-work-receive-milk", "record-purchase-button"),
            Triple("sale-work-tab", "daily-work-record-sale", "record-sale-button"),
            Triple("production-work-tab", "daily-work-make-yogurt", "record-production-button"),
            Triple("spoilage-work-tab", "daily-work-spoilage", "record-spoilage-button"),
            Triple("expense-work-tab", "daily-work-expense", "record-expense-button"),
            Triple("payments-work-tab", "daily-work-payments", "record-payment-button"),
            Triple("salary-work-tab", "daily-work-pay-salary", "record-salary-button"),
        )
        states.forEach { (tab, slug, readyId) ->
            click(driver, tab, "document.getElementById('$readyId') !== null")
            capturePage(driver, slug)
        }
    }

    private fun captureAccounts(driver: HybridWebViewTestDriver, seeded: SeededIds) {
        navigate(driver, "accounts-nav", "accounts", "document.getElementById('account-balance-list') !== null")
        capturePage(driver, "accounts-customer-balances")
        click(driver, "ledger-account-${seeded.customerId}", "document.getElementById('account-ledger-detail') !== null", 20_000L)
        capturePage(driver, "accounts-customer-ledger-detail")
        click(driver, "ledger-back-button", "document.getElementById('account-balance-list') !== null")
        click(driver, "account-supplier-tab", "document.getElementById('ledger-account-${seeded.supplierId}') !== null")
        capturePage(driver, "accounts-supplier-balances")
        click(driver, "accounts-payments-tab", "document.getElementById('record-payment-button') !== null")
        capturePage(driver, "accounts-record-payment")
    }

    private fun captureHistory(driver: HybridWebViewTestDriver, seeded: SeededIds) {
        navigate(driver, "history-nav", "history", "document.getElementById('history-list') !== null")
        capturePage(driver, "history-transaction-list")
        click(driver, "history-event-${seeded.expenseEventId}", "document.getElementById('history-detail') !== null", 20_000L)
        capturePage(driver, "history-expense-detail")
        click(driver, "correct-event-${seeded.expenseEventId}", "document.getElementById('correction-form') !== null")
        captureVisibleDialog(driver, "history-correction-dialog")
        closeVisibleDialog(driver)
    }

    private fun captureProducts(driver: HybridWebViewTestDriver, seeded: SeededIds) {
        navigate(driver, "products-nav", "products", "document.getElementById('native-product-list') !== null")
        capturePage(driver, "products-active")
        click(driver, "add-product-button", "document.getElementById('save-product-button') !== null")
        captureVisibleDialog(driver, "products-add-dialog")
        closeVisibleDialog(driver)
        click(driver, "archive-product-${seeded.milkProductId}", "document.getElementById('archive-confirmation') !== null")
        captureVisibleDialog(driver, "products-archive-confirmation")
        closeVisibleDialog(driver)
        click(driver, "archived-products-tab", "document.getElementById('restore-product-${seeded.archivedProductId}') !== null")
        capturePage(driver, "products-archived")
    }

    private fun captureCustomers(driver: HybridWebViewTestDriver, seeded: SeededIds) {
        navigate(driver, "customers-nav", "customers", "document.getElementById('native-customer-list') !== null")
        capturePage(driver, "customers-active")
        click(driver, "add-customer-button", "document.getElementById('save-customer-button') !== null")
        captureVisibleDialog(driver, "customers-add-dialog")
        closeVisibleDialog(driver)
        click(driver, "deactivate-customer-${seeded.customerId}", "document.getElementById('customer-deactivate-confirmation') !== null")
        captureVisibleDialog(driver, "customers-deactivate-confirmation")
        closeVisibleDialog(driver)
        click(driver, "inactive-customers-tab", "document.getElementById('restore-customer-${seeded.inactiveCustomerId}') !== null")
        capturePage(driver, "customers-inactive")
        click(driver, "active-customers-tab", "document.getElementById('deactivate-customer-${seeded.customerId}') !== null")
        driver.setInput("customer-search", "no matching customer")
        driver.waitFor("document.getElementById('native-customer-list').innerText.includes('No customers match')")
        capturePage(driver, "customers-empty-search-result")
        driver.setInput("customer-search", "")
    }

    private fun captureSuppliers(driver: HybridWebViewTestDriver, seeded: SeededIds) {
        navigate(driver, "suppliers-nav", "suppliers", "document.getElementById('native-supplier-list') !== null")
        capturePage(driver, "suppliers-active")
        click(driver, "add-supplier-button", "document.getElementById('save-supplier-button') !== null")
        captureVisibleDialog(driver, "suppliers-add-dialog")
        closeVisibleDialog(driver)
        click(driver, "deactivate-supplier-${seeded.supplierId}", "document.getElementById('supplier-deactivate-confirmation') !== null")
        captureVisibleDialog(driver, "suppliers-deactivate-confirmation")
        closeVisibleDialog(driver)
        click(driver, "inactive-suppliers-tab", "document.getElementById('restore-supplier-${seeded.inactiveSupplierId}') !== null")
        capturePage(driver, "suppliers-inactive")
    }

    private fun captureEmployees(driver: HybridWebViewTestDriver, seeded: SeededIds) {
        navigate(driver, "employees-nav", "employees", "document.getElementById('native-employee-list') !== null")
        capturePage(driver, "employees-active-profiles")
        click(driver, "add-employee-button", "document.getElementById('save-employee-button') !== null")
        captureVisibleDialog(driver, "employees-add-dialog")
        closeVisibleDialog(driver)
        click(driver, "deactivate-employee-${seeded.employeeId}", "document.getElementById('employee-deactivate-confirmation') !== null")
        captureVisibleDialog(driver, "employees-deactivate-confirmation")
        closeVisibleDialog(driver)
        click(driver, "inactive-employees-tab", "document.getElementById('restore-employee-${seeded.inactiveEmployeeId}') !== null")
        capturePage(driver, "employees-inactive-profiles")
        click(driver, "attendance-tab", "document.getElementById('native-attendance-list') !== null", 20_000L)
        capturePage(driver, "employees-daily-attendance")
    }

    private fun captureCostingAndPlanning(driver: HybridWebViewTestDriver) {
        navigate(driver, "costing-planning-nav", "costing-planning", "document.getElementById('costing-planning-workspace') !== null")
        val states = listOf(
            Triple("planning-tab", "cost-plans-plans-and-targets", "costing-planning-workspace"),
            Triple("yogurt-cost-tab", "cost-plans-yogurt-cost", "costing-planning-workspace"),
            Triple("opening-cost-tab", "cost-plans-opening-stock-cost", "costing-planning-workspace"),
            Triple("yield-review-tab", "cost-plans-yield-and-loss", "costing-planning-workspace"),
        )
        states.forEach { (tab, slug, readyId) ->
            click(driver, tab, "document.getElementById('$readyId') !== null")
            capturePage(driver, slug)
        }
    }

    private fun captureSettings(driver: HybridWebViewTestDriver) {
        navigate(driver, "settings-nav", "settings", "document.getElementById('business-settings-tab') !== null")
        click(driver, "business-settings-tab", "document.getElementById('save-store-identity-button') !== null")
        capturePage(driver, "settings-shop-details-and-rates")
        click(driver, "opening-state-tab", "document.getElementById('opening-date') !== null || document.getElementById('correct-opening-state-button') !== null")
        capturePage(driver, "settings-opening-balances")
        click(driver, "expense-categories-tab", "document.getElementById('native-expense-category-list') !== null")
        capturePage(driver, "settings-expense-categories-active")
        click(driver, "add-expense-category-button", "document.getElementById('save-expense-category-button') !== null")
        captureVisibleDialog(driver, "settings-add-expense-category-dialog")
        closeVisibleDialog(driver)
        click(driver, "inactive-expense-categories-tab", "document.getElementById('native-expense-category-list') !== null")
        capturePage(driver, "settings-expense-categories-inactive")
    }

    private fun captureAccess(driver: HybridWebViewTestDriver) {
        navigate(driver, "access-nav", "access", "document.getElementById('security-staff-tab') !== null")
        driver.waitFor("document.getElementById('new-user-form') !== null", 20_000L)
        capturePage(driver, "access-staff-accounts")
        click(driver, "security-account-tab", "document.getElementById('change-own-passphrase-form') !== null")
        capturePage(driver, "access-my-account")
        click(driver, "security-audit-tab", "document.getElementById('security-audit-list') !== null")
        capturePage(driver, "access-security-history")
        click(driver, "audit-filter-failed", "document.getElementById('security-audit-list') !== null")
        capturePage(driver, "access-security-history-empty-filter")
    }

    private fun captureBackup(driver: HybridWebViewTestDriver) {
        navigate(driver, "backup-nav", "backup", "document.getElementById('backup-create-tab') !== null")
        driver.waitFor("document.getElementById('encrypted-backup-form') !== null", 20_000L)
        capturePage(driver, "backup-manual-encrypted-file")
        click(driver, "backup-cloud-tab", "document.getElementById('connect-cloud-backup-form') !== null", 20_000L)
        capturePage(driver, "backup-cloud-not-connected")
        click(driver, "backup-restore-tab", "document.getElementById('inspect-backup-form') !== null")
        capturePage(driver, "backup-restore-file")
    }

    private suspend fun seedRepresentativeShop(app: MilkShopApplication): SeededIds {
        val database = app.database
        val today = LocalDate.now().toString()
        StoreIdentityService(database).save(StoreIdentityInput("Saaf Milk Model Shop", "+92 300 555 0184", "Model Town, Lahore"))

        val supplierId = VendorManagementService(database).create("Rehman Dairy Farm")
        VendorProfileService(database).save(supplierId, VendorProfileInput("+92 301 555 0101", "Raiwind Road", "Morning milk route"))
        val inactiveSupplierId = VendorManagementService(database).create("Old Canal Dairy")
        VendorProfileService(database).save(inactiveSupplierId, VendorProfileInput(location = "Canal Road", active = false))

        val customerId = CustomerManagementService(database).create("Noor General Store")
        CustomerProfileService(database).save(customerId, CustomerProfileInput("+92 302 555 0122", "Route B", "Main Market", "Collect every Friday", 25_000.0, 12.0))
        val inactiveCustomerId = CustomerManagementService(database).create("Former Walk-in Customer")
        CustomerProfileService(database).save(inactiveCustomerId, CustomerProfileInput(areaRoute = "Old Route", active = false))

        val employeeId = EmployeeManagementService(database).create("Bilal Ahmed")
        EmployeeProfileService(database).save(employeeId, EmployeeProfileInput("Delivery rider", "+92 303 555 0144", 35_000.0, "monthly", today, true))
        val inactiveEmployeeId = EmployeeManagementService(database).create("Former Shop Helper")
        EmployeeProfileService(database).save(inactiveEmployeeId, EmployeeProfileInput(role = "Shop helper", active = false))

        val packagingCategoryId = ExpenseCategoryManagementService(database).create("Packaging")
        val inactiveCategoryId = ExpenseCategoryManagementService(database).create("Legacy cleaning")
        ExpenseCategoryManagementService(database).setArchived(inactiveCategoryId, true)

        val catalog = ProductCatalogService(database)
        val archivedProductId = catalog.create(ProductCatalogInput("Seasonal cream", "Kg", 650.0, true, "Seasonal line"))
        catalog.setArchived(archivedProductId, true)
        val milkProductId = requireNotNull(database.productDao().findByName("Milk")).productId
        val cashId = database.accountDao().findByCategory("Cash/Bank").single { it.name == "Cash" }.accountId

        val eventIds = mutableListOf<Long>()
        eventIds += VendorPurchaseService(database).record(VendorPurchaseInput(supplierId, today, 120.0, 4.2, 28.5, "Morning collection")).eventId
        eventIds += YogurtProductionService(database).record(YogurtProductionInput(today, 25.0, 21.0, "Morning yogurt batch")).eventId
        eventIds += SaleService(database).record(SaleInput(milkProductId, today, "retail", 220.0, quantity = 18.0, payment = SalePayment.Account(cashId), surchargeEnabled = true, notes = "Counter sales")).eventId
        val creditSale = SaleService(database).record(SaleInput(milkProductId, today, "credit-retail", 215.0, quantity = 10.0, payment = SalePayment.OnCredit(customerId), notes = "Route B delivery sale"))
        eventIds += creditSale.eventId
        eventIds += SpoilageService(database).record(SpoilageInput(milkProductId, 1.5, today, "spill", "Container slipped during transfer")).eventId
        val expense = ExpenseService(database).record(ExpenseInput("Packaging", cashId, 1_250.0, today, "Cups and carry bags"))
        eventIds += expense.eventId
        eventIds += VendorPaymentService(database).record(VendorPaymentInput(supplierId, cashId, 8_000.0, today, "Part payment after collection")).eventId
        eventIds += CreditCollectionService(database).record(CreditCollectionInput(customerId, cashId, 1_000.0, today, "Friday route collection")).eventId
        eventIds += SalaryPaymentService(database).record(SalaryPaymentInput(employeeId, cashId, 2_000.0, today, "Weekly advance")).eventId
        DeliveryService(database).create(DeliveryInput(customerId, milkProductId, 10.0, "Route B", today, creditSale.eventId, "Deliver before noon"))
        AttendanceService(database).record(AttendanceInput(employeeId, today, "present", "Morning shift"))
        PhysicalCashCountService(database).record(PhysicalCashCountInput(cashId, today, 4_600.0, "Opening and counter cash counted"))

        val sessionId = requireNotNull(app.currentSessionId)
        val ownerSession = app.nativeSessions.require(sessionId, touch = false)
        val owner = requireNotNull(database.appUserDao().findById(ownerSession.userId))
        eventIds.forEach { eventId ->
            database.eventActorLinkDao().insert(EventActorLinkEntity(eventId, owner.userId, owner.displayName, owner.role, Instant.now().toString()))
        }
        val staff = app.userSecurity.beginUserEnrollment(sessionId, CreateUserInput("bilal.cashier", "Bilal Ahmed", "cashier", employeeId))
        app.userSecurity.completeUserEnrollment(staff.enrollmentToken, "bilal private visual passphrase 2026".toCharArray())

        return SeededIds(
            supplierId = supplierId,
            inactiveSupplierId = inactiveSupplierId,
            customerId = customerId,
            inactiveCustomerId = inactiveCustomerId,
            employeeId = employeeId,
            inactiveEmployeeId = inactiveEmployeeId,
            milkProductId = milkProductId,
            archivedProductId = archivedProductId,
            expenseCategoryId = packagingCategoryId,
            expenseEventId = expense.eventId,
        )
    }

    private fun navigate(driver: HybridWebViewTestDriver, navId: String, area: String, ready: String) {
        click(
            driver,
            navId,
            "document.querySelector('[data-app-area=\"$area\"][data-app-area-active=\"true\"]') !== null && ($ready)",
            30_000L,
        )
    }

    private fun click(driver: HybridWebViewTestDriver, id: String, ready: String, timeoutMs: Long = 10_000L) {
        val quoted = JSONObject.quote(id)
        assertTrue("Missing element $id", driver.evaluate("document.getElementById($quoted) !== null") == "true")
        driver.evaluate("document.getElementById($quoted).click(); true")
        driver.waitFor(ready, timeoutMs)
        settle()
    }

    private fun closeVisibleDialog(driver: HybridWebViewTestDriver) {
        val closed = driver.evaluate(
            """(() => { const button = document.querySelector('.hybrid-overlay:not([aria-hidden]) .hybrid-dialog-panel button[data-native-back-close]'); if (!button) return false; button.click(); return true; })()""",
        )
        assertTrue("Visible dialog did not expose a close button", closed == "true")
        driver.waitFor("document.querySelector('.hybrid-overlay:not([aria-hidden=\"true\"]) .hybrid-dialog-panel') === null")
        settle()
    }

    private fun capturePage(driver: HybridWebViewTestDriver, state: String) {
        driver.evaluate("document.activeElement?.blur(); window.scrollTo(0, 0); true")
        settle()
        saveScreenshot(driver, state, "top")
        val tall = driver.evaluate("document.scrollingElement.scrollHeight > window.innerHeight + 120") == "true"
        if (tall) {
            driver.evaluate("window.scrollTo(0, document.scrollingElement.scrollHeight); true")
            settle()
            saveScreenshot(driver, state, "bottom")
        }
    }

    private fun captureVisibleDialog(driver: HybridWebViewTestDriver, state: String) =
        capturePanel(driver, state, ".hybrid-overlay:not([aria-hidden=\"true\"]) .hybrid-dialog-panel")

    private fun capturePanel(driver: HybridWebViewTestDriver, state: String, selector: String) {
        val quoted = JSONObject.quote(selector)
        driver.waitFor("document.querySelector($quoted) !== null")
        driver.evaluate("(() => { const panel = document.querySelector($quoted); document.activeElement?.blur(); panel.scrollTop = 0; return true; })()")
        settle()
        saveScreenshot(driver, state, "top")
        val tall = driver.evaluate("(() => { const panel = document.querySelector($quoted); return panel.scrollHeight > panel.clientHeight + 80; })()") == "true"
        if (tall) {
            driver.evaluate("(() => { const panel = document.querySelector($quoted); panel.scrollTop = panel.scrollHeight; return true; })()")
            settle()
            saveScreenshot(driver, state, "bottom")
        }
    }

    private fun saveScreenshot(driver: HybridWebViewTestDriver, state: String, position: String) {
        captureNumber += 1
        val filename = "%03d-%s-%s.png".format(captureNumber, state, position)
        val visualStateReady = CountDownLatch(1)
        instrumentation.runOnMainSync {
            driver.webView.postVisualStateCallback(
                captureNumber.toLong(),
                object : WebView.VisualStateCallback() {
                    override fun onComplete(requestId: Long) {
                        driver.webView.postInvalidateOnAnimation()
                        visualStateReady.countDown()
                    }
                },
            )
        }
        assertTrue("WebView did not commit $state for capture", visualStateReady.await(5, TimeUnit.SECONDS))
        instrumentation.waitForIdleSync()
        SystemClock.sleep(350)
        val bitmap = driver.captureWindow()
        try {
            val captureFile = File(outputDirectory, filename)
            FileOutputStream(captureFile).use { output ->
                assertTrue(
                    "Could not encode $filename",
                    bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, output),
                )
            }
            executeShell("cp ${captureFile.absolutePath} $PUBLIC_OUTPUT_DIRECTORY/$filename")
        } finally {
            bitmap.recycle()
        }
        manifestRows += "$filename\t$state\t$position\t${Build.MANUFACTURER} ${Build.MODEL}\t${Build.VERSION.RELEASE}"
        syncManifest()
    }

    private fun syncManifest() {
        manifestFile.writeText(manifestRows.joinToString(separator = "\n", postfix = "\n"))
        executeShell("cp ${manifestFile.absolutePath} $PUBLIC_OUTPUT_DIRECTORY/manifest.tsv")
    }

    private fun preparePublicEvidenceDirectory() {
        executeShell("rm -rf $PUBLIC_OUTPUT_DIRECTORY")
        executeShell("mkdir -p $PUBLIC_OUTPUT_DIRECTORY")
    }

    private fun executeShell(command: String) {
        ParcelFileDescriptor.AutoCloseInputStream(instrumentation.uiAutomation.executeShellCommand(command)).use {
            it.readBytes()
        }
    }

    private fun wakeDevice() {
        instrumentation.uiAutomation.executeShellCommand("input keyevent KEYCODE_WAKEUP").close()
        instrumentation.uiAutomation.executeShellCommand("wm dismiss-keyguard").close()
        SystemClock.sleep(300)
    }

    private fun settle() = SystemClock.sleep(550)

    private data class SeededIds(
        val supplierId: Long,
        val inactiveSupplierId: Long,
        val customerId: Long,
        val inactiveCustomerId: Long,
        val employeeId: Long,
        val inactiveEmployeeId: Long,
        val milkProductId: Long,
        val archivedProductId: Long,
        val expenseCategoryId: Long,
        val expenseEventId: Long,
    )

    private companion object {
        const val OUTPUT_DIRECTORY = "phase12-baseline"
        const val PUBLIC_OUTPUT_DIRECTORY = "/sdcard/Download/$OUTPUT_DIRECTORY"
        const val OWNER_LOGIN = "visual.owner"
        const val OWNER_DISPLAY_NAME = "Ayesha Khan"
        const val OWNER_PASSPHRASE = "ayesha private visual passphrase 2026"
    }
}
