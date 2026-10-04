package com.saaf.milkshop.sync

import com.saaf.milkshop.data.SyncOutboxEntity
import com.saaf.milkshop.readSyncOutboxRows
import com.saaf.milkshop.syncOutboxJson
import org.json.JSONArray
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * JVM tests for the go-8 format-5 backup outbox codec: the outbox rides in
 * the snapshot (uid/table/timestamp only) so edits that were unsynced at
 * backup time survive a restore and can be classified against the server.
 * Format-4 (and older) backups have no such array — the reader must treat
 * that as empty, never an error.
 */
class SyncOutboxBackupCodecTest {

    @Test
    fun roundTrip_preservesIdentityAndOrder() {
        val rows = listOf(
            SyncOutboxEntity(tableName = "events", rowUid = "u1_event_a", enqueuedAt = "2026-10-03T10:00:00.000Z"),
            SyncOutboxEntity(tableName = "products", rowUid = "u1_seed_products_Milk", enqueuedAt = "2026-10-03T10:00:01.000Z"),
            SyncOutboxEntity(tableName = "settings", rowUid = "setting:milk_rate_vdr", enqueuedAt = "2026-10-03T10:00:02.000Z"),
        )
        val array = JSONArray(rows.map(::syncOutboxJson))
        val restored = readSyncOutboxRows(array)
        assertEquals(rows, restored)
    }

    @Test
    fun absentArray_readsAsEmpty_format4Backups() {
        assertTrue(readSyncOutboxRows(null).isEmpty())
        assertTrue(readSyncOutboxRows(JSONArray()).isEmpty())
    }

    @Test
    fun writtenShape_carriesOnlyTheThreeFields() {
        val json = syncOutboxJson(
            SyncOutboxEntity(outboxId = 12L, tableName = "deliveries", rowUid = "u1_del", enqueuedAt = "t"),
        )
        assertEquals(3, json.length())
        assertEquals("deliveries", json.getString("table_name"))
        assertEquals("u1_del", json.getString("row_uid"))
        assertEquals("t", json.getString("enqueued_at"))
        // outbox_id is storage-local and must not ride in the backup.
        assertTrue(!json.has("outbox_id"))
    }
}
