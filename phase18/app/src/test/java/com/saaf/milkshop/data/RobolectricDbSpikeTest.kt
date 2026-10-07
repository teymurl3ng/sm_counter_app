package com.saaf.milkshop.data

import android.app.Application
import androidx.room3.useReaderConnection
import com.saaf.milkshop.domain.UserSecurityService
import com.saaf.milkshop.application.bridge.BridgeCommandDayClosed
import com.saaf.milkshop.application.bridge.ReconciliationBridgeCommandService
import com.saaf.milkshop.domain.PhysicalCashCountInput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * C2 spike under Robolectric (verdict: option C, two configurations).
 *
 * Spike 1 confirms the prediction: the DEFAULT bundled driver (Android
 * natives) cannot load on the host JVM even under Robolectric.
 *
 * Spike 2 is the real harness: the framework driver builds the REAL
 * 42-entity database, and the probe fires a capture trigger (local edit →
 * outbox row), the closed-day gate (a count on a sealed day is refused),
 * and logs sqlite_version() — the driver-fidelity gap the reviewer required
 * us to record (device suite stays the production-driver backstop).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class RobolectricDbSpikeTest {

    @Test
    fun spike1_bundledDriverCannotLoadOnJvm() {
        val database = MilkShopDatabase.createInMemory() // default = BundledSQLiteDriver
        // Room builds lazily: the driver's natives only load when a connection
        // opens, so the probe must actually open one.
        val error = assertThrows(Throwable::class.java) {
            kotlinx.coroutines.runBlocking {
                database.useReaderConnection { conn ->
                    conn.usePrepared("SELECT 1") { stmt -> stmt.step() }
                }
            }
        }
        println("SPIKE1 cause chain: $error / ${error.cause} / ${error.cause?.cause}")
        database.close()
    }

    @Test
    fun spike2_frameworkDriver_realDb_triggers_andSqliteVersion() = kotlinx.coroutines.runBlocking<Unit> {
        val database = MilkShopDatabase.createInMemory(driver = null)
        try {
            database.useReaderConnection { conn ->
                conn.usePrepared("SELECT sqlite_version()") { stmt ->
                    stmt.step()
                    println("sqlite_version() = " + stmt.getText(0))
                }
            }

            val security = UserSecurityService(database)
            val owner = security.bootstrapOwner("spike.owner", "Spike Owner", "correct horse battery staple".toCharArray())

            // Capture trigger: a local account insert must enqueue an outbox row.
            val before = database.syncOutboxDao().count()
            database.accountDao().insert(
                AccountEntity(name = "Spike Customer", type = "Asset", category = "Customer"),
            )
            val after = database.syncOutboxDao().count()
            assertTrue("capture trigger did not enqueue (before=$before after=$after)", after > before)

            // Closed-day gate: a real count for the day, a real seal, then a
            // further count for the same date must be refused.
            val cash = database.accountDao().findByCategory("Cash/Bank").firstOrNull { it.name == "Cash" }
                ?: database.accountDao().insert(
                    AccountEntity(name = "Cash", type = "Asset", category = "Cash/Bank"),
                ).let { database.accountDao().findById(it)!! }
            val commands = ReconciliationBridgeCommandService(database, security)
            val session = security.login("spike.owner", "correct horse battery staple".toCharArray())
            val reconciliation = kotlinx.coroutines.runBlocking {
                commands.recordCount(
                    session.sessionId,
                    PhysicalCashCountInput(
                        accountId = cash.accountId,
                        businessDate = "2026-10-06",
                        countedAmount = 0.0, // empty ledger: zero variance, no explanation owed
                    ),
                )
            }
            database.dailyCloseDao().insert(
                DailyCloseActionEntity(
                    businessDate = "2026-10-06",
                    action = "close",
                    physicalCashCountId = reconciliation.count.countId,
                    actorName = owner.user.displayName,
                    actorRole = owner.user.role,
                    actorUserId = owner.user.userId,
                    createdAt = "2026-10-06T23:00:00Z",
                ),
            )
            assertThrows(BridgeCommandDayClosed::class.java) {
                kotlinx.coroutines.runBlocking {
                    commands.recordCount(
                        session.sessionId,
                        PhysicalCashCountInput(
                            accountId = cash.accountId,
                            businessDate = "2026-10-06",
                            countedAmount = 100.0,
                        ),
                    )
                }
            }
        } finally {
            database.close()
        }
    }

}
