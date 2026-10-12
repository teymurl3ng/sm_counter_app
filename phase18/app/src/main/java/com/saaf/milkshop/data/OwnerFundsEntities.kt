package com.saaf.milkshop.data

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/** Declared withdrawal truth, independent of its correction's signed ledger legs. */
@Entity(tableName = "owner_funds_details", foreignKeys = [
    ForeignKey(entity = EventEntity::class, parentColumns = ["event_id"], childColumns = ["event_id"]),
    ForeignKey(entity = EventEntity::class, parentColumns = ["event_id"], childColumns = ["withdrawal_root_event_id"]),
    ForeignKey(entity = AccountEntity::class, parentColumns = ["account_id"], childColumns = ["payment_account_id"]),
], indices = [Index("payment_account_id"), Index("withdrawal_root_event_id")])
data class OwnerFundsDetailEntity(
    @PrimaryKey @ColumnInfo(name = "event_id") val eventId: Long,
    @ColumnInfo(name = "payment_account_id") val paymentAccountId: Long,
    @ColumnInfo(name = "withdrawal_root_event_id") val withdrawalRootEventId: Long,
    @ColumnInfo(name = "reviewed_withdrawal_amount") val reviewedWithdrawalAmount: Double? = null,
    @ColumnInfo(name = "reviewed_received_total") val reviewedReceivedTotal: Double? = null,
    @ColumnInfo(name = "excess_acknowledged", defaultValue = "0") val excessAcknowledged: Boolean = false,
)

internal object OwnerFundsMigrationSql {
    const val CREATE_TABLE = """CREATE TABLE IF NOT EXISTS owner_funds_details (
        event_id INTEGER NOT NULL PRIMARY KEY,
        payment_account_id INTEGER NOT NULL,
        withdrawal_root_event_id INTEGER NOT NULL,
        FOREIGN KEY(event_id) REFERENCES events(event_id) ON UPDATE NO ACTION ON DELETE NO ACTION,
        FOREIGN KEY(payment_account_id) REFERENCES accounts(account_id) ON UPDATE NO ACTION ON DELETE NO ACTION,
        FOREIGN KEY(withdrawal_root_event_id) REFERENCES events(event_id) ON UPDATE NO ACTION ON DELETE NO ACTION
    )"""

    val detailTriggers = listOf(
        """CREATE TRIGGER IF NOT EXISTS validate_owner_funds_detail BEFORE INSERT ON owner_funds_details
        BEGIN SELECT CASE
            WHEN NOT EXISTS (SELECT 1 FROM events WHERE event_id = NEW.event_id AND event_type = 'OWNER_DRAWING')
                OR NOT EXISTS (SELECT 1 FROM accounts WHERE account_id = NEW.payment_account_id AND category = 'Cash/Bank')
                THEN RAISE(ABORT, 'Invalid withdrawal account or event')
            WHEN (SELECT corrects_event_id FROM events WHERE event_id = NEW.event_id) IS NULL
                AND NEW.withdrawal_root_event_id <> NEW.event_id
                THEN RAISE(ABORT, 'Original withdrawal must reference itself')
            WHEN (SELECT corrects_event_id FROM events WHERE event_id = NEW.event_id) IS NOT NULL
                AND NOT EXISTS (SELECT 1 FROM owner_funds_details p JOIN events e ON e.corrects_event_id = p.event_id
                    WHERE e.event_id = NEW.event_id AND p.withdrawal_root_event_id = NEW.withdrawal_root_event_id)
                THEN RAISE(ABORT, 'Withdrawal parent details are missing or inconsistent')
            WHEN EXISTS (SELECT 1 FROM events e JOIN events p ON p.event_id = e.corrects_event_id
                JOIN owner_funds_details d ON d.event_id = p.event_id WHERE e.event_id = NEW.event_id
                AND (e.amount = p.amount AND NEW.payment_account_id = d.payment_account_id
                     OR e.amount = 0 AND NEW.payment_account_id <> d.payment_account_id))
                THEN RAISE(ABORT, 'Withdrawal truth must change; cancellation retains its account')
        END; END""".trimIndent(),
        """CREATE TRIGGER IF NOT EXISTS prevent_owner_funds_update BEFORE UPDATE ON owner_funds_details
        BEGIN SELECT RAISE(ABORT, 'Withdrawal details are immutable'); END""",
    )

    /** Backfill only complete, unambiguous legacy same-account chains. Never re-enqueue history. */
    suspend fun backfill(connection: SQLiteConnection) {
        connection.execSQL("""INSERT INTO owner_funds_details
            SELECT e.event_id, fl.account_id, e.event_id FROM events e
            JOIN financial_log fl ON fl.event_id = e.event_id JOIN accounts a ON a.account_id = fl.account_id
            WHERE e.event_type = 'OWNER_DRAWING' AND e.corrects_event_id IS NULL
            AND e.amount > 0 AND fl.amount = -e.amount AND a.category = 'Cash/Bank'
            AND (SELECT COUNT(*) FROM financial_log WHERE event_id = e.event_id) = 1
            AND NOT EXISTS (SELECT 1 FROM inventory_log WHERE event_id = e.event_id)
            AND NOT EXISTS (SELECT 1 FROM owner_funds_details WHERE event_id = e.event_id)""")
        var changed: Long
        do {
            connection.execSQL("""INSERT INTO owner_funds_details
                SELECT e.event_id, d.payment_account_id, d.withdrawal_root_event_id FROM events e
                JOIN events p ON p.event_id = e.corrects_event_id
                JOIN owner_funds_details d ON d.event_id = p.event_id
                WHERE e.event_type = 'OWNER_DRAWING' AND p.event_type = 'OWNER_DRAWING'
                AND e.amount >= 0 AND e.amount <> p.amount AND e.occurred_at = p.occurred_at
                AND length(trim(COALESCE(e.notes,''))) > 0
                AND (SELECT COUNT(*) FROM financial_log WHERE event_id = e.event_id) = 1
                AND EXISTS (SELECT 1 FROM financial_log WHERE event_id = e.event_id
                    AND account_id = d.payment_account_id AND amount = p.amount - e.amount)
                AND NOT EXISTS (SELECT 1 FROM inventory_log WHERE event_id = e.event_id)
                AND NOT EXISTS (SELECT 1 FROM owner_funds_details WHERE event_id = e.event_id)""")
            changed = connection.prepare("SELECT changes()").use { it.step(); it.getLong(0) }
        } while (changed > 0)
    }
}

internal object OwnerFundsReceiptMigrationSql {
    val detailTriggers = listOf(
        """CREATE TRIGGER IF NOT EXISTS validate_owner_funds_detail BEFORE INSERT ON owner_funds_details
        BEGIN SELECT CASE
            WHEN NOT EXISTS (SELECT 1 FROM events WHERE event_id = NEW.event_id AND event_type IN ('OWNER_DRAWING','OWNER_FUNDS_IN'))
                OR NOT EXISTS (SELECT 1 FROM accounts WHERE account_id = NEW.payment_account_id AND category = 'Cash/Bank')
                THEN RAISE(ABORT, 'Invalid owner funds account or event')
            WHEN (SELECT event_type FROM events WHERE event_id = NEW.event_id) = 'OWNER_DRAWING'
                AND (SELECT corrects_event_id FROM events WHERE event_id = NEW.event_id) IS NULL
                AND NEW.withdrawal_root_event_id <> NEW.event_id
                THEN RAISE(ABORT, 'Original withdrawal must reference itself')
            WHEN (SELECT event_type FROM events WHERE event_id = NEW.event_id) = 'OWNER_FUNDS_IN'
                AND NOT EXISTS (SELECT 1 FROM events r WHERE r.event_id = NEW.withdrawal_root_event_id
                    AND r.event_type = 'OWNER_DRAWING' AND r.corrects_event_id IS NULL)
                THEN RAISE(ABORT, 'Receipt must reference an original withdrawal')
            WHEN (SELECT corrects_event_id FROM events WHERE event_id = NEW.event_id) IS NOT NULL
                AND NOT EXISTS (SELECT 1 FROM events e JOIN owner_funds_details p ON p.event_id = e.corrects_event_id
                    WHERE e.event_id = NEW.event_id AND p.withdrawal_root_event_id = NEW.withdrawal_root_event_id)
                THEN RAISE(ABORT, 'Owner funds parent details are missing or inconsistent')
            WHEN EXISTS (SELECT 1 FROM events e JOIN events p ON p.event_id = e.corrects_event_id
                JOIN owner_funds_details d ON d.event_id = p.event_id WHERE e.event_id = NEW.event_id
                AND (e.amount = p.amount AND NEW.payment_account_id = d.payment_account_id
                     OR e.amount = 0 AND NEW.payment_account_id <> d.payment_account_id))
                THEN RAISE(ABORT, 'Owner funds truth must change; cancellation retains its account')
            WHEN (NEW.reviewed_withdrawal_amount IS NULL) <> (NEW.reviewed_received_total IS NULL)
                OR (NEW.reviewed_withdrawal_amount IS NOT NULL AND (NEW.reviewed_withdrawal_amount < 0
                    OR NEW.reviewed_received_total < 0))
                THEN RAISE(ABORT, 'Owner funds review snapshot is invalid')
        END; END""".trimIndent(),
        OwnerFundsMigrationSql.detailTriggers.last(),
    )
}
