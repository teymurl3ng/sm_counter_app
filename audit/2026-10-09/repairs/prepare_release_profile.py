"""Create a separately scoped profile fixture without modifying the earlier probe."""
from pathlib import Path

ROOT = Path(__file__).resolve().parents[3]
tests = ROOT / 'phase18/app/src/androidTest/java/com/saaf/milkshop'
source = (tests / 'NativeDeviceProfileProbe.kt').read_text(encoding='utf-8')

def replace(old, new):
    global source
    assert old in source, old
    source = source.replace(old, new)

replace('class NativeDeviceProfileProbe', 'class NativeReleaseProfileProbe')
replace('import org.junit.Assert.*', 'import org.junit.Assert.*\nimport org.junit.Before\nimport org.junit.Assume')
replace('class NativeReleaseProfileProbe {', '''class NativeReleaseProfileProbe {
    @Before fun requireIsolatedBenchmark() {
        Assume.assumeTrue("Release profile requires the isolated benchmark build", BuildConfig.BUILD_TYPE == "benchmark")
    }''')
replace('/** Observational debug profiles, not production budgets or owner visual approval. */',
        '/** Release-derived, isolated benchmark observations; no owner visual approval or production budget claim. */')
replace('com.saaf.milkshop.qa.tests', 'com.saaf.milkshop.qa.benchmark')
replace('File(context.filesDir, "audit_device_profile")', 'File(requireNotNull(context.getExternalFilesDir(null)), "audit_release_profile")')
replace('profileReceiptAndReportReadsWithTenThousandCollectionEvents', 'profilePersistentMixedCostingDataset')
replace('val db = MilkShopDatabase.createInMemory()', '''check(BuildConfig.BUILD_TYPE == "benchmark")
        val fixtureRoot = File(context.filesDir, "audit_release_database").also { it.mkdirs() }
        val isolatedContext = object : android.content.ContextWrapper(context) {
            override fun getApplicationContext(): android.content.Context = this
            override fun getDatabasePath(name: String): File = File(fixtureRoot, name)
        }
        val db = MilkShopDatabase.create(isolatedContext)''')
replace('val seedStarted = SystemClock.elapsedRealtimeNanos()', '''val policy = CostingPolicyService(db, security).approve(session, CostingPolicyInput(
                calendarMonth = java.time.YearMonth.from(today).toString(),
                normalCapacityKg = java.math.BigDecimal("1000"), reviewReason = "monthly",
                components = CostingPolicyService.COMPONENT_CODES.map { ConversionCostComponentInput(it, 100000, java.math.BigDecimal("1000")) }))
            val seedStarted = SystemClock.elapsedRealtimeNanos()''')
replace('assertEquals(10_001, db.eventDao().count())', '''db.withWriteTransaction {
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
            assertEquals(policy.version.policyId, costing.loadPolicies(session, today.toString()).policies.single().policyId)''')
replace('assertEquals(10_045, db.eventDao().count())', 'assertEquals(10_125, db.eventDao().count())')
replace('"100 customers; 10000 collections/20000 financial legs; one stock purchase"',
        '"Persistent bundled Room; 100 customers; 10000 collections; 21 purchases; 40 yogurt batches; 20 yogurt sales; one costing policy"')
replace('.put("warmups_per_operation", 2).put("seed_ms", seedMs)',
        '.put("storage", "persistent isolated Room fixture").put("warmups_per_operation", 2).put("collection_seed_ms", seedMs).put("yield_context_40_batches", distribution(yieldMs))')
(tests / 'NativeReleaseProfileProbe.kt').write_text(source, encoding='utf-8')
print('Release profile fixture generated; earlier NativeDeviceProfileProbe unchanged.')
