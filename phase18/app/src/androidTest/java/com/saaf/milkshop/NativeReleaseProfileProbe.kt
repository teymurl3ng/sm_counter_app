package com.saaf.milkshop

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import android.graphics.Bitmap
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.SystemClock
import android.util.Log
import android.view.FrameMetrics
import android.view.PixelCopy
import androidx.activity.enableEdgeToEdge
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.room3.withWriteTransaction
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.saaf.milkshop.application.authentication.NativeSignedInIdentity
import com.saaf.milkshop.application.collection.*
import com.saaf.milkshop.application.owner.*
import com.saaf.milkshop.application.sale.*
import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.domain.*
import com.saaf.milkshop.nativeui.common.*
import com.saaf.milkshop.nativeui.owner.*
import com.saaf.milkshop.nativeui.sale.*
import com.saaf.milkshop.nativeui.theme.SaafTheme
import com.saaf.milkshop.sync.BusinessClock
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Before
import org.junit.Assume
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.Collections
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.math.ceil

/** Release-derived, isolated benchmark observations; no owner visual approval or production budget claim. */
@RunWith(AndroidJUnit4::class)
class NativeReleaseProfileProbe {
    @Before fun requireIsolatedBenchmark() {
        Assume.assumeTrue("Release profile requires the isolated benchmark build", BuildConfig.BUILD_TYPE == "benchmark")
    }
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val output get() = File(requireNotNull(context.getExternalFilesDir(null)), "audit_release_profile").also { it.mkdirs() }
    private fun save(name: String, json: JSONObject) {
        check(context.packageName == "com.saaf.milkshop.qa.benchmark")
        val file = File(output, name)
        file.writeText(json.put("captured_at", java.time.Instant.now().toString()).toString(2))
        exportMotionEvidence(file)
        Log.i("SaafAuditProfile", json.toString())
    }
    private fun exportMotionEvidence(file: File) {
        check(context.packageName == "com.saaf.milkshop.qa.benchmark")
        val device = androidx.test.uiautomator.UiDevice.getInstance(instrumentation)
        val destination = "/sdcard/Download/phase14-native-evidence/native-motion-a06"
        device.executeShellCommand("mkdir -p $destination")
        device.executeShellCommand("cp ${file.absolutePath} $destination/${file.name}")
        assertEquals(file.length(), device.executeShellCommand("stat -c %s $destination/${file.name}").trim().toLong())
    }

    private fun distribution(values: List<Double>): JSONObject {
        val sorted = values.sorted()
        fun q(p: Double) = sorted[(ceil(sorted.size * p).toInt() - 1).coerceIn(sorted.indices)]
        return JSONObject().put("samples", sorted.size).put("median_ms", q(.5))
            .put("p95_ms", q(.95)).put("max_ms", sorted.last()).put("values_ms", JSONArray(values))
    }

    @Test fun profilePersistentMixedCostingDataset() = runBlocking {
        check(context.packageName == "com.saaf.milkshop.qa.benchmark")
        check(BuildConfig.BUILD_TYPE == "benchmark")
        val fixtureRoot = File(context.filesDir, "audit_release_database").also { it.mkdirs() }
        val isolatedContext = object : android.content.ContextWrapper(context) {
            override fun getApplicationContext(): android.content.Context = this
            override fun getDatabasePath(name: String): File = File(fixtureRoot, name)
        }
        val db = MilkShopDatabase.create(isolatedContext)
        try {
            val today = BusinessClock.today()
            val security = UserSecurityService(db)
            val session = security.bootstrapOwner("profile.owner", "Profile Owner",
                "synthetic native performance owner 2026".toCharArray()).session.sessionId
            val customers = (1..100).map { CustomerManagementService(db).create("Synthetic customer $it") }
            val cash = db.accountDao().findByCategory("Cash/Bank").single { it.name == "Cash" }.accountId
            val vendor = VendorManagementService(db).create("Synthetic milk farm")
            VendorPurchaseService(db).record(VendorPurchaseInput(vendor, today.toString(), 1000.0, 4.0, 28.0))
            val policy = CostingPolicyService(db, security).approve(session, CostingPolicyInput(
                calendarMonth = java.time.YearMonth.from(today).toString(),
                normalCapacityKg = java.math.BigDecimal("1000"), reviewReason = "monthly",
                components = CostingPolicyService.COMPONENT_CODES.map { ConversionCostComponentInput(it, 100000, java.math.BigDecimal("1000")) }))
            val seedStarted = SystemClock.elapsedRealtimeNanos()
            db.withWriteTransaction {
                repeat(10_000) { i -> CreditCollectionService(db).record(
                    CreditCollectionInput(customers[i % customers.size], cash, 100.0, today.toString())) }
            }
            val seedMs = (SystemClock.elapsedRealtimeNanos() - seedStarted) / 1_000_000.0
            db.withWriteTransaction {
                repeat(20) { VendorPurchaseService(db).record(VendorPurchaseInput(vendor, today.toString(), 1000.0, 4.0, 28.0)) }
                repeat(40) { YogurtProductionService(db).record(YogurtProductionInput(today.toString(), 5.0, 5.0)) }
                val yogurt = db.productDao().findByName("Yogurt")!!.productId
                repeat(20) { NativeSaleFacade(db, security).record(session, NativeSaleRequest(yogurt, NativeSaleEntryMode.QUANTITY,
                    1.0, NativeSalePaymentKind.PAID_NOW, cash, null), today) }
            }
            assertEquals(10_081, db.eventDao().count())
            assertTrue("A real persistent Room fixture file must exist", fixtureRoot.listFiles().orEmpty().any { it.isFile && it.name.endsWith(".db") })
            val costing = NativeOwnerCostingPlansFacade(db, security)
            val yieldMs = mutableListOf<Double>()
            repeat(4) { sample ->
                val began = SystemClock.elapsedRealtimeNanos()
                val yields = costing.loadYieldContext(session)
                assertEquals(40, yields.batches.size)
                if (sample > 0) yieldMs += (SystemClock.elapsedRealtimeNanos() - began) / 1_000_000.0
            }
            assertEquals(policy.version.policyId, costing.loadPolicies(session, today.toString()).policies.single().policyId)
            val collection = NativeCollectionFacade(db, security)
            val sale = NativeSaleFacade(db, security)
            val milk = db.productDao().findByName("Milk")!!.productId
            val reports = NativeOwnerReportsFacade(db, security)
            val collectionMs = mutableListOf<Double>()
            val saleMs = mutableListOf<Double>()
            val reportsMs = mutableListOf<Double>()
            repeat(22) { i ->
                var start = SystemClock.elapsedRealtimeNanos()
                val collected = collection.record(session, NativeCollectionRequest(customers[0], cash, 100, null), today)
                val elapsedCollection = (SystemClock.elapsedRealtimeNanos() - start) / 1_000_000.0
                assertEquals(100L, collected.receipt.amountPaisa)
                start = SystemClock.elapsedRealtimeNanos()
                val sold = sale.record(session, NativeSaleRequest(milk, NativeSaleEntryMode.QUANTITY, 1.0,
                    NativeSalePaymentKind.PAID_NOW, cash, null), today)
                val elapsedSale = (SystemClock.elapsedRealtimeNanos() - start) / 1_000_000.0
                assertEquals(1.0, sold.receipt.quantity, .000001)
                start = SystemClock.elapsedRealtimeNanos()
                reports.load(session, today.withDayOfMonth(1).toString(), today.withDayOfMonth(1).plusMonths(1).toString(),
                    NativeOwnerReportsPreset.ThisMonth, today)
                val elapsedReports = (SystemClock.elapsedRealtimeNanos() - start) / 1_000_000.0
                if (i >= 2) { collectionMs += elapsedCollection; saleMs += elapsedSale; reportsMs += elapsedReports }
            }
            assertEquals(10_125, db.eventDao().count())
            save("native-data-profile.json", JSONObject().put("build", BuildConfig.BUILD_TYPE)
                .put("device", Build.MODEL).put("dataset", "Persistent bundled Room; 100 customers; 10000 collections; 21 purchases; 40 yogurt batches; 20 yogurt sales; one costing policy")
                .put("storage", "persistent isolated Room fixture").put("warmups_per_operation", 2).put("collection_seed_ms", seedMs).put("yield_context_40_batches", distribution(yieldMs))
                .put("collection_command_and_receipt", distribution(collectionMs))
                .put("sale_command_and_receipt", distribution(saleMs)).put("month_reports", distribution(reportsMs)))
        } finally { db.close() }
    }

    @Test fun profileRealTimeNativeTransitionFramesAndGuardOwnership() {
        check(context.packageName == "com.saaf.milkshop.qa.benchmark")
        // ActivityScenario uses the real Choreographer, without Compose's test
        // clock auto-advancing animation frames to completion.
        var reportsPane by mutableStateOf(false)
        var active: EntryLeaveGuard? = null
        val paneGuards = mutableMapOf<Boolean, EntryLeaveGuard>()
        data class Frame(val totalNs: Long, val deadlineNs: Long, val layoutNs: Long, val drawNs: Long)
        val frames = Collections.synchronizedList(mutableListOf<Frame>())
        val handlerThread = HandlerThread("audit-frame-metrics").also { it.start() }
        val identity = NativeSignedInIdentity(1, "profile", "Profile Owner", AppRole.OWNER, RolePermissions.permissions(AppRole.OWNER))
        val date = BusinessClock.today().toString()
        val saleContext = NativeSaleContext(date, true, listOf(NativeSaleProduct(1, "Milk", "Liters", 19000, "20 L")),
            emptyList(), listOf(NativeSaleAccount(10, "Cash")), 10, 0, 0, 50.0)
        val reportSnapshot = NativeOwnerReportsSnapshot("Saaf Milk", date, true, NativeOwnerReportsPreset.ThisMonth,
            "This month", date, date, NativeOwnerReportsCoverageKind.Complete, "Cost history complete",
            "Synthetic motion fixture", "PKR 12.00", "PKR 100.00", "PKR 50.00", "PKR 240.00",
            listOf(NativeOwnerReportsProductRow(1, "Milk", "PKR 400.00", "PKR 200.00", "PKR 200.00", "Cost history complete")))
        val listener = android.view.Window.OnFrameMetricsAvailableListener { _, metric, _ ->
            val deadline = if (Build.VERSION.SDK_INT >= 31) metric.getMetric(FrameMetrics.DEADLINE) else 16_666_667L
            frames += Frame(metric.getMetric(FrameMetrics.TOTAL_DURATION), deadline, metric.getMetric(FrameMetrics.LAYOUT_MEASURE_DURATION), metric.getMetric(FrameMetrics.DRAW_DURATION))
        }
        fun capture(scenario: ActivityScenario<ComponentActivity>, name: String) {
            val copied = CountDownLatch(1)
            var copyResult = -1
            var bitmap: Bitmap? = null
            scenario.onActivity { activity ->
                val view = activity.window.decorView
                bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
                PixelCopy.request(activity.window, bitmap!!, { result ->
                    copyResult = result
                    copied.countDown()
                }, Handler(activity.mainLooper))
            }
            assertTrue("Window screenshot must complete", copied.await(5, TimeUnit.SECONDS))
            assertEquals(PixelCopy.SUCCESS, copyResult)
            val image = requireNotNull(bitmap)
            val file = File(output, name)
            file.outputStream().use { image.compress(Bitmap.CompressFormat.PNG, 100, it) }
            exportMotionEvidence(file)
            val review = File(output, name.removeSuffix(".png") + ".review.json")
            review.writeText(JSONObject().put("scenario", name.removeSuffix(".png"))
                .put("workflowId", "profileRealTimeNativeTransitionFramesAndGuardOwnership")
                .put("reviewStatus", "UNREVIEWED").put("packageName", context.packageName)
                .put("capturedAt", java.time.Instant.now().toString())
                .put("note", "Synthetic Sale/Reports body transition under persistent Owner chrome; not an exhaustive app motion audit.").toString(2))
            exportMotionEvidence(review)
            image.recycle()
        }
        fun awaitPane(scenario: ActivityScenario<ComponentActivity>, expected: Boolean) {
            val deadline = SystemClock.uptimeMillis() + 5000
            while (SystemClock.uptimeMillis() < deadline) {
                var owns = false
                scenario.onActivity { owns = active != null && active === paneGuards[expected] }
                if (owns) return
                SystemClock.sleep(16)
            }
            fail("The requested pane must compose and own the active guard: $expected")
        }
        try {
            ActivityScenario.launch(ComponentActivity::class.java).use { scenario ->
                scenario.onActivity { activity ->
                    activity.enableEdgeToEdge()
                    androidx.core.view.WindowCompat.getInsetsController(activity.window, activity.window.decorView).apply {
                        isAppearanceLightStatusBars = false
                        isAppearanceLightNavigationBars = true
                    }
                    activity.window.addOnFrameMetricsAvailableListener(listener, Handler(handlerThread.looper))
                    activity.setContent {
                        SaafTheme {
                            OwnerScaffold(remember { SnackbarHostState() }, OwnerSection.Work, {}) { padding ->
                                androidx.compose.foundation.layout.Column(androidx.compose.ui.Modifier
                                    .fillMaxSize().padding(padding)) {
                                    CashierScreenHeader("Work", "Profile Owner", "Owner", date, true)
                                    androidx.compose.runtime.CompositionLocalProvider(LocalPaneHeaderEnabled provides false) {
                            NativeSectionTransition(reportsPane, modifier = androidx.compose.ui.Modifier.weight(1f), contentKey = { it }) { report ->
                                val guard = remember { EntryLeaveGuard().also { it.markDirty() } }
                                val register = rememberPaneGuardRegistration({ active }) { active = it }
                                DisposableEffect(guard) {
                                    paneGuards[report] = guard
                                    register(guard)
                                    onDispose { register(null) }
                                }
                                if (report) OwnerReportsScreen(remember { SnackbarHostState() }, identity,
                                    NativeOwnerReportsState.Ready(reportSnapshot), {}, {}, {}, {},
                                    hostedScaffold = { snackbar, modifier, _, content ->
                                        OwnerScaffold(snackbar, OwnerSection.Overview, {}, modifier = modifier, content = content)
                                    })
                                else CashierSaleScreen(remember { SnackbarHostState() }, identity,
                                    NativeSaleState.Ready(saleContext), {}, {}, {}, {}, {}, {})
                            }
                                    }
                                }
                            }
                        }
                    }
                }
                SystemClock.sleep(900)
                awaitPane(scenario, false)
                capture(scenario, "motion-before.png")
                scenario.onActivity { reportsPane = true }
                awaitPane(scenario, true)
                SystemClock.sleep(80)
                capture(scenario, "motion-mid.png")
                SystemClock.sleep(400)
                capture(scenario, "motion-after.png")
                assertFalse("Before/after window captures must show different panes",
                    File(output, "motion-before.png").readBytes().contentEquals(File(output, "motion-after.png").readBytes()))
                repeat(4) {
                    scenario.onActivity { reportsPane = !reportsPane }
                    awaitPane(scenario, reportsPane)
                    SystemClock.sleep(350)
                }
                frames.clear()
                repeat(30) {
                    scenario.onActivity { reportsPane = !reportsPane }
                    awaitPane(scenario, reportsPane)
                    SystemClock.sleep(350)
                    scenario.onActivity {
                        assertSame(paneGuards[reportsPane], active)
                        assertTrue(active!!.dirty)
                    }
                }
                scenario.onActivity { it.window.removeOnFrameMetricsAvailableListener(listener) }
                val samples = synchronized(frames) { frames.toList() }
                assertTrue("The device must expose actual frame metrics", samples.isNotEmpty())
                save("native-motion-profile.json", JSONObject().put("build", BuildConfig.BUILD_TYPE).put("device", Build.MODEL)
                    .put("transitions", 30).put("capture_method", "real ActivityScenario Choreographer, Window FrameMetrics and PixelCopy window screenshots")
                    .put("fixture", "Sale/Reports bodies under persistent Owner header and navigation")
                    .put("frame_total", distribution(samples.map { it.totalNs / 1_000_000.0 }))
                    .put("layout_measure", distribution(samples.map { it.layoutNs / 1_000_000.0 }))
                    .put("draw", distribution(samples.map { it.drawNs / 1_000_000.0 }))
                    .put("frames_over_deadline", samples.count { it.deadlineNs > 0 && it.totalNs > it.deadlineNs })
                    .put("deadline_ms", JSONArray(samples.map { it.deadlineNs / 1_000_000.0 })))
            }
        } finally { handlerThread.quitSafely(); handlerThread.join(1000) }
    }
}
