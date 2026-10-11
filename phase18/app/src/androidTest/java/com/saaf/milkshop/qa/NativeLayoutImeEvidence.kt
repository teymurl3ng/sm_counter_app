package com.saaf.milkshop.qa

import android.app.Activity
import android.graphics.Rect
import android.os.Build
import androidx.compose.ui.geometry.Rect as ComposeRect
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import java.io.File
import org.json.JSONObject
import org.junit.Assert.assertTrue

/**
 * Shared layout / IME helpers for native Compose screens.
 * Prefer Activity-hosted rules so IME and system windows are real.
 */
object NativeLayoutImeEvidence {
    /** Finish the real IME resize before testing a physical scroll/tap. */
    fun dismissImeAndWait(activity: Activity, rule: ComposeTestRule) {
        rule.runOnIdle {
            activity.currentFocus?.clearFocus()
            WindowCompat.getInsetsController(activity.window, activity.window.decorView)
                .hide(WindowInsetsCompat.Type.ime())
        }
        rule.waitUntil(5_000) {
            var hidden = false
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                val insets = imeInsets(activity)
                hidden = !insets.isVisible(WindowInsetsCompat.Type.ime()) &&
                    insets.getInsets(WindowInsetsCompat.Type.ime()).bottom == 0
            }
            hidden
        }
        rule.waitForIdle()
    }

    data class OcclusionReport(
        val focusedBounds: ComposeRect,
        val visibleFrame: Rect,
        val imeTop: Int,
        val imeVisible: Boolean,
        val focusedOccludedByIme: Boolean,
        val criticalBottom: Float,
        val criticalOccludedByIme: Boolean,
    )

    fun imeInsets(activity: Activity): WindowInsetsCompat {
        val decor = activity.window.decorView
        val compat = ViewCompat.getRootWindowInsets(decor)
        if (compat != null) return compat
        if (Build.VERSION.SDK_INT >= 30) {
            val raw = decor.rootWindowInsets
            if (raw != null) return WindowInsetsCompat.toWindowInsetsCompat(raw, decor)
        }
        return WindowInsetsCompat.CONSUMED
    }

    fun visibleDisplayFrame(activity: Activity): Rect {
        val frame = Rect()
        activity.window.decorView.getWindowVisibleDisplayFrame(frame)
        return frame
    }

    fun evaluateOcclusion(
        activity: Activity,
        focused: SemanticsNodeInteraction,
        critical: SemanticsNodeInteraction? = null,
    ): OcclusionReport {
        val focusedBounds = focused.fetchSemanticsNode().boundsInWindow
        val criticalBounds = critical?.fetchSemanticsNode()?.boundsInWindow
        val insets = imeInsets(activity)
        val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
        val visible = visibleDisplayFrame(activity)
        val imeVisible = insets.isVisible(WindowInsetsCompat.Type.ime()) || ime.bottom > 0
        val height = activity.window.decorView.height
        val imeTop = if (imeVisible) {
            (height - ime.bottom).coerceAtMost(visible.bottom)
        } else {
            visible.bottom
        }
        val focusedOccluded = imeVisible && focusedBounds.bottom > imeTop + 1f
        val criticalBottom = criticalBounds?.bottom ?: focusedBounds.bottom
        val criticalOccluded = imeVisible && criticalBottom > imeTop + 1f
        return OcclusionReport(
            focusedBounds = focusedBounds,
            visibleFrame = visible,
            imeTop = imeTop,
            imeVisible = imeVisible,
            focusedOccludedByIme = focusedOccluded,
            criticalBottom = criticalBottom,
            criticalOccludedByIme = criticalOccluded,
        )
    }

    fun assertFocusedFieldNotOccluded(
        activity: Activity,
        focused: SemanticsNodeInteraction,
        critical: SemanticsNodeInteraction? = null,
    ): OcclusionReport {
        val report = evaluateOcclusion(activity, focused, critical)
        if (report.imeVisible) {
            assertTrue(
                "Focused field is occluded by IME. focusedBottom=${report.focusedBounds.bottom} imeTop=${report.imeTop} visible=${report.visibleFrame}",
                !report.focusedOccludedByIme,
            )
            if (critical != null) {
                assertTrue(
                    "Critical control is occluded by IME. criticalBottom=${report.criticalBottom} imeTop=${report.imeTop}",
                    !report.criticalOccludedByIme,
                )
            }
        }
        return report
    }
}

object NativeEvidenceCapture {
    private const val PUBLIC_ROOT = "/sdcard/Download/phase14-native-evidence"

    data class CaptureSpec(
        val screen: String,
        val scenario: String,
        val workflowId: String,
        val orientation: String = "portrait",
        val fontScale: String = "1.0",
        val theme: String = "light",
        val reviewStatus: String = "UNREVIEWED",
        val note: String = "",
    )

    fun captureWholeWindow(
        activity: Activity,
        spec: CaptureSpec,
    ): File {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val dirName = "native-${spec.screen}-a06"
        val publicDir = "$PUBLIC_ROOT/$dirName"
        executeShell("mkdir -p $publicDir")

        val filename = "${spec.scenario}.png"
        val localDir = File(requireNotNull(instrumentation.targetContext.getExternalFilesDir(null)), dirName).apply { mkdirs() }
        val localFile = File(localDir, filename)

        val device = UiDevice.getInstance(instrumentation)
        assertTrue("Failed to capture device screenshot for ${spec.scenario}", device.takeScreenshot(localFile))
        assertTrue("Screenshot file missing for ${spec.scenario}", localFile.isFile && localFile.length() > 0L)
        exportQaEvidence(localFile, "$publicDir/$filename")

        val review = JSONObject()
            .put("scenario", spec.scenario)
            .put("screen", spec.screen)
            .put("device", "${Build.MANUFACTURER} ${Build.MODEL}")
            .put("packageName", instrumentation.targetContext.packageName)
            .put("capturedAt", java.time.Instant.now().toString())
            .put("orientation", spec.orientation)
            .put("fontScale", spec.fontScale)
            .put("theme", spec.theme)
            .put("workflowId", spec.workflowId)
            .put("evidenceFilename", filename)
            .put("reviewStatus", spec.reviewStatus)
            .put("note", spec.note)
        val reviewFile = File(localDir, "${spec.scenario}.review.json")
        reviewFile.writeText(review.toString(2))
        exportQaEvidence(reviewFile, "$publicDir/${spec.scenario}.review.json")

        val manifestLine = listOf(
            spec.scenario,
            filename,
            spec.workflowId,
            "${Build.MODEL}/${Build.VERSION.RELEASE}",
            spec.orientation,
            spec.fontScale,
            spec.theme,
            spec.reviewStatus,
        ).joinToString("\t")
        val manifest = File(localDir, "manifest.tsv")
        if (!manifest.exists()) {
            manifest.writeText("scenario\tpng\tworkflow_id\tdevice\torientation\tfont_scale\ttheme\treview_status\n")
        }
        manifest.appendText("$manifestLine\n")
        exportQaEvidence(manifest, "$publicDir/manifest.tsv")
        return localFile
    }

    private fun exportQaEvidence(file: File, destination: String) {
        val pkg = InstrumentationRegistry.getInstrumentation().targetContext.packageName
        require(pkg in setOf("com.saaf.milkshop.qa", "com.saaf.milkshop.qa.tests", "com.saaf.milkshop.qa.benchmark")) { "Evidence export requires an isolated QA package" }
        require(file.absolutePath.matches(Regex("[A-Za-z0-9_./-]+")))
        require(destination.startsWith("$PUBLIC_ROOT/") && destination.matches(Regex("[A-Za-z0-9_./-]+")))
        // App-owned external files are writable by this QA target and readable by adb shell.
        executeShell("cp ${file.absolutePath} $destination")
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        val size = device.executeShellCommand("stat -c %s $destination").trim().toLongOrNull()
        assertTrue("Evidence export missing or truncated: $destination", size == file.length())
    }

    private fun executeShell(command: String) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        android.os.ParcelFileDescriptor.AutoCloseInputStream(
            instrumentation.uiAutomation.executeShellCommand(command),
        ).use { it.readBytes() }
    }
}
