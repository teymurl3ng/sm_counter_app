package com.saaf.milkshop

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.saaf.milkshop.application.cashclose.*
import android.content.ContextWrapper
import androidx.room3.executeSQL
import androidx.room3.withWriteTransaction
import androidx.test.platform.app.InstrumentationRegistry
import com.saaf.milkshop.application.commands.OperationsBridgeCommandService
import com.saaf.milkshop.application.owner.NativeOwnerSyncFacade
import com.saaf.milkshop.data.*
import com.saaf.milkshop.domain.*
import com.saaf.milkshop.sync.*
import java.io.File
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.runBlocking
import android.os.Process
import org.json.JSONObject
import org.junit.Test
import org.junit.Assert.*

/** Staged real DEV transport, a distinct private database on each physical phone. */
class PhysicalOfflineCloseProbe {
    @Test fun runStage() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val base = instrumentation.targetContext
        check(base.packageName == "com.saaf.milkshop.qa.tests")
        val args = InstrumentationRegistry.getArguments()
        val run = args.getString("qaRun")!!
        check(run.matches(Regex("[a-f0-9]{12}")))
        val phone = args.getString("qaPhone")!!
        check(phone in setOf("owner", "counter"))
        val stage = args.getString("qaStage")!!
        val root = File(base.filesDir, "two-phone-$run").apply { mkdirs() }
        val context = FixtureContext(base, root)
        SyncScheduler.suppressSchedulingForTest()
        SyncIdentity.warmUp(context)
        val db = MilkShopDatabase.create(context)
        val security = UserSecurityService(db)
        val output = JSONObject().put("run", run).put("phone", phone).put("stage", stage)
        val calls = mutableListOf<String>()
        var discardedPushResponses = 0
        val actualHttp = UrlConnectionSyncHttp()
        var expectedShop: String? = SyncShopBindingStore.load(context)?.shopId
        val http = SyncHttp { request ->
            check(request.url.startsWith(DEV + "/"))
            if (request.url.endsWith("sync_push") || request.url.endsWith("sync_pull")) {
                val field = if (request.url.endsWith("sync_pull")) "shop" else "shop_id"
                check(JSONObject(String(request.body!!)).getString(field) == expectedShop)
            }
            try {
                val response = actualHttp.execute(request)
                calls += request.url.substringAfter(DEV) + ":" + response.status
                if (stage == "lost_ack_push" && request.url.endsWith("sync_push")) {
                    assertTrue(response.status in 200..299)
                    assertTrue(JSONObject(response.bodyText()!!).getBoolean("ok"))
                    discardedPushResponses += 1
                    throw java.io.IOException("Synthetic missing successful push response")
                }
                response
            } catch (failure: java.io.IOException) {
                calls += request.url.substringAfter(DEV) + ":transport_" + failure.javaClass.simpleName
                throw failure
            }
        }
        val day = LocalDate.parse(args.getString("qaDay")!!)
        try {
            when (stage) {
                "lost_ack_push" -> {
                    check(phone == "counter")
                    val session = security.login("two.cashier", CASHIER_PHRASE.toCharArray())
                    val milk = db.productDao().findByName("Milk")!!
                    val cash = db.accountDao().findByCategory("Cash/Bank").single { it.name == "Cash" }
                    val sale = OperationsBridgeCommandService(db, security).recordSale(session.sessionId,
                        SaleInput(milk.productId, day.toString(), "retail", 190.0, quantity = 1.0,
                            payment = SalePayment.Account(cash.accountId)))
                    assertEquals(190.0, sale.total, 0.0)
                    val queue = db.syncOutboxDao().all()
                    val events = db.backupDataDao().events()
                    val ledger = db.backupDataDao().financialLogs()
                    val inventory = db.backupDataDao().inventoryLogs()
                    val drain = SyncRuntime(context, db, http).drainOnce()
                    assertTrue(drain is SyncPushEngine.DrainOutcome.RetryLater)
                    assertEquals(1, discardedPushResponses)
                    assertEquals(1L, db.syncOutboxDao().count())
                    assertEquals(queue, db.syncOutboxDao().all())
                    assertEquals(events, db.backupDataDao().events())
                    assertEquals(ledger, db.backupDataDao().financialLogs())
                    assertEquals(inventory, db.backupDataDao().inventoryLogs())
                    output.put("eventUid", db.eventDao().findById(sale.eventId)!!.eventUid)
                        .put("discardedSuccessfulPushResponses", discardedPushResponses)
                        .put("queueAndLedgerPreserved", true).put("drain", drain.toString())
                }
                "queued_close_cycle" -> {
                    check(phone == "owner")
                    val cashier = security.login("two.cashier", CASHIER_PHRASE.toCharArray())
                    val facade = NativeCashCloseFacade(db, security)
                    assertEquals(1019000L, facade.load(cashier.sessionId, day).expectedCashPaisa)
                    val first = facade.recordCount(cashier.sessionId,
                        NativeCashCloseCountRequest(1019000L, "Queued first count"), day).count
                    facade.closeDay(cashier.sessionId, NativeCashCloseDayRequest(first.countId, null), day)
                    val owner = security.login("two.owner", OWNER_PHRASE.toCharArray())
                    facade.reopenDay(owner.sessionId, "Synthetic queued recount", OWNER_PHRASE.toCharArray(), day)
                    kotlinx.coroutines.delay(10)
                    val second = facade.recordCount(cashier.sessionId,
                        NativeCashCloseCountRequest(1019000L, "Queued fresh recount"), day).count
                    facade.closeDay(cashier.sessionId, NativeCashCloseDayRequest(second.countId, null), day)
                    assertEquals(5L, db.syncOutboxDao().count())
                    val envelopes = SyncEnvelopeFactory(db).build(db.syncOutboxDao().all()).envelopes
                    assertEquals(listOf("physical_cash_counts", "daily_close_actions", "daily_close_actions",
                        "physical_cash_counts", "daily_close_actions"), envelopes.map { it.getString("table") })
                    output.put("queuedWireTables", org.json.JSONArray(envelopes.map { it.getString("table") }))
                    assertTrue(db.dailyCloseDao().isPostingClosed(day.toString()))
                }
                "init" -> {
                    check(db.appUserDao().count() == 0L)
                    if (phone == "counter") db.withWriteTransaction {
                        listOf("accounts", "products", "events", "budget_plans", "collections_targets", "physical_cash_counts", "daily_close_actions", "app_users").forEach {
                            executeSQL("DELETE FROM sqlite_sequence WHERE name='$it'")
                            executeSQL("INSERT INTO sqlite_sequence(name,seq) VALUES('$it',1000)")
                        }
                    }
                    security.bootstrapOwner("two.owner", "Two Phone Owner", OWNER_PHRASE.toCharArray())
                    val owner = security.login("two.owner", OWNER_PHRASE.toCharArray())
                    val pending = security.beginUserEnrollment(owner.sessionId, CreateUserInput("two.cashier", "Two Phone Cashier", "cashier"))
                    security.completeUserEnrollment(pending.enrollmentToken, CASHIER_PHRASE.toCharArray())
                    if (phone == "owner") setup(db, owner.sessionId, security, day)
                }
                "enroll" -> {
                    val inputFile = File(root, "input.json")
                    val input = JSONObject(inputFile.readText())
                    inputFile.delete()
                    expectedShop = input.getString("shopId")
                    check(input.getString("baseUrl") == DEV)
                    val owner = security.login("two.owner", OWNER_PHRASE.toCharArray())
                    val result = SyncEnrollment(context, http, db).enroll(
                        SyncServerConfig("dev", DEV, input.getString("publishableKey")),
                        input.getString("username"), input.getString("credential").toCharArray(),
                        expectedShop, if (phone == "owner") owner.userId else null,
                    )
                    assertTrue("Enrollment failed", result is SyncEnrollment.Outcome.Enrolled)
                    assertEquals(if (phone == "owner") "owner" else "cashier", SyncShopBindingStore.load(context)!!.role)
                }
                "exchange" -> {
                    val runtime = SyncRuntime(context, db, http)
                    val drain = runtime.drainOnce()
                    output.put("drain", drain.toString())
                    assertTrue("Drain failed: $drain", drain is SyncPushEngine.DrainOutcome.Drained)
                    val pull = runtime.pullOnce()
                    output.put("pull", pull.toString())
                    assertTrue("Pull failed: $pull", pull is SyncRuntime.SyncPullSummary.Done)
                    assertEquals(0, (pull as SyncRuntime.SyncPullSummary.Done).skipped)
                    assertEquals(0, pull.conflicts)
                    assertEquals(0, db.syncOutboxDao().all().count { !it.held })
                    if (phone == "counter") {
                        val holds = db.syncRebaselineHoldDao().all()
                        val seedProducts = db.backupDataDao().products().filter { it.name in setOf("Milk", "Yogurt") }.map { it.uid }.toSet()
                        val seedAccounts = db.backupDataDao().accounts().filter { it.category in setOf("Cash/Bank", "Operating Expense") && it.name != "Expense: Cold Storage" }.map { it.uid }.toSet()
                        for (hold in holds) {
                            check((hold.tableName == "products" && hold.rowUid in seedProducts) ||
                                (hold.tableName == "accounts" && hold.rowUid in seedAccounts) ||
                                (hold.tableName == "settings" && hold.rowUid.startsWith("setting:")))
                            NativeOwnerSyncFacade(context, db).resolveHold(hold.tableName, hold.rowUid, keepMine = false)
                        }
                        output.put("seedHoldsReviewedTakeServer", holds.size)
                        val reviewedPull = runtime.pullOnce()
                        output.put("reviewedPull", reviewedPull.toString())
                        assertTrue("Reviewed pull failed: $reviewedPull", reviewedPull is SyncRuntime.SyncPullSummary.Done)
                        assertEquals(0, (reviewedPull as SyncRuntime.SyncPullSummary.Done).skipped)
                        assertEquals(0, reviewedPull.conflicts)
                    }
                    assertEquals(0, db.syncOutboxDao().count())
                    assertEquals(0, db.syncRebaselineHoldDao().count())
                    assertEquals(0, db.syncParkedRowDao().count())
                }
                "sale" -> {
                    check(phone == "counter")
                    val session = security.login("two.cashier", CASHIER_PHRASE.toCharArray())
                    val milk = db.productDao().findByName("Milk")!!
                    val cash = db.accountDao().findByCategory("Cash/Bank").single { it.name == "Cash" }
                    val result = OperationsBridgeCommandService(db, security).recordSale(session.sessionId,
                        SaleInput(milk.productId, day.toString(), "retail", 190.0, quantity = 2.0, payment = SalePayment.Account(cash.accountId)))
                    assertEquals(380.0, result.total, 0.000001)
                    output.put("eventUid", db.eventDao().findById(result.eventId)!!.eventUid)
                }
                "independent_sale" -> {
                    val session = security.login("two.cashier", CASHIER_PHRASE.toCharArray())
                    val milk = db.productDao().findByName("Milk")!!
                    val cash = db.accountDao().findByCategory("Cash/Bank").single { it.name == "Cash" }
                    val result = OperationsBridgeCommandService(db, security).recordSale(session.sessionId,
                        SaleInput(milk.productId, day.toString(), "retail", 190.0, quantity = 1.0,
                            payment = SalePayment.Account(cash.accountId)))
                    assertEquals(190.0, result.total, 0.0)
                    output.put("eventUid", db.eventDao().findById(result.eventId)!!.eventUid)
                    assertEquals(1L, db.syncOutboxDao().count())
                    assertEquals(10570.0, db.readModelDao().accountBalances().single { it.name == "Cash" }.balance, 0.0)
                    assertEquals(77.0, db.readModelDao().currentStock().single { it.name == "Milk" }.quantityOnHand, 0.0)
                }
                "offline_probe" -> {
                    check(phone == "owner")
                    val connectivity = base.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                    val validated = connectivity.allNetworks.count { network ->
                        connectivity.getNetworkCapabilities(network)?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true
                    }
                    output.put("validatedNetworks", validated)
                    assertEquals("Radio outage must be real", 0, validated)
                    val queue = db.syncOutboxDao().all()
                    val events = db.backupDataDao().events()
                    val ledger = db.backupDataDao().financialLogs()
                    val stock = db.backupDataDao().inventoryLogs()
                    val runtime = SyncRuntime(context, db, http)
                    val drain = runtime.drainOnce()
                    val pull = runtime.pullOnce()
                    output.put("drain", drain.toString()).put("pull", pull.toString())
                    assertTrue("Offline drain must retain sale: $drain", drain is SyncPushEngine.DrainOutcome.RetryLater)
                    assertTrue("Offline pull must refuse network: $pull", pull is SyncRuntime.SyncPullSummary.RetryLater)
                    assertTrue(calls.any { it.contains(":transport_") })
                    assertEquals(queue, db.syncOutboxDao().all())
                    assertEquals(events, db.backupDataDao().events())
                    assertEquals(ledger, db.backupDataDao().financialLogs())
                    assertEquals(stock, db.backupDataDao().inventoryLogs())
                    output.put("queueAndLedgerPreserved", true)
                }
                "cashier_close", "cashier_reclose" -> {
                    check(phone == "counter")
                    val session = security.login("two.cashier", CASHIER_PHRASE.toCharArray())
                    val facade = NativeCashCloseFacade(db, security)
                    assertEquals(1076000L, facade.load(session.sessionId, day).expectedCashPaisa)
                    val count = facade.recordCount(session.sessionId,
                        NativeCashCloseCountRequest(1076000L, "Synthetic physical drawer count"), day).count
                    val closed = facade.closeDay(session.sessionId, NativeCashCloseDayRequest(count.countId, null), day)
                    assertFalse(closed.refreshedContext.postingOpen)
                    assertEquals(0L, closed.receipt.variancePaisa)
                    assertTrue(db.dailyCloseDao().isPostingClosed(day.toString()))
                    output.put("countUid", db.physicalCashCountDao().findById(count.countId)!!.uid)
                        .put("actionUid", db.dailyCloseDao().currentAction(day.toString())!!.uid)
                }
                "closed_refusals" -> {
                    assertTrue(db.dailyCloseDao().isPostingClosed(day.toString()))
                    val session = security.login("two.cashier", CASHIER_PHRASE.toCharArray())
                    val milk = db.productDao().findByName("Milk")!!
                    val cash = db.accountDao().findByCategory("Cash/Bank").single { it.name == "Cash" }
                    val saleFailure = runCatching {
                        OperationsBridgeCommandService(db, security).recordSale(session.sessionId,
                            SaleInput(milk.productId, day.toString(), "retail", 190.0, quantity = 1.0,
                                payment = SalePayment.Account(cash.accountId)))
                    }.exceptionOrNull()
                    assertNotNull("Closed sale must fail", saleFailure)
                    assertTrue(saleFailure!!.message.orEmpty().contains("closed", ignoreCase = true))
                    val reopenFailure = runCatching {
                        NativeCashCloseFacade(db, security).reopenDay(session.sessionId,
                            "Cashier cannot reopen", CASHIER_PHRASE.toCharArray(), day)
                    }.exceptionOrNull()
                    assertTrue(reopenFailure is SecurityFailure.PermissionDenied)
                    output.put("closedSaleRefused", true).put("cashierReopenRefused", true)
                }
                "owner_reopen" -> {
                    check(phone == "owner")
                    val session = security.login("two.owner", OWNER_PHRASE.toCharArray())
                    val secret = OWNER_PHRASE.toCharArray()
                    NativeCashCloseFacade(db, security).reopenDay(session.sessionId,
                        "Synthetic verified drawer recount", secret, day)
                    assertTrue(secret.all { it == '\u0000' })
                    assertFalse(db.dailyCloseDao().isPostingClosed(day.toString()))
                    output.put("actionUid", db.dailyCloseDao().currentAction(day.toString())!!.uid)
                }
                "stale_count_refusal" -> {
                    assertFalse(db.dailyCloseDao().isPostingClosed(day.toString()))
                    val session = security.login("two.cashier", CASHIER_PHRASE.toCharArray())
                    val history = db.dailyCloseDao().history(day.toString())
                    assertEquals(listOf("close", "reopen"), history.map { it.action })
                    val failure = runCatching {
                        NativeCashCloseFacade(db, security).closeDay(session.sessionId,
                            NativeCashCloseDayRequest(history.first().physicalCashCountId!!, null), day)
                    }.exceptionOrNull()
                    assertNotNull("Old count must not reclose", failure)
                    assertTrue(failure!!.message.orEmpty().contains("count", ignoreCase = true))
                    output.put("preReopenCountRefused", true)
                }
                "status" -> Unit
                "cleanup" -> SyncEnrollment(context, http, db).unenroll()
                else -> error("Unknown stage")
            }
            output.put("httpStatuses", org.json.JSONArray(calls))
                .put("pid", Process.myPid())
                .put("deviceId", SyncIdentity.currentOrNull())
                .put("outbox", db.syncOutboxDao().count()).put("parks", db.syncParkedRowDao().count())
                .put("holds", db.syncRebaselineHoldDao().count())
                .put("postingClosed", db.dailyCloseDao().isPostingClosed(day.toString()))
            db.close()
            output.put("dbFilename", context.dbDir.listFiles()!!.single { it.name.endsWith(".db") }.name)
            File(root, "result.json").writeText(output.toString(2))
        } finally { db.close() }
    }

    private suspend fun setup(db: MilkShopDatabase, owner: String, security: UserSecurityService, date: LocalDate) {
        val now = "2026-10-13T00:00:00Z"
        val week = date.with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
        val vendor = VendorManagementService(db).create("Two Supplier")
        val customer = CustomerManagementService(db).create("Two Customer")
        db.vendorProfileDao().save(VendorProfileEntity(vendor, "03000000000", "QA market", "Weekly", true, now))
        db.customerProfileDao().save(CustomerProfileEntity(customer, "03000000001", "QA North", "QA street", "Morning", 500.0, 3.5, true, now))
        val employee = db.accountDao().insert(AccountEntity(name = "Salary: Two Employee", type = "Expense", category = "Salary"))
        db.employeeProfileDao().save(EmployeeProfileEntity(employee, "Driver", "03000000002", 40000.0, "month", "2026-10-01", true, now))
        val product = db.productDao().insert(ProductEntity(name = "Two Cream", unit = "Kg"))
        db.productProfileDao().save(ProductProfileEntity(product, 275.5, true, false, "QA rich", now))
        val expense = db.accountDao().insert(AccountEntity(name = "Expense: Cold Storage", type = "Expense", category = "Operating Expense"))
        db.expenseCategoryProfileDao().insert(ExpenseCategoryProfileEntity(expense, "Cold Storage", true, now))
        db.budgetPlanDao().insert(BudgetPlanEntity(category = "Milk purchases", periodStart = week.toString(),
            periodEndExclusive = week.plusDays(7).toString(), plannedLiters = 100.0, plannedAmount = 18000.0,
            vendorRateSnapshot = 180.0, retailRateSnapshot = 190.0, createdAt = now))
        db.collectionsTargetDao().insert(CollectionsTargetEntity(weekStart = week.toString(),
            weekEndExclusive = week.plusDays(7).toString(), targetAmount = 25000.0, createdAt = now))
        StoreIdentityService(db).save(StoreIdentityInput("Two Phone QA"))
        BusinessSettingsService(db).update(BusinessSettings(170.0, 11.0, 13.0, 190.0, 280.0, 50.0))
        val cash = db.accountDao().findByCategory("Cash/Bank").single { it.name == "Cash" }
        OpeningStateService(db).record(OpeningStateInput(date.toString(), accountBalances = listOf(OpeningAccountBalance(cash.accountId, 10_000.0))))
        OperationsBridgeCommandService(db, security).recordPurchase(owner, VendorPurchaseInput(vendor, date.toString(), 100.0, 4.0, 28.0))
        OperationsBridgeCommandService(db, security).recordProduction(owner, YogurtProductionInput(date.toString(), 20.0, 17.0))
    }

    private class FixtureContext(base: Context, root: File) : ContextWrapper(base) {
        val dbDir = File(root, "db").apply { mkdirs() }
        private val noBackup = File(root, "no-backup").apply { mkdirs() }
        override fun getApplicationContext(): Context = this
        override fun getDatabasePath(name: String): File = File(dbDir, name)
        override fun getNoBackupFilesDir(): File = noBackup
    }
    companion object {
        private const val DEV = "https://tghzmofkkdsaasvnixps.supabase.co"
        private const val OWNER_PHRASE = "synthetic two phone owner phrase 2026"
        private const val CASHIER_PHRASE = "synthetic two phone cashier phrase 2026"
    }
}
