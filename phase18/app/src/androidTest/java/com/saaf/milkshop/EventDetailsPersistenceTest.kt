package com.saaf.milkshop

import android.net.Uri
import androidx.room3.Room
import androidx.room3.testing.MigrationTestHelper
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.saaf.milkshop.data.*
import com.saaf.milkshop.domain.UserSecurityService
import com.saaf.milkshop.domain.NativeSessionManager
import com.saaf.milkshop.sync.SyncScheduler
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class EventDetailsPersistenceTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val name = "event-detail-migration.db"
    private val stamp = "2026-10-14T00:00:00Z"

    @get:Rule val helper = MigrationTestHelper(instrumentation, context.getDatabasePath(name),
        BundledSQLiteDriver(), MilkShopDatabase::class,
        { Room.databaseBuilder<MilkShopDatabase>(context, name).setDriver(BundledSQLiteDriver())
            .addMigrations(MilkShopDatabase.MIGRATION_16_17).build() }, emptyList())

    @Test fun migration16To17PreservesLocalActorsRowsQueueAndForeignKeys() = runBlocking {
        context.deleteDatabase(name)
        helper.createDatabase(16).apply {
            execSQL("INSERT INTO app_users(user_id,login_name,display_name,role,status,credential_setup_required,created_at,updated_at) VALUES(7,'existing.owner','Existing Owner','owner','active',0,'$stamp','$stamp')")
            execSQL("INSERT INTO events(event_id,created_at,occurred_at,event_type,sale_quantity,sale_value,unit_rate,surcharge_applied,event_uid) VALUES(8,'$stamp','2026-10-14','SALE',1,190,190,0,'u1_existing_actor_event')")
            execSQL("INSERT INTO event_actor_links VALUES(8,7,'Existing Owner','owner','$stamp')")
            execSQL("INSERT INTO products(product_id,name,unit,uid) VALUES(3,'Migration cream','Kg','u1_migration_product')")
            execSQL("INSERT INTO inventory_log(event_id,product_id,quantity) VALUES(8,3,-1)")
            execSQL("INSERT INTO sync_outbox(table_name,row_uid,enqueued_at,held) VALUES('events','u1_existing_actor_event','$stamp',1)")
            close()
        }
        helper.runMigrationsAndValidate(17, listOf(MilkShopDatabase.MIGRATION_16_17)).use { connection ->
            fun count(sql: String) = connection.prepare(sql).use { check(it.step()); it.getLong(0) }
            assertEquals(1L, count("SELECT count(*) FROM event_actor_links WHERE event_id=8 AND actor_user_id=7 AND actor_name_snapshot='Existing Owner' AND attributed_at='$stamp'"))
            assertEquals(1L, count("SELECT count(*) FROM inventory_log WHERE event_id=8 AND product_id=3 AND quantity=-1"))
            assertEquals(1L, count("SELECT count(*) FROM sync_outbox WHERE row_uid='u1_existing_actor_event' AND held=1"))
            connection.execSQL("PRAGMA foreign_keys=ON")
            connection.execSQL("INSERT INTO events(event_id,created_at,occurred_at,event_type,sale_quantity,sale_value,unit_rate,surcharge_applied,event_uid) VALUES(9,'$stamp','2026-10-14','SALE',1,190,190,0,'u1_remote_actor_event')")
            connection.execSQL("INSERT INTO event_actor_links VALUES(9,NULL,'Remote Owner','owner','$stamp')")
            assertEquals(1L, count("SELECT count(*) FROM event_actor_links WHERE event_id=9 AND actor_user_id IS NULL"))
            val foreignUser = runCatching { connection.execSQL("UPDATE event_actor_links SET actor_user_id=999 WHERE event_id=9") }
            assertTrue(foreignUser.isFailure)
            val foreignEvent = runCatching { connection.execSQL("INSERT INTO event_actor_links VALUES(999,NULL,'Remote','owner','$stamp')") }
            assertTrue(foreignEvent.isFailure)
            connection.prepare("PRAGMA foreign_key_check").use { assertFalse(it.step()) }
            assertEquals(1L, count("SELECT count(*) FROM app_users"))
        }
        context.deleteDatabase(name)
        Unit
    }

    @Test fun encryptedBackupPreservesBothLocalAndUnlinkedRemoteActorSnapshots() = runBlocking {
        check(context.packageName == "com.saaf.milkshop.qa.tests")
        val source = MilkShopDatabase.createInMemory()
        val target = MilkShopDatabase.createInMemory()
        val file = File(context.cacheDir, "event_details_${System.nanoTime()}.milkbackup")
        val phrase = "synthetic event detail backup phrase 2026".toCharArray()
        val manager = BackupManager(context)
        val dir = File(context.filesDir, BackupManager.BACKUP_DIR)
        val beforeFiles = dir.listFiles().orEmpty().map { it.name }.toSet()
        SyncScheduler.suppressSchedulingForTest()
        try {
            val owner = UserSecurityService(source).bootstrapOwner("details.owner", "Local Owner", "synthetic owner phrase 2026".toCharArray())
            for ((uid, actorId, name) in listOf(Triple("u1_local_backup_actor", owner.user.userId, "Local Owner"),
                Triple("u1_remote_backup_actor", null, "Remote Owner"))) {
                val id = source.eventDao().insert(EventEntity(createdAt = stamp, occurredAt = "2026-10-14", eventType = "SALE",
                    saleQuantity = 1.0, saleValue = 190.0, unitRate = 190.0, eventUid = uid))
                source.eventActorLinkDao().insert(EventActorLinkEntity(id, actorId, name, "owner", stamp))
            }
            val expected = source.backupDataDao().eventActorLinks()
            val users = source.appUserDao().all()
            val remote = expected.single { it.actorUserId == null }
            val encoded = eventActorLinkJson(remote)
            assertEquals(listOf(remote), readEventActorLinks(org.json.JSONArray().put(encoded)))
            encoded.remove("actor_user_id")
            assertTrue("Incomplete author reference must not silently become unlinked",
                runCatching { readEventActorLinks(org.json.JSONArray().put(encoded)) }.isFailure)
            val export = manager.exportEncryptedSnapshot(source, Uri.fromFile(file), phrase)
            assertTrue(export.exceptionOrNull()?.message, export.isSuccess)
            val restore = manager.restoreEncrypted(target, Uri.fromFile(file),
                BackupManager.RestoreSelection(true, true, false, security = true), phrase, NativeSessionManager())
            assertTrue(restore.exceptionOrNull()?.message, restore.isSuccess)
            assertEquals(expected, target.backupDataDao().eventActorLinks())
            assertEquals(users, target.appUserDao().all())
            assertEquals(source.backupDataDao().events(), target.backupDataDao().events())
        } finally {
            source.close(); target.close(); file.delete()
            dir.listFiles().orEmpty().filter { it.name !in beforeFiles }.forEach { it.delete() }
        }
    }
}
