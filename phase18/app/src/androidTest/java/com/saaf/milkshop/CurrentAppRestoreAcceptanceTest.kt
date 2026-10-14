package com.saaf.milkshop

import android.content.Context
import android.content.ContextWrapper
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.saaf.milkshop.data.MilkShopDatabase
import com.saaf.milkshop.domain.*
import com.saaf.milkshop.sync.SyncScheduler
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** Current recovery acceptance uses only synthetic rows and a private test file. */
@RunWith(AndroidJUnit4::class)
class CurrentAppRestoreAcceptanceTest {
    @Test
    fun encryptedFullRestorePreservesRowsPreferencesAndReopenedState() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        check(context.packageName == "com.saaf.milkshop.qa.tests")
        val prefs = context.getSharedPreferences("milk_shop_preferences", Context.MODE_PRIVATE)
        val previousPrefs = prefs.all.toMap()
        val backupDir = File(context.filesDir, BackupManager.BACKUP_DIR)
        val previousBackups = backupDir.listFiles()?.map { it.name }?.toSet().orEmpty()
        val unique = System.nanoTime().toString()
        val destinationFile = File(context.cacheDir, "restore_acceptance_$unique.db")
        val destinationContext = object : ContextWrapper(context) {
            override fun getApplicationContext(): Context = this
            override fun getDatabasePath(name: String): File = destinationFile
        }
        val source = MilkShopDatabase.createInMemory()
        var target: MilkShopDatabase? = MilkShopDatabase.create(destinationContext)
        val backup = File(context.cacheDir, "restore_acceptance_$unique.milkbackup")
        val phrase = "synthetic portable restore phrase 2026".toCharArray()
        val ownerPhrase = "synthetic local owner phrase 2026".toCharArray()
        SyncScheduler.suppressSchedulingForTest()
        try {
            val owner = UserSecurityService(source).bootstrapOwner("restore.acceptance", "Synthetic Owner", ownerPhrase)
            StoreIdentityService(source).save(StoreIdentityInput("Synthetic Recovery Shop", phone = "0300", address = "Synthetic Road"))
            val vendor = VendorManagementService(source).create("Synthetic Recovery Vendor")
            CustomerManagementService(source).create("Synthetic Recovery Customer")
            val employee = EmployeeManagementService(source).create("Synthetic Recovery Employee")
            AttendanceService(source).record(AttendanceInput(employee, "2026-10-13", "present"))
            VendorPurchaseService(source).record(VendorPurchaseInput(vendor, "2026-10-13", 10.0, 4.0, 28.0))
            val milk = source.productDao().findByName("Milk")!!
            SpoilageService(source).record(SpoilageInput(milk.productId, 1.0, "2026-10-13", "Synthetic loss"))
            val expected = source.backupDataDao()
            val events = expected.events()
            val inventory = expected.inventoryLogs()
            val financial = expected.financialLogs()
            assertTrue(events.isNotEmpty()); assertTrue(inventory.isNotEmpty()); assertTrue(financial.isNotEmpty())
            assertEquals(9.0, source.readModelDao().currentStock().single { it.productId == milk.productId }.quantityOnHand, 0.0)
            assertTrue(prefs.edit().clear().putString("acceptance_label", "Synthetic recovery")
                .putBoolean("acceptance_enabled", true).putInt("acceptance_count", 7)
                .putLong("acceptance_epoch", 1_790_000_000_000L).putFloat("acceptance_scale", 1.25f).commit())
            val exportedPrefs = prefs.all.toMap()
            val manager = BackupManager(context)
            assertTrue(manager.exportEncryptedSnapshot(source, Uri.fromFile(backup), phrase).isSuccess)
            assertFalse(backup.readText().contains("restore.acceptance"))
            assertFalse(backup.readText().contains("Synthetic recovery"))
            val destination = target!!
            val beforeProducts = destination.backupDataDao().products()
            val beforeEvents = destination.backupDataDao().events()
            assertTrue(prefs.edit().clear().putString("destination_marker", "retain on rejection").commit())
            val beforePrefs = prefs.all.toMap()
            val sessions = NativeSessionManager()
            val stale = sessions.start(99L)
            val selection = BackupManager.RestoreSelection(true, true, true, security = true)
            val wrong = manager.restoreEncrypted(destination, Uri.fromFile(backup), selection,
                "wrong synthetic restore phrase".toCharArray(), sessions)
            assertTrue(wrong.isFailure)
            assertEquals(beforeProducts, destination.backupDataDao().products())
            assertEquals(beforeEvents, destination.backupDataDao().events())
            assertEquals(beforePrefs, prefs.all)
            assertEquals(99L, sessions.require(stale.sessionId).userId)
            val result = manager.restoreEncrypted(destination, Uri.fromFile(backup), selection, phrase, sessions)
            assertTrue(result.exceptionOrNull()?.message, result.isSuccess)
            assertTrue(runCatching { sessions.require(stale.sessionId) }.isFailure)
            assertEquals(exportedPrefs, prefs.all)
            assertEquals(events, destination.backupDataDao().events())
            assertEquals(inventory, destination.backupDataDao().inventoryLogs())
            assertEquals(financial, destination.backupDataDao().financialLogs())
            assertEquals(expected.products(), destination.backupDataDao().products())
            assertEquals(expected.accounts(), destination.backupDataDao().accounts())
            assertEquals(expected.productProfiles(), destination.backupDataDao().productProfiles())
            assertEquals(expected.customerProfiles(), destination.backupDataDao().customerProfiles())
            assertEquals(expected.vendorProfiles(), destination.backupDataDao().vendorProfiles())
            assertEquals(expected.employeeProfiles(), destination.backupDataDao().employeeProfiles())
            assertEquals(expected.attendanceRecords(), destination.backupDataDao().attendanceRecords())
            assertEquals(expected.storeProfiles(), destination.backupDataDao().storeProfiles())
            assertEquals(expected.spoilageDetails(), destination.backupDataDao().spoilageDetails())
            assertTrue(backupDir.listFiles().orEmpty().any { it.name !in previousBackups })
            destination.close(); target = null
            assertTrue(destinationFile.isFile)
            target = MilkShopDatabase.create(destinationContext)
            val reopened = target!!
            assertEquals(events, reopened.backupDataDao().events())
            assertEquals(inventory, reopened.backupDataDao().inventoryLogs())
            assertEquals(financial, reopened.backupDataDao().financialLogs())
            assertEquals(9.0, reopened.readModelDao().currentStock().single { it.productId == milk.productId }.quantityOnHand, 0.0)
            assertEquals(owner.user.userId, UserSecurityService(reopened).login("restore.acceptance", ownerPhrase).userId)
        } finally {
            source.close(); target?.close()
            backup.delete()
            listOf(destinationFile, File(destinationFile.path + "-wal"), File(destinationFile.path + "-shm")).forEach { it.delete() }
            backupDir.listFiles()?.filter { it.name !in previousBackups }?.forEach { it.delete() }
            val editor = prefs.edit().clear()
            previousPrefs.forEach { (key, value) -> when (value) {
                is String -> editor.putString(key, value)
                is Boolean -> editor.putBoolean(key, value)
                is Int -> editor.putInt(key, value)
                is Long -> editor.putLong(key, value)
                is Float -> editor.putFloat(key, value)
                is Set<*> -> editor.putStringSet(key, value.filterIsInstance<String>().toSet())
            } }
            assertTrue(editor.commit())
            com.saaf.milkshop.sync.SyncRebaselineStore(context).clear()
            SyncScheduler.warmUp(context)
            phrase.fill('\u0000'); ownerPhrase.fill('\u0000')
        }
    }
}
