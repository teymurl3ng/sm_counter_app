package com.saaf.milkshop

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.domain.AttendanceInput
import com.saaf.milkshop.domain.AttendanceService
import com.saaf.milkshop.domain.CustomerManagementService
import com.saaf.milkshop.domain.CreateUserInput
import com.saaf.milkshop.domain.DailyCloseInput
import com.saaf.milkshop.domain.DailyCloseService
import com.saaf.milkshop.domain.DailyReopenInput
import com.saaf.milkshop.domain.EmployeeManagementService
import com.saaf.milkshop.domain.ExpenseCategoryManagementService
import com.saaf.milkshop.domain.PhysicalCashCountInput
import com.saaf.milkshop.domain.PhysicalCashCountService
import com.saaf.milkshop.domain.OpeningAccountBalance
import com.saaf.milkshop.domain.OpeningInventoryBalance
import com.saaf.milkshop.domain.OpeningStateInput
import com.saaf.milkshop.domain.OpeningStateService
import com.saaf.milkshop.domain.NativeSessionManager
import com.saaf.milkshop.domain.SpoilageInput
import com.saaf.milkshop.domain.SpoilageService
import com.saaf.milkshop.domain.StoreIdentityInput
import com.saaf.milkshop.domain.StoreIdentityService
import com.saaf.milkshop.domain.UserSecurityService
import com.saaf.milkshop.domain.VerifiedDailyCloseInput
import com.saaf.milkshop.domain.VendorManagementService
import com.saaf.milkshop.domain.VendorPurchaseInput
import com.saaf.milkshop.domain.VendorPurchaseService
import android.net.Uri
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import org.json.JSONObject

@RunWith(AndroidJUnit4::class)
class Phase9BackupRestoreTest {
    @Test
    fun localSnapshot_isCreated_andContainsExpectedMetadata() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = MilkShopDatabase.createInMemory()
        val backupDir = File(context.filesDir, BackupManager.BACKUP_DIR)
        val existing = backupDir.listFiles()?.mapTo(mutableSetOf()) { it.name } ?: mutableSetOf()
        try {
            val manager = BackupManager(context)
            val result = manager.createLocalSnapshot(database)
            assertTrue(result.isSuccess)
            val meta = result.getOrThrow()
            assertTrue(meta.fileName.endsWith(BackupManager.BACKUP_EXTENSION))
            assertEquals(BackupManager.CURRENT_SCHEMA_VERSION, meta.schemaVersion)
            assertEquals(BackupManager.FORMAT_VERSION, meta.formatVersion)
            val file = File(backupDir, meta.fileName)
            val envelope = file.readText()
            assertTrue(envelope.contains("android-keystore"))
            assertFalse(envelope.contains("products"))
            assertTrue(manager.localBackups().any { it.first.name == meta.fileName })
        } finally {
            database.close()
            backupDir.listFiles()?.filter { it.name !in existing }?.forEach { it.delete() }
        }
    }

    @Test
    fun encryptedLocalSnapshot_roundTripPreservesSecurityAndRejectsTampering() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val source = MilkShopDatabase.createInMemory()
        val restored = MilkShopDatabase.createInMemory()
        val backupDir = File(context.filesDir, BackupManager.BACKUP_DIR)
        val existing = backupDir.listFiles()?.mapTo(mutableSetOf()) { it.name } ?: mutableSetOf()
        val ownerPassphrase = "local snapshot owner phrase 2026".toCharArray()
        try {
            val sourceSecurity = UserSecurityService(source)
            val owner = sourceSecurity.bootstrapOwner("local.owner", "Local Owner", ownerPassphrase)
            val manager = BackupManager(context)
            val meta = manager.createLocalSnapshot(source).getOrThrow()
            val snapshot = File(backupDir, meta.fileName)
            assertFalse(snapshot.readText().contains("local.owner"))
            assertEquals(
                BackupManager.FORMAT_VERSION,
                manager.readBackupMeta(Uri.fromFile(snapshot)).getOrThrow().formatVersion,
            )

            val restoredSessions = NativeSessionManager()
            val stale = restoredSessions.start(123L)
            val restore = manager.restoreLocalSnapshot(
                restored,
                Uri.fromFile(snapshot),
                BackupManager.RestoreSelection(true, true, false, security = true),
                restoredSessions,
            )
            assertTrue(restore.exceptionOrNull()?.message, restore.isSuccess)
            assertTrue(runCatching { restoredSessions.require(stale.sessionId) }.isFailure)
            assertNotNull(restored.savedRecoveryCodeDao().current(owner.user.userId))
            assertTrue(UserSecurityService(restored).login("local.owner", ownerPassphrase).sessionId.isNotBlank())

            val envelope = JSONObject(snapshot.readText())
            val cipher = envelope.getJSONObject("cipher")
            val ciphertext = cipher.getString("ciphertext")
            cipher.put("ciphertext", (if (ciphertext[0] == 'A') 'B' else 'A') + ciphertext.substring(1))
            snapshot.writeText(envelope.toString())
            assertTrue(manager.readBackupMeta(Uri.fromFile(snapshot)).isFailure)
        } finally {
            ownerPassphrase.fill('\u0000')
            source.close()
            restored.close()
            backupDir.listFiles()?.filter { it.name !in existing }?.forEach { it.delete() }
        }
    }

    @Test
    fun confirmationPhrase_isExact() {
        assertEquals(
            "I understood the consequences and I agree to proceed forward",
            BackupManager.CONFIRMATION_PHRASE,
        )
    }

    @Test
    fun encryptedFormat2_roundTripPreservesSecurityAndInvalidatesSessions() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val source = MilkShopDatabase.createInMemory()
        val restored = MilkShopDatabase.createInMemory()
        val backupFile = File(context.cacheDir, "encrypted_security_round_trip.milkbackup")
        val backupPassphrase = "owner portable backup phrase 2026".toCharArray()
        val ownerPassphrase = "owner account passphrase 2026".toCharArray()
        try {
            val sourceSessions = NativeSessionManager()
            val sourceSecurity = UserSecurityService(source, sourceSessions)
            val owner = sourceSecurity.bootstrapOwner("backup.owner", "Backup Owner", ownerPassphrase)
            val adminEnrollment = sourceSecurity.beginUserEnrollment(
                owner.session.sessionId,
                CreateUserInput("backup.admin", "Backup Admin", "admin"),
            )
            sourceSecurity.completeUserEnrollment(
                adminEnrollment.enrollmentToken,
                "admin account passphrase 2026".toCharArray(),
            )
            assertTrue(
                runCatching {
                    sourceSecurity.login("backup.owner", "incorrect owner phrase 2026".toCharArray())
                }.isFailure
            )
            val sourceAuditCount = source.securityAuditDao().all().size
            val cashId = source.accountDao().findByCategory("Cash/Bank").single { it.name == "Cash" }.accountId
            val count = PhysicalCashCountService(source).record(
                PhysicalCashCountInput(cashId, "2026-08-27", 0.0)
            )
            DailyCloseService(source).closeAuthenticated(
                owner.session.sessionId,
                sourceSecurity,
                VerifiedDailyCloseInput("2026-08-27", count.count.countId),
            )

            val manager = BackupManager(context)
            val export = manager.exportEncryptedSnapshot(source, Uri.fromFile(backupFile), backupPassphrase)
            assertTrue(export.exceptionOrNull()?.message, export.isSuccess)
            assertEquals(BackupManager.FORMAT_VERSION, export.getOrThrow().formatVersion)
            val envelopeText = backupFile.readText()
            assertFalse(envelopeText.contains("backup.owner"))
            assertFalse(envelopeText.contains("user_credentials"))
            assertEquals(
                BackupManager.FORMAT_VERSION,
                manager.readEncryptedBackupMeta(Uri.fromFile(backupFile), backupPassphrase).getOrThrow().formatVersion,
            )

            val restoredSessions = NativeSessionManager()
            val preRestoreSession = restoredSessions.start(99L)
            val restore = manager.restoreEncrypted(
                restored,
                Uri.fromFile(backupFile),
                BackupManager.RestoreSelection(
                    business = true,
                    planning = true,
                    preferences = false,
                    security = true,
                ),
                backupPassphrase,
                restoredSessions,
            )
            assertTrue(restore.exceptionOrNull()?.message, restore.isSuccess)
            assertTrue(runCatching { restoredSessions.require(preRestoreSession.sessionId) }.isFailure)
            assertEquals(2L, restored.appUserDao().count())
            assertEquals(sourceAuditCount, restored.securityAuditDao().all().size)
            assertEquals(0, restored.userCredentialDao().find(owner.user.userId)!!.failedAttemptCount)
            assertTrue(restored.userCredentialDao().find(owner.user.userId)!!.lastFailedAtEpochMs == null)
            assertNotNull(restored.savedRecoveryCodeDao().current(owner.user.userId))
            assertEquals(owner.user.userId, DailyCloseService(restored).history("2026-08-27").single().actorUserId)
            assertTrue(UserSecurityService(restored).login("backup.owner", ownerPassphrase).sessionId.isNotBlank())
        } finally {
            source.close()
            restored.close()
            backupFile.delete()
            backupPassphrase.fill('\u0000')
            ownerPassphrase.fill('\u0000')
        }
    }

    @Test
    fun encryptedFormat2_rejectsWrongPassphraseAndTampering() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val source = MilkShopDatabase.createInMemory()
        val backupFile = File(context.cacheDir, "encrypted_security_rejection.milkbackup")
        val passphrase = "correct portable backup phrase 2026".toCharArray()
        try {
            UserSecurityService(source).bootstrapOwner(
                "secure.owner",
                "Secure Owner",
                "secure owner account phrase 2026".toCharArray(),
            )
            val manager = BackupManager(context)
            assertTrue(manager.exportEncryptedSnapshot(source, Uri.fromFile(backupFile), passphrase).isSuccess)
            assertTrue(
                manager.readEncryptedBackupMeta(
                    Uri.fromFile(backupFile),
                    "wrong portable backup phrase 2026".toCharArray(),
                ).isFailure
            )

            val envelope = JSONObject(backupFile.readText())
            val cipher = envelope.getJSONObject("cipher")
            val ciphertext = cipher.getString("ciphertext")
            val replacement = if (ciphertext[0] == 'A') 'B' else 'A'
            cipher.put("ciphertext", replacement + ciphertext.substring(1))
            backupFile.writeText(envelope.toString())
            assertTrue(manager.readEncryptedBackupMeta(Uri.fromFile(backupFile), passphrase).isFailure)
        } finally {
            source.close()
            backupFile.delete()
            passphrase.fill('\u0000')
        }
    }

    @Test
    fun fullBackupRestore_preservesPhase3NativeExtensionState() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val source = MilkShopDatabase.createInMemory()
        val restored = MilkShopDatabase.createInMemory()
        val backupFile = File(context.cacheDir, "phase3_extension_round_trip.milkbackup")
        try {
            val vendorId = VendorManagementService(source).create("Backup Vendor")
            val customerId = CustomerManagementService(source).create("Backup Customer")
            val employeeId = EmployeeManagementService(source).create("Backup Employee")
            AttendanceService(source).record(
                AttendanceInput(employeeId, "2026-08-27", "present")
            )
            ExpenseCategoryManagementService(source).create("Cleaning")
            val cashId = source.accountDao().findByCategory("Cash/Bank").single { it.name == "Cash" }.accountId
            PhysicalCashCountService(source).record(
                PhysicalCashCountInput(cashId, "2026-08-27", 500.0)
            )
            VendorPurchaseService(source).record(
                VendorPurchaseInput(vendorId, "2026-08-27", 10.0, 4.0, 28.0)
            )
            val milk = source.productDao().findByName("Milk")!!
            SpoilageService(source).record(
                SpoilageInput(milk.productId, 1.0, "2026-08-27", "Backup test")
            )

            val manager = BackupManager(context)
            val export = manager.exportSnapshot(source, Uri.fromFile(backupFile))
            assertTrue(export.isSuccess)
            assertEquals(7, export.getOrThrow().schemaVersion)
            val restore = manager.restore(
                restored,
                Uri.fromFile(backupFile),
                BackupManager.RestoreSelection(business = true, planning = false, preferences = false),
            )
            assertTrue(restore.isSuccess)
            assertNotNull(restored.customerProfileDao().findByAccountId(customerId))
            assertEquals(1, restored.attendanceDao().history(employeeId).size)
            assertEquals(1, restored.physicalCashCountDao().all().size)
            assertEquals(1, restored.spoilageDao().all().size)
            assertTrue(restored.expenseCategoryProfileDao().all().any { it.displayName == "Cleaning" })
        } finally {
            source.close()
            restored.close()
            backupFile.delete()
        }
    }

    @Test
    fun fullBackupRestore_preservesStoreIdentityAndOpeningCorrectionChain() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val source = MilkShopDatabase.createInMemory()
        val restored = MilkShopDatabase.createInMemory()
        val backupFile = File(context.cacheDir, "phase3_opening_round_trip.milkbackup")
        try {
            StoreIdentityService(source).save(
                StoreIdentityInput("Backup Opening Shop", phone = "0300", address = "Main Road")
            )
            val customerId = CustomerManagementService(source).create("Opening Backup Customer")
            val vendorId = VendorManagementService(source).create("Opening Backup Vendor")
            val cashId = source.accountDao().findByCategory("Cash/Bank").single { it.name == "Cash" }.accountId
            val milkId = source.productDao().findByName("Milk")!!.productId
            val opening = OpeningStateService(source)
            opening.record(
                OpeningStateInput(
                    "2026-08-27",
                    listOf(OpeningInventoryBalance(milkId, 20.0)),
                    listOf(
                        OpeningAccountBalance(cashId, 500.0),
                        OpeningAccountBalance(customerId, 100.0),
                        OpeningAccountBalance(vendorId, 200.0),
                    ),
                )
            )
            opening.correct(
                OpeningStateInput(
                    "2026-08-27",
                    listOf(OpeningInventoryBalance(milkId, 18.0)),
                    listOf(
                        OpeningAccountBalance(cashId, 550.0),
                        OpeningAccountBalance(vendorId, 200.0),
                    ),
                )
            )

            val manager = BackupManager(context)
            assertTrue(manager.exportSnapshot(source, Uri.fromFile(backupFile)).isSuccess)
            val restore = manager.restore(
                restored,
                Uri.fromFile(backupFile),
                BackupManager.RestoreSelection(business = true, planning = false, preferences = false),
            )
            assertTrue(restore.exceptionOrNull()?.message, restore.isSuccess)

            assertEquals("Backup Opening Shop", StoreIdentityService(restored).get()!!.storeName)
            assertEquals("PKR", StoreIdentityService(restored).get()!!.currencyCode)
            assertEquals(2, OpeningStateService(restored).history().size)
            assertEquals(18.0, restored.readModelDao().currentStock().single { it.productId == milkId }.quantityOnHand, 0.0)
            assertEquals(550.0, restored.readModelDao().accountBalances().single { it.accountId == cashId }.balance, 0.0)
            assertEquals(0.0, restored.readModelDao().accountBalances().single { it.accountId == customerId }.balance, 0.0)
            assertEquals(200.0, restored.readModelDao().accountBalances().single { it.accountId == vendorId }.balance, 0.0)
        } finally {
            source.close()
            restored.close()
            backupFile.delete()
        }
    }

    @Test
    fun fullBackupRestore_preservesDailyCloseReopenAndRecloseHistory() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val source = MilkShopDatabase.createInMemory()
        val restored = MilkShopDatabase.createInMemory()
        val backupFile = File(context.cacheDir, "phase3_daily_close_round_trip.milkbackup")
        try {
            val cashId = source.accountDao().findByCategory("Cash/Bank").single { it.name == "Cash" }.accountId
            val cashCounts = PhysicalCashCountService(source)
            val closes = DailyCloseService(source)
            val first = cashCounts.record(PhysicalCashCountInput(cashId, "2026-08-27", 0.0))
            closes.close(DailyCloseInput("2026-08-27", first.count.countId, "Backup Cashier", "cashier"))
            closes.reopen(DailyReopenInput("2026-08-27", "Backup Owner", "owner", "Backup correction test"))
            val second = cashCounts.record(PhysicalCashCountInput(cashId, "2026-08-27", 0.0))
            closes.close(DailyCloseInput("2026-08-27", second.count.countId, "Backup Cashier", "cashier"))

            val manager = BackupManager(context)
            assertTrue(manager.exportSnapshot(source, Uri.fromFile(backupFile)).isSuccess)
            val dailyCloseRestore = manager.restore(
                restored,
                Uri.fromFile(backupFile),
                BackupManager.RestoreSelection(business = true, planning = false, preferences = false),
            )
            assertTrue(dailyCloseRestore.exceptionOrNull()?.message, dailyCloseRestore.isSuccess)

            val restoredClose = DailyCloseService(restored)
            assertEquals(3, restoredClose.history("2026-08-27").size)
            assertEquals(DailyCloseService.STATUS_CLOSED, restoredClose.current("2026-08-27")!!.status)
            assertEquals("Backup Owner", restoredClose.history("2026-08-27")[1].actorName)
        } finally {
            source.close()
            restored.close()
            backupFile.delete()
        }
    }
}
