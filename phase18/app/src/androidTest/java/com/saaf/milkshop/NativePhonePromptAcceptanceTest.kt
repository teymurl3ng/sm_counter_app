package com.saaf.milkshop

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.saaf.milkshop.nativeui.NativePreviewActivity
import com.saaf.milkshop.quickunlock.AndroidQuickUnlockDeviceGate
import com.saaf.milkshop.quickunlock.QuickUnlockFailure
import java.io.File
import java.util.UUID
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Real Android prompt; no supplied phone credential or successful biometric authentication. */
@RunWith(AndroidJUnit4::class)
class NativePhonePromptAcceptanceTest {
    @get:Rule val rule = createAndroidComposeRule<NativePreviewActivity>()

    @Test fun realPhoneSecurityPromptBackCancelsWithoutEnrollment() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        check(context.packageName == "com.saaf.milkshop.qa.tests")
        val app = context.applicationContext as MilkShopApplication
        val store = File(context.noBackupFilesDir, "quick_unlock/state.json")
        val quickBefore = if (store.exists()) store.readBytes().toList() else null
        fun facts() = runBlocking {
            val dao = app.database.backupDataDao()
            listOf(dao.events(), dao.inventoryLogs(), dao.financialLogs(), dao.deliveries(),
                dao.deliveryStatuses(), dao.physicalCashCounts(), dao.dailyCloseActions())
        }
        val before = facts()
        val output = File(context.getExternalFilesDir(null), "phone-acceptance").apply { mkdirs() }
        val keyId = UUID.randomUUID().toString()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        val result = AtomicReference<Throwable?>()
        var gate: AndroidQuickUnlockDeviceGate? = null
        var shown = 0
        var cancellations = 0
        try {
            rule.runOnIdle { gate = AndroidQuickUnlockDeviceGate(rule.activity) }
            val actual = requireNotNull(gate)
            val status = actual.status()
            val device = UiDevice.getInstance(instrumentation)
            val repeats = if (status.available) 2 else 1
            repeat(repeats) { index ->
                result.set(null)
                val finished = java.util.concurrent.atomic.AtomicBoolean(false)
                rule.runOnIdle {
                    scope.launch {
                        try {
                            actual.protect(keyId, 999_999, "Synthetic Phone QA", byteArrayOf(1, 2, 3))
                            result.set(AssertionError("QA prompt must be cancelled; no authentication was requested"))
                        } catch (error: Throwable) { result.set(error) }
                        finally { finished.set(true) }
                    }
                }
                if (status.available) {
                    assertTrue("Actual Android setup prompt did not appear", device.wait(Until.hasObject(By.text("Set up Quick unlock")), 8_000))
                    shown++
                    if (index == 0) device.takeScreenshot(File(output, "phone-prompt.png"))
                    device.pressBack()
                    rule.waitUntil(8_000) { finished.get() }
                    assertTrue("Expected native cancellation, got ${result.get()}", result.get() is QuickUnlockFailure.Cancelled)
                    cancellations++
                    assertTrue(device.wait(Until.gone(By.text("Set up Quick unlock")), 5_000))
                } else {
                    rule.waitUntil(8_000) { finished.get() }
                    assertTrue("Expected unavailable actual authentication, got ${result.get()}", result.get() is QuickUnlockFailure.Unavailable)
                }
            }
            assertEquals(before, facts())
            assertEquals(quickBefore, if (store.exists()) store.readBytes().toList() else null)
            val metadata = JSONObject().put("actualAuthenticationAvailable", status.available)
                .put("label", status.label).put("setupPromptsShown", shown).put("backCancellations", cancellations)
                .put("phoneCredentialEntered", false).put("successfulBiometricAuthentication", false)
                .put("businessRowsAndEnrollmentRecordUnchanged", true)
            File(output, "phone-prompt-result.json").writeText(metadata.toString(2))
            println("QA_REAL_PHONE_PROMPT ${metadata}")
        } finally {
            rule.runOnIdle {
                scope.cancel()
                gate?.close()
                gate?.delete(keyId)
            }
        }
    }
}
