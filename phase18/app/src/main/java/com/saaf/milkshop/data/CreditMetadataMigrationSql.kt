package com.saaf.milkshop.data

internal object CreditMetadataMigrationSql {
    val createTables = listOf(
        """CREATE TABLE IF NOT EXISTS credit_term_revisions (revision_uid TEXT NOT NULL PRIMARY KEY, customer_uid TEXT NOT NULL, prior_revision_uid TEXT NOT NULL, term_kind TEXT NOT NULL, term_value INTEGER NOT NULL, local_actor_uid TEXT NOT NULL, accepted_at TEXT NOT NULL, contract_version INTEGER NOT NULL, FOREIGN KEY(customer_uid) REFERENCES accounts(uid) ON UPDATE NO ACTION ON DELETE NO ACTION)""",
        "CREATE INDEX IF NOT EXISTS index_credit_term_revisions_customer_uid ON credit_term_revisions(customer_uid)",
        """CREATE TABLE IF NOT EXISTS credit_term_heads (customer_uid TEXT NOT NULL PRIMARY KEY, revision_uid TEXT NOT NULL, FOREIGN KEY(customer_uid) REFERENCES accounts(uid) ON UPDATE NO ACTION ON DELETE NO ACTION, FOREIGN KEY(revision_uid) REFERENCES credit_term_revisions(revision_uid) ON UPDATE NO ACTION ON DELETE NO ACTION)""",
        "CREATE INDEX IF NOT EXISTS index_credit_term_heads_revision_uid ON credit_term_heads(revision_uid)",
        """CREATE TABLE IF NOT EXISTS credit_sale_agreements (root_sale_uid TEXT NOT NULL PRIMARY KEY, customer_uid TEXT NOT NULL, evidence TEXT NOT NULL, term_kind TEXT NOT NULL, term_value INTEGER NOT NULL, revision_uid TEXT NOT NULL, contract_version INTEGER NOT NULL, FOREIGN KEY(root_sale_uid) REFERENCES events(event_uid) ON UPDATE NO ACTION ON DELETE NO ACTION, FOREIGN KEY(customer_uid) REFERENCES accounts(uid) ON UPDATE NO ACTION ON DELETE NO ACTION)""",
        "CREATE INDEX IF NOT EXISTS index_credit_sale_agreements_customer_uid ON credit_sale_agreements(customer_uid)",
        """CREATE TABLE IF NOT EXISTS credit_review_actions (action_uid TEXT NOT NULL PRIMARY KEY, customer_uid TEXT NOT NULL, kind TEXT NOT NULL, clears_set_uid TEXT, reason TEXT NOT NULL, local_actor_uid TEXT NOT NULL, occurred_at TEXT NOT NULL, business_date TEXT NOT NULL, contract_version INTEGER NOT NULL, FOREIGN KEY(customer_uid) REFERENCES accounts(uid) ON UPDATE NO ACTION ON DELETE NO ACTION)""",
        "CREATE INDEX IF NOT EXISTS index_credit_review_actions_customer_uid ON credit_review_actions(customer_uid)",
        "CREATE INDEX IF NOT EXISTS index_credit_review_actions_clears_set_uid ON credit_review_actions(clears_set_uid)",
        """CREATE TABLE IF NOT EXISTS credit_metadata_pending (operation_uid TEXT NOT NULL PRIMARY KEY, kind TEXT NOT NULL, payload_json TEXT NOT NULL, created_at TEXT NOT NULL, held INTEGER NOT NULL)""",
        """CREATE TABLE IF NOT EXISTS credit_metadata_sync_state (state_id INTEGER NOT NULL PRIMARY KEY, shop_uid TEXT NOT NULL, last_server_seq INTEGER NOT NULL, contract_version INTEGER NOT NULL, enabled INTEGER NOT NULL)""",
    )
    val immutableTables = listOf("credit_term_revisions", "credit_sale_agreements", "credit_review_actions")
    val immutableTriggerNames = immutableTables.flatMap { table -> listOf("prevent_${table}_update", "prevent_${table}_delete") }
    val immutableTriggers = immutableTables.flatMap { table -> listOf("UPDATE", "DELETE").map { verb ->
        "CREATE TRIGGER IF NOT EXISTS prevent_${table}_${verb.lowercase()} BEFORE $verb ON $table BEGIN SELECT RAISE(ABORT, 'Credit metadata is immutable'); END"
    } }
    // Explicit evidence for unambiguous pre-contract root sales only; never change financial truth.
    const val BACKFILL_LEGACY = """INSERT OR IGNORE INTO credit_sale_agreements(root_sale_uid, customer_uid, evidence, term_kind, term_value, revision_uid, contract_version)
        SELECT e.event_uid, MIN(a.uid), 'LEGACY_WEEKLY', 'NET_DAYS', 7, 'weekly-default-v1', 1
        FROM events e JOIN financial_log f ON f.event_id=e.event_id JOIN accounts a ON a.account_id=f.account_id
        WHERE e.event_type='SALE' AND e.corrects_event_id IS NULL AND a.category='Customer'
        GROUP BY e.event_id HAVING COUNT(DISTINCT a.uid)=1"""
}
